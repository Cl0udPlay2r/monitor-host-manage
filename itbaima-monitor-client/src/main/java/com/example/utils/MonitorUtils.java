package com.example.utils;


import com.example.entity.BaseDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Component
public class MonitorUtils {

    private final SystemInfo info = new SystemInfo();
    private final Properties prop = System.getProperties();


    public BaseDetail getBaseDetail(){
        OperatingSystem os = info.getOperatingSystem();
        HardwareAbstractionLayer hardware = info.getHardware();

        double memory = hardware.getMemory().getTotal() / 1024.0 /1024 / 1024;
        double diskSize = Arrays.stream(File.listRoots()).mapToDouble(File::getTotalSpace).sum() / 1024.0 /1024 / 1024;
        String ip = Objects.requireNonNull(this.findNetworkInterface(hardware)).getIPv4addr()[0];

        return new BaseDetail()
                .setOsArch(prop.getProperty("os.arch"))
                .setOsName(os.getFamily())
                .setOsVersion(os.getVersionInfo().getVersion())
                .setOsBit(os.getBitness())
                .setCpuName(hardware.getProcessor().getProcessorIdentifier().getName())
                .setCpuCores(hardware.getProcessor().getLogicalProcessorCount())
                .setMemory(memory)
                .setDisk(diskSize)
                .setIp(ip);

    }

    private NetworkIF findNetworkInterface(HardwareAbstractionLayer hardware){
        try{
            for(NetworkIF network : hardware.getNetworkIFs()){
                String[] ip4Addr = network.getIPv4addr();
                NetworkInterface ni = network.queryNetworkInterface();
                if(!ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual() && ni.isUp()
                        && ip4Addr.length > 0){
                    return network;
                }
            }
        }catch (IOException e){
            log.error("读取网络信息时出错",e);
        }
        return null;
    }
}
