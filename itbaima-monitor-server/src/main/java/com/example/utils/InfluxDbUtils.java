package com.example.utils;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.request.RuntimeDetailVO;
import com.example.entity.vo.response.RuntimeHistoryVO;
import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import com.example.controller.exception.TimeSeriesQueryException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.stream.Stream;

@Component
@Slf4j
public class InfluxDbUtils {
    @Value("${spring.influxdb3-core.url}")
    String url;
    @Value("${spring.influxdb3-core.database}")
    String database;
    @Value("${spring.influxdb3-core.token}")
    String token;

    private InfluxDBClient client;

    @PostConstruct
    public void init() {
        client = InfluxDBClient.getInstance(url, token.toCharArray(), database);

    }


    public void writeRuntimeDetail(RuntimeDetailVO vo, int clientId) {
        Point point = Point.measurement("runtime")
                .setTag("client_id", String.valueOf(clientId))
                .setField("cpu_usage", vo.getCpuUsage())
                .setField("memory_usage", vo.getMemoryUsage())
                .setField("disk_usage", vo.getDiskUsage())
                .setField("network_download", vo.getNetworkDownload())
                .setField("network_upload", vo.getNetworkUpload())
                .setField("disk_read", vo.getDiskRead())
                .setField("disk_write", vo.getDiskWrite())
                .setTimestamp(new Date(vo.getTimestamp()).toInstant());

        client.writePoint(point);
    }

    public RuntimeHistoryVO queryRuntimeHistory(int id) {
        String query = """
                SELECT
                *
                FROM runtime
                WHERE time >= now() - interval '1 hour'
                AND "client_id" = '%s'
                ORDER BY time ASC;
                """.formatted(id);
        try (Stream<Object[]> stream = client.query(query)) {

            String[] columns = {
                    "clientId",
                    "cpuUsage",
                    "diskRead",
                    "diskUsage",
                    "diskWrite",
                    "memoryUsage",
                    "networkDownload",
                    "networkUpload",
                    "timestamp"
            };
            RuntimeHistoryVO vo = new RuntimeHistoryVO();
            stream.forEach(row -> {
                int size = row.length;
                JSONObject object = new JSONObject();
                for (int i = 0; i < size; i++) {
                    object.put(columns[i], row[i]);
                }
                vo.getList().add(object);
            });
            return vo;
        } catch (Exception e) {
            // 不再返回 null：null 会被 Controller 包成「成功 + 空数据」，前端表现为一张空图表且无任何
            // 错误提示，等同于用空数据掩盖存储层故障（见 F12）。改为抛出专用异常，由全局异常处理返回 500。
            log.error("influxdb数据库查询失败", e);
            throw new TimeSeriesQueryException("时序库查询失败，请稍后重试", e);
        }
    }

}
