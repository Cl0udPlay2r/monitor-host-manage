package com.example.filter;

import com.example.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;

import java.io.IOException;

/**
 * 跨域配置过滤器，仅处理跨域，添加跨域响应头
 */
@Component
@Order(Const.ORDER_CORS)
public class CorsFilter extends HttpFilter {

    @Value("${spring.web.cors.origin}")
    String origin;

    @Value("${spring.web.cors.credentials}")
    boolean credentials;

    @Value("${spring.web.cors.methods}")
    String methods;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request, response);
        // 预检请求必须在此处直接结束并返回 2xx：
        // 浏览器的 CORS 预检要求响应状态必须是 2xx，否则无论响应头多完美都会判定为跨域失败。
        // 若继续传递，匿名 OPTIONS 会被 Spring Security 拦截并返回 401（见 F2 改为真实状态码后），
        // 前端控制台就会报 "Response to preflight request doesn't pass access control check"。
        if (CorsUtils.isPreFlightRequest(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 添加所有跨域相关响应头
     * @param request 请求
     * @param response 响应
     */
    private void addCorsHeader(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", this.resolveOrigin(request));
        response.addHeader("Access-Control-Allow-Methods", this.resolveMethod());
        response.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        if(credentials) {
            response.addHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    /**
     * 解析配置文件中的请求方法
     * @return 解析得到的请求头值
     */
    private String resolveMethod(){
        return methods.equals("*") ? "GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, PATCH" : methods;
    }

    /**
     * 解析配置文件中的请求原始站点
     * @param request 请求
     * @return 解析得到的请求头值
     */
    private String resolveOrigin(HttpServletRequest request){
        return origin.equals("*") ? request.getHeader("Origin") : origin;
    }
}
