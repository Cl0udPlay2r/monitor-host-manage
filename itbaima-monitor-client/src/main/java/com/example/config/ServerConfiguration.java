package com.example.config;

import com.alibaba.fastjson2.JSON;
import com.example.entity.ConnectionConfig;
import com.example.utils.MonitorUtils;
import com.example.utils.NetUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Slf4j
@Configuration
public class ServerConfiguration {

    @Resource
    NetUtils netUtils;

    @Resource
    MonitorUtils monitorUtils;

    @Bean
    public ConnectionConfig connectionConfig(){
        log.info("正在加载服务端加载配置...");
        ConnectionConfig connectionConfig = this.readConfigurationFromFile();
        if(connectionConfig == null){
            connectionConfig = this.registerToServer();
        }
        System.out.println(monitorUtils.getBaseDetail());
        return connectionConfig;
    }

    private ConnectionConfig registerToServer(){
        Scanner scanner = new Scanner(System.in);
        String address,token;
        do {
            log.info("请输入需要注册的服务端访问地址，格式如'http://192.168.0.23:8080'，请输入：");
            address = scanner.nextLine();
            log.info("请输入服务端生成的用于注册客户端的Token密钥,请输入：");
            token = scanner.nextLine();
        }while (!netUtils.registerToServer(address,token));
        ConnectionConfig config = new ConnectionConfig(address, token);
        this.saveConfigurationToFile(config);
        return config;
    }

    private void saveConfigurationToFile(ConnectionConfig config){
        File dir = new File("config");
        if(!dir.exists() && dir.mkdirs())
            log.info("创建用于保存服务端连接信息的目录已完成");
        File file = new File("config/server.json");
        try(FileWriter writer = new FileWriter(file)){
            writer.write(JSON.toJSONString(config));
        }catch(IOException e){
            log.error("服务端连接信息保存时出现错误",e);
        }
    }

    /**
     * 从本地文件读取服务端配置信息
     * @return 配置信息
     */
    private ConnectionConfig readConfigurationFromFile(){
        File configurationFile= new File("config/server.json");
        if(configurationFile.exists()){
            try(FileInputStream stream = new FileInputStream(configurationFile)){
                String raw =  new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                return JSON.parseObject(raw, ConnectionConfig.class);
            }catch (IOException e){
                log.error("配置文件加载失败...",e);
            }
        }
        return null;
    }

}
