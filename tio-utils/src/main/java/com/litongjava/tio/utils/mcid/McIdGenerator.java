package com.litongjava.tio.utils.mcid;

public class McIdGenerator {
  private final int machineId;
  private int sequence = 0;
  private long lastTimestamp = -1L;

  public McIdGenerator(int machineId) {
    if (machineId < 0 || machineId > 255) { // 8位机器/进程ID限制
      throw new IllegalArgumentException("Machine ID must be between 0 and 255");
    }
    this.machineId = machineId;
  }

  public synchronized long generateId() {
    long timestamp = System.currentTimeMillis();

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & 15; // 4位序列号
      if (sequence == 0) {
        while (System.currentTimeMillis() <= timestamp) {
          // 等待下一毫秒
        }
        timestamp = System.currentTimeMillis();
      }
    } else {
      sequence = 0;
    }

    lastTimestamp = timestamp;
    // 41位时间戳 | 8位机器/进程ID | 4位序列号
    return ((timestamp & 0x1FFFFFFFFFFL) << 12) | (machineId << 4) | sequence;
  }
}