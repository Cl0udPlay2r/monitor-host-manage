package com.example.websocket;


import com.example.entity.dto.ClientDetail;
import com.example.entity.dto.ClientSsh;
import com.example.mapper.ClientDetailMapper;
import com.example.mapper.ClientSshMapper;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@ServerEndpoint("/terminal/{clientId}")
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

    private static final Map<Session, Shell> sessionMap = new ConcurrentHashMap<>();
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    @OnOpen
    public void onOpen(Session session,
                       @PathParam(value = "clientId") String clientId) throws Exception {
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
            jsSession.setPassword(ssh.getPassword());
            jsSession.setConfig("StrictHostKeyChecking","no");
            jsSession.setTimeout(3000);
            jsSession.connect();
            ChannelShell channel = (ChannelShell) jsSession.openChannel("shell");
            channel.setPtyType("xterm");
            channel.connect(1000);
            sessionMap.put(session,new Shell(session,jsSession,channel));
            return true;
        }catch (JSchException e){
            String message = e.getMessage();
            if(message.equals("Auth fail")){
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "登录SSH失败，用户名或密码错误"));
                log.error("连接SSH失败，请检查用户名或密码");
            }else if(message.contains("Connection refused")){
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "连接被拒绝，可能是没有开启ssh服务或没有开放端口"));
                log.error("连接SSH失败，连接被拒绝，可能是没有开启ssh服务或者没有开放端口");
            }else{
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT,message));
                log.error("ssh服务连接失败{}", message);
            }
        }
        return false;
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

        private void read(){
            try{
                byte[] buffer = new byte[1024 * 1024];
                int i;
                while ((i = input.read(buffer)) != -1){
                    String text = new String(Arrays.copyOfRange(buffer,0,i),StandardCharsets.UTF_8);
                    session.getBasicRemote().sendText(text);
                }
            }catch (IOException e){
                log.error("读取ssh输入流出现异常{}", e.getMessage());
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
