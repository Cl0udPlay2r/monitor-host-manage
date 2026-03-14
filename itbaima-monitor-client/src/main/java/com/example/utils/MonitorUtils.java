package com.example.utils;


import com.example.entity.BaseDetail;
import com.example.entity.RuntimeDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;
import oshi.hardware.CentralProcessor.TickType;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Date;
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

    /**
     * 返回运行时数据
     * @return RuntimeDetail
     */
    public RuntimeDetail  monitorRuntimeDetail(){
        double statisticTime = 0.5;
        try{
            HardwareAbstractionLayer hardware = info.getHardware();
            NetworkIF networkIF = Objects.requireNonNull(this.findNetworkInterface(hardware));
            CentralProcessor processor = hardware.getProcessor();
            double upload = networkIF.getBytesSent() , download = networkIF.getBytesRecv() ;
            double read = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum();
            double write = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum();
            long[] ticks = processor.getSystemCpuLoadTicks();
            Thread.sleep((long) (statisticTime * 1000));
            networkIF = Objects.requireNonNull(this.findNetworkInterface(hardware));
            upload = (networkIF.getBytesSent() - upload) / statisticTime;
            download = (networkIF.getBytesRecv() - download) / statisticTime;
            read = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum() - read) / statisticTime;
            write = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum() - write) / statisticTime;
            double memory = (hardware.getMemory().getTotal() - hardware.getMemory().getAvailable()) / 1024.0 / 1024 / 1024;
            double disk = Arrays.stream(File.listRoots())
                    .mapToDouble(file -> file.getTotalSpace() - file.getFreeSpace()).sum() / 1024.0 / 1024;
            return new RuntimeDetail()
                    .setCpuUsage(this.calculateCpuUsage(processor,ticks))
                    .setDiskUsage(disk)
                    .setMemoryUsage(memory)
                    .setNetworkUpload(upload/1024)
                    .setNetworkDownload(download / 1024)
                    .setDiskRead(read / 1024 /1024)
                    .setDiskWrite(write / 1024 /1024)
                    .setTimestamp(new Date().getTime());
        }catch (InterruptedException e) {
            log.error("读取运行时信息出现错误:{}", String.valueOf(e));
            return null;
        }
    }

    private double calculateCpuUsage(CentralProcessor processor,long[] prevTicks) {
        long[] ticks = processor.getSystemCpuLoadTicks();

        // 计算总时间差
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];

        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

        // 计算使用率: (总时间 - 空闲时间) / 总时间
        double cpuUsage = 0;
        if (totalCpu > 0) {
            cpuUsage = (double) (totalCpu - idle) / totalCpu;
        }

        return  cpuUsage*100;
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
