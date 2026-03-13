package com.example.entity.vo.response;

import lombok.Data;

@Data
public class ClientDetailsVO {
    private int id;
    private String name;
    private String node;
    private String location;
    private boolean online;
    private String ip;
    private String cpuName;
    private double memory;
    private String osName;
    private String osVersion;
    private double disk;
    private int cpuCores;
}
