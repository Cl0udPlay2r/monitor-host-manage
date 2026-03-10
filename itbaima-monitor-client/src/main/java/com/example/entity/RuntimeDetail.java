package com.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class RuntimeDetail {
    private long timestamp;
    private double cpuUsage;
    private double memoryUsage;
    private double diskUsage;
    private double networkUpload;
    private double networkDownload;
    private double diskRead;
    private double diskWrite;
}
