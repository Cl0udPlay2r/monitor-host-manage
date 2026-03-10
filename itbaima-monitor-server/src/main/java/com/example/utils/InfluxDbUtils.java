package com.example.utils;

import com.example.entity.vo.request.RuntimeDetailVO;
import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class InfluxDbUtils {
    @Value("${spring.influxdb3-core.url}")
    String url;
    @Value("${spring.influxdb3-core.database}")
    String database;
    @Value("${spring.influxdb3-core.token}")
    String token;

    private InfluxDBClient client;

    @PostConstruct
    public void init(){
        client = InfluxDBClient.getInstance(url,token.toCharArray(),database);

    }


    public void writeRuntimeDetail(RuntimeDetailVO vo,int clientId){
        Point point = Point.measurement("runtime")
                .setTag("clientId",String.valueOf(clientId))
                .setField("cpuUsage",vo.getCpuUsage())
                .setField("memoryUsage",vo.getMemoryUsage())
                .setField("diskUsage",vo.getDiskUsage())
                .setField("networkDownload",vo.getNetworkDownload())
                .setField("networkUpload",vo.getNetworkUpload())
                .setField("diskRead",vo.getDiskRead())
                .setField("diskWrite",vo.getDiskWrite())
                .setTimestamp(new Date(vo.getTimestamp()).toInstant());

        client.writePoint(point);
    }

}
