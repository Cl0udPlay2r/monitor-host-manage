package com.example.websocket;


import com.example.entity.dto.ClientDetail;
import com.example.entity.dto.ClientSsh;
import com.example.mapper.ClientDetailMapper;
import com.example.mapper.ClientSshMapper;
import com.example.utils.CredentialCipher;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@ServerEndpoint(value = "/terminal/{clientId}",
        configurator = TerminalHandshakeConfigurator.class)
public class TerminalWebSocket {


    private static ClientDetailMapper detailMapper;

    @Resource
    public void setDetailMapper(ClientDetailMapper detailMapper) {
        TerminalWebSocket.detailMapper = detailMapper;
    }

    private static ClientSshMapper sshMapper;

    @Resource
    public void setSshMapper(ClientSshMapper sshMapper) {
        TerminalWebSocket.sshMapper = sshMapper;
    }

    private static CredentialCipher cipher;

    @Resource
    public void setCipher(CredentialCipher cipher) {
        TerminalWebSocket.cipher = cipher;
    }

    private static final Map<Session, Shell> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    /**
     * 终端尺寸控制帧前缀。以 NUL 开头的消息不当做键盘输入，而是“上报终端尺寸”，
     * 格式：{@code \0resize:{cols},{rows}}。
     * 浏览器终端默认 80x20，而服务端未设置 PTY 尺寸时 JSch 会回退成 24x80，
     * 两者不一致会导致 top/vi 等全屏程序错行（见测试记录 F10）。
     */
    private static final String CTRL_RESIZE_PREFIX = "\u0000resize:";

    @OnOpen
    public void onOpen(Session session,
                       @PathParam(value = "clientId") String clientId) throws Exception {
        // 双保险：正常情况下身份与权限已在 TerminalHandshakeConfigurator 的握手阶段校验完毕。
        // 该校验失败时不会写入身份属性，这里立即关闭会话，全程不查询数据库、不建立 SSH 连接。
        if (session.getUserProperties().get(TerminalHandshakeConfigurator.PROP_USER_ID) == null) {
            Object rejectReason = session.getUserProperties().get(TerminalHandshakeConfigurator.PROP_REJECT_REASON);
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                    rejectReason == null ? "未通过身份校验" : rejectReason.toString()));
            return;
        }
        ClientDetail detail = detailMapper.selectById(clientId);
        ClientSsh ssh = sshMapper.selectById(clientId);
        if(detail == null || ssh == null){
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "无法识别主机"));
            return;
        }
        if (this.createSshConnection(session,ssh,detail.getIp())) {
            log.info("主机{}的SSH连接已建立" , detail.getIp());
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        Shell shell = sessionMap.get(session);
        if (shell == null) return;
        // 以 NUL 开头的控制帧：同步终端尺寸，不写入 SSH 输入流
        if (message.startsWith(CTRL_RESIZE_PREFIX)) {
            shell.resize(message.substring(CTRL_RESIZE_PREFIX.length()));
            return;
        }
        OutputStream output = shell.output;
        output.write(message.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        Shell shell = sessionMap.get(session);
        if(shell != null){
            shell.close();
            sessionMap.remove(session);
            log.info("主机 {} 的ssh连接已断开" , shell.jsSession.getHost());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) throws IOException {
        log.error("用户WebSocket连接出现错误" + error);
        session.close();
    }

    private boolean createSshConnection(Session session,ClientSsh ssh,String ip) throws IOException{
        try{
            JSch jSch = new JSch();
            com.jcraft.jsch.Session jsSession = jSch.getSession(ssh.getUsername(),ip,ssh.getPort());
            // 库中存的是密文，连接前解密（见 F4）
            jsSession.setPassword(cipher.decrypt(ssh.getPassword()));
            jsSession.setConfig("StrictHostKeyChecking","no");
            jsSession.setTimeout(3000);
            jsSession.connect();
            ChannelShell channel = (ChannelShell) jsSession.openChannel("shell");
            channel.setPtyType("xterm");
            channel.connect(1000);
            sessionMap.put(session,new Shell(session,jsSession,channel));
            return true;
        }catch (JSchException e){
            String reason = this.describeSshFailure(e);
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, reason));
            log.error("连接SSH失败: {}", reason, e);
        }
        return false;
    }

    /**
     * 归类 SSH 连接失败原因。
     * 旧实现用 message.equals("Auth fail") / contains("Connection refused") 判断，
     * 而 JSch 在端口不可达时抛的是 "timeout: socket is not established"，两个分支都不命中，
     * 原始异常文本被直接回传给用户，运维无法定位（见 F7）。
     * 这里改为「异常链类型优先、文本特征辅助」，覆盖常见四类失败。
     */
    private String describeSshFailure(JSchException e){
        Throwable cause = e.getCause();
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("Auth fail") || message.contains("Auth cancel")) {
            return "登录SSH失败，用户名或密码错误";
        }
        if (cause instanceof java.net.ConnectException || message.contains("Connection refused")) {
            return "连接被拒绝，可能是没有开启ssh服务或没有开放端口";
        }
        if (cause instanceof java.net.SocketTimeoutException || message.startsWith("timeout")) {
            return "连接目标主机超时，请检查网络连通性或主机是否可达";
        }
        if (cause instanceof java.net.UnknownHostException || message.contains("UnknownHost")) {
            return "无法解析目标主机地址";
        }
        return "SSH 连接失败：" + message;
    }

    private class Shell {
        private final Session session;
        private final com.jcraft.jsch.Session jsSession;
        private final ChannelShell channel;
        private final InputStream input;
        private final OutputStream output;

        public Shell(Session session, com.jcraft.jsch.Session jsSession,
                     ChannelShell channel) throws IOException {
            this.channel = channel;
            this.session = session;
            this.jsSession = jsSession;
            this.input = channel.getInputStream();
            this.output = channel.getOutputStream();
            service.submit(this::read);
        }

        /**
         * 跨 read 块保留木完成的多字节序列的 UTF-8 解码器。
         * 旧实现是 `new String(本次字节块, UTF_8)`，若一个中文字符（3 字节）刚好被两次 read 切开，
         * 两块各自都解不出合法字符，就会各产生一个替换字符（实测 100870 字符中出现 13 个，见 F11）。
         */
        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        /** 上一次解码剩余的未完成字节（UTF-8 单字符最多 4 字节，8 字节足够） */
        private final ByteBuffer carry = ByteBuffer.allocate(8);

        private void read(){
            try{
                byte[] buffer = new byte[1024 * 1024];
                int i;
                while ((i = input.read(buffer)) != -1){
                    String text = this.decode(buffer, i);
                    if (!text.isEmpty()) session.getBasicRemote().sendText(text);
                }
            }catch (IOException e){
                log.error("读取ssh输入流出现异常{}", e.getMessage());
            }
        }

        /**
         * 把本批字节接在上次残留的未完成序列之后一起解码。
         * endOfInput 传 false 时，解码器会把不完整的尾部序列留在 ByteBuffer 里，交给下一轮拼接。
         */
        private String decode(byte[] bytes, int length){
            ByteBuffer input = ByteBuffer.allocate(carry.position() + length);
            input.put(carry.array(), 0, carry.position());
            input.put(bytes, 0, length);
            input.flip();
            carry.clear();

            CharBuffer output = CharBuffer.allocate((int) (input.remaining() * decoder.maxCharsPerByte()) + 1);
            // 注意：该重载不抛受检异常，而是把“不完整尾部序列”留在 input 里（返回 CoderResult）
            decoder.decode(input, output, false);
            output.flip();
            if (input.hasRemaining()) carry.put(input);
            return output.toString();
        }

        /** 同步 PTY 窗口尺寸，参数格式为 "{cols},{rows}" */
        public void resize(String payload){
            try {
                String[] parts = payload.split(",");
                int cols = Integer.parseInt(parts[0].trim());
                int rows = Integer.parseInt(parts[1].trim());
                if (cols > 0 && rows > 0) {
                    channel.setPtySize(cols, rows, cols * 8, rows * 16);
                    log.info("终端尺寸已同步为 {}x{}", cols, rows);
                }
            } catch (Exception e) {
                log.warn("终端尺寸同步失败: {}", payload);
            }
        }

        public void close() throws IOException {
            input.close();
            output.close();
            channel.disconnect();
            jsSession.disconnect();
            service.shutdown();
        }
    }
}
