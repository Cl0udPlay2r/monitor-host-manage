package com.example.utils;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.BaseDetail;
import com.example.entity.ConnectionConfig;
import com.example.entity.Response;
import com.example.entity.RuntimeDetail;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
public class NetUtils {

    @Lazy
    @Resource
    ConnectionConfig config;

    // 必须显式设置超时：服务端不可达时（防火墙静默丢包）默认行为是无限期阻塞，
    // 而采集任务每 10 秒起一个线程，阻塞会把 Quartz 工作线程逐步占满，
    // 最终从“一次网络故障”放大成“采集链路整体停摆”（见测试记录 F6）。
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public boolean registerToServer(String address,String token){
        log.info("正在向服务端发起注册请求...");
        Response response = this.doGet("/register", address, token);
        if(response.success()){
            log.info("客户端注册成功");
        }else{
            log.error("客户端注册失败: {}",response.message());
        }
        return response.success();
    }

    private Response doGet(String url){
        return this.doGet(url,config.getAddress() ,config.getToken());
    }

    private Response doGet(String url,String address,String token){
         try{
             HttpRequest request = HttpRequest.newBuilder().GET()
                     .uri(new URI(address + "/monitor" + url))
                     .header("Authorization",token)
                     .timeout(REQUEST_TIMEOUT)
                     .build();
             HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
             return JSONObject.parseObject(response.body()).to(Response.class);
         }catch (Exception e){
             log.error("向服务端发起请求时出现问题",e);
             return Response.errorResponse(e);
         }
    }

    public void updateBeasDetail(BaseDetail detail){
        Response response = this.doPost("/detail", detail);
        if(response.success()){
            log.info("系统基本信息已更新成功");
        }else{
            log.error("系统基本信息更新失败: {}",response.message());
        }
    }

    public void updateRuntimeDetail(RuntimeDetail detail){
        Response response = this.doPost("/runtime", detail);
        if(!response.success())
            log.error("运行时数据上报失败失败: {}",response.message());
    }

    private Response doPost(String url,Object data){
        try{
            String rawData = JSONObject.from(data).toJSONString();
            HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(rawData))
                    .uri(new URI(config.getAddress() + "/monitor" + url))
                    .header("Authorization", config.getToken())
                    .header("Content-Type","application/json")
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        }catch(Exception e){
            log.error("向服务端发起请求时出现问题",e);
            return Response.errorResponse(e);
        }
    }
}
