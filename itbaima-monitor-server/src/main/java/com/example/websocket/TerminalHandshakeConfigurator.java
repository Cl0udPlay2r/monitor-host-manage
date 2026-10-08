package com.example.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.entity.dto.Account;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSSH 握手鉴权配置器。
 * <p>
 * 原实现中 {@code /terminal/{clientId}} 在 Spring Security 里是 permitAll，且 {@link TerminalWebSocket}
 * 的 onOpen 里也没有任何身份与权限校验，导致任何人只要猜出 clientId 就能驱动服务端用库中凭据
 * 向任意被管主机发起 SSH 连接。
 * <p>
 * 本配置器在 HTTP 升级为 WebSocket 之前（modifyHandshake 阶段）完成两件事：
 * <ol>
 *     <li>校验 JWT —— 浏览器无法为 WebSocket 自定义请求头，因此令牌通过
 *         {@code Sec-WebSocket-Protocol} 子协议字段传递，格式为 {@code new WebSocket(url, ["bearer", token])}；</li>
 *     <li>校验主机权限 —— 非管理员账号只能连自己授权列表内的 clientId（与 HTTP 接口口径一致）。</li>
 * </ol>
 * 任一校验不通过即不授权、不回选子协议，由 onOpen 立即关闭会话（不建库查询、不建 SSH 连接）。
 * <p>
 * 说明：浏览器建连时若声明了子协议而服务端未回选，握手本身即被浏览器判定失败，因此浏览器侧表现为
 * “连接失败”；对不校验子协议的裸客户端，则由 1008 关闭帧明确告知原因。
 */
@Slf4j
@Component
public class TerminalHandshakeConfigurator extends ServerEndpointConfig.Configurator {

    /** 子协议标识，前端固定携带 */
    public static final String SUB_PROTOCOL = "bearer";
    /** 握手成功后写入会话的属性名，供 TerminalWebSocket 复核 */
    public static final String PROP_USER_ID = "handshakeUserId";
    public static final String PROP_ROLE = "handshakeRole";
    /** 握手阶段被拒绝时写入的拒绝原因 */
    public static final String PROP_REJECT_REASON = "handshakeRejectReason";

    private static final String ROLE_PREFIX = "ROLE_";

    private static JwtUtils jwtUtils;
    private static AccountService accountService;

    // 该 Configurator 由容器（Tomcat）实例化，因此沿用与本包内 TerminalWebSocket 相同的静态注入手法
    @Resource
    public void setJwtUtils(JwtUtils jwtUtils) {
        TerminalHandshakeConfigurator.jwtUtils = jwtUtils;
    }

    @Resource
    public void setAccountService(AccountService accountService) {
        TerminalHandshakeConfigurator.accountService = accountService;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig config,
                               HandshakeRequest request,
                               HandshakeResponse response) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI().getPath();
        String clientId = path.substring(path.lastIndexOf('/') + 1);

        String token = this.extractToken(request);

        if (token == null) {
            this.reject(config, response, "未携带身份凭证");
            return;
        }

        DecodedJWT jwt = jwtUtils.resolveJwt("Bearer " + token);
        if (jwt == null) {
            this.reject(config, response, "身份凭证无效或已过期");
            return;
        }

        Integer userId = jwtUtils.toId(jwt);
        String role = this.extractRole(jwt);
        if (userId == null || role == null) {
            this.reject(config, response, "身份凭证内容不完整");
            return;
        }

        int targetClientId;
        try {
            targetClientId = Integer.parseInt(clientId);
        } catch (NumberFormatException e) {
            this.reject(config, response, "主机标识非法");
            return;
        }

        if (!this.isPermitted(role, userId, targetClientId)) {
            this.reject(config, response, "无权访问该主机");
            return;
        }

        config.getUserProperties().put(PROP_USER_ID, userId);
        config.getUserProperties().put(PROP_ROLE, role);

        // 客户端声明了子协议时，服务端必须回选其中之一，否则浏览器会判定握手失败
        response.getHeaders().put("Sec-WebSocket-Protocol", List.of(SUB_PROTOCOL));
        log.info("主机{}的终端握手校验通过，操作账号 id={}", targetClientId, userId);
    }

    /**
     * 从 Sec-WebSocket-Protocol 中取出令牌，形如 "bearer, eyJhbGciOi..."
     */
    private String extractToken(HandshakeRequest request) {
        List<String> values = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (values == null || values.isEmpty()) return null;
        for (String raw : values) {
            if (raw == null) continue;
            for (String part : raw.split(",")) {
                String candidate = part.trim();
                if (!candidate.isEmpty() && !SUB_PROTOCOL.equals(candidate)) return candidate;
            }
        }
        return null;
    }

    private String extractRole(DecodedJWT jwt) {
        try {
            List<String> authorities = new ArrayList<>(jwtUtils.toUser(jwt).getAuthorities())
                    .stream().map(a -> a.getAuthority()).toList();
            if (authorities.isEmpty()) return null;
            String authority = authorities.get(0);
            return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 与 MonitorController#permissionCheck 保持同一口径：管理员放行，普通账号需在授权主机列表内
     */
    private boolean isPermitted(String role, int userId, int clientId) {
        if (Const.ROLE_ADMIN.equals(role)) return true;
        Account account = accountService.getById(userId);
        if (account == null) return false;
        List<Integer> accessible = account.getClientList();
        return accessible != null && accessible.contains(clientId);
    }

    private void reject(ServerEndpointConfig config, HandshakeResponse response, String reason) {
        log.warn("终端握手被拒绝：{}", reason);
        // JSR-356 标准未提供“拒绝握手”的 API（jakarta.websocket.HandshakeResponse 无 setStatusCode，
        // Tomcat 的 WsHandshakeResponse 也未实现），因此此处不写入身份属性、也不回选子协议，
        // 由 TerminalWebSocket#onOpen 第一行立即关闭会话，全过程不访问数据库、不发起 SSH 连接。
        config.getUserProperties().put(PROP_REJECT_REASON, reason);
    }
}
