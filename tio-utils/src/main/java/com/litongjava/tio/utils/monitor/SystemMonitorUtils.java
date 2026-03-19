package com.litongjava.tio.utils.monitor;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

import com.litongjava.model.sys.SystemInfo;

public class SystemMonitorUtils {

  @SuppressWarnings("restriction")
  public static SystemInfo getSystemInfo() {
    SystemInfo info = new SystemInfo();

    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    // 获取CPU信息
    info.setAvailableProcessors(osBean.getAvailableProcessors());
    info.setSystemLoadAverage(osBean.getSystemLoadAverage());

    // CPU使用率（仅适用于某些实现，下面这个是一个大致的实现，需要依赖第三方库）
    com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) osBean;
    info.setCpuUsage((long) (os.getSystemCpuLoad() * 100));

    // 获取内存信息
    info.setTotalPhysicalMemorySize(os.getTotalPhysicalMemorySize());
    info.setFreePhysicalMemorySize(os.getFreePhysicalMemorySize());
    MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
    info.setHeapMemoryUsed(heapMemoryUsage.getUsed());
    info.setHeapMemoryMax(heapMemoryUsage.getMax());
    MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
    info.setNonHeapMemoryUsed(nonHeapMemoryUsage.getUsed());
    info.setNonHeapMemoryMax(nonHeapMemoryUsage.getMax());

    // 获取JVM信息
    info.setJavaVersion(System.getProperty("java.version"));
    info.setJvmVendor(System.getProperty("java.vendor"));
    info.setJvmUptime(runtimeBean.getUptime());

    // 获取操作系统信息
    info.setOsName(osBean.getName());
    info.setOsVersion(osBean.getVersion());
    info.setOsArch(osBean.getArch());

    // 获取磁盘信息
    File[] roots = File.listRoots();
    info.setFileRoots(roots);

    return info;
  }
}
