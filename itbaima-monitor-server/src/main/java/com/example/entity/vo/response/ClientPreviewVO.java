package com.example.entity.vo.response;

import lombok.Data;

@Data
public class ClientPreviewVO {
    private int id;
    private String name;
    private String osName;
    private String osVersion;
    private boolean online;
    private String location;
    private String ip;
    private String cpuName;
    private int cpuCores;
    private double cpuUsage;
    private double memory;
    private double memoryUsage;
    private double networkUpload;
    private double networkDownload;
}
