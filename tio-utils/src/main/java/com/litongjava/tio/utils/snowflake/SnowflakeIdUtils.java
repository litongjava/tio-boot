package com.litongjava.tio.utils.snowflake;

import java.util.Random;

public class SnowflakeIdUtils {
  // Singleton instance of the SnowflakeId generator
  private static final SnowflakeIdUtils snowflakeIdGenerator = new SnowflakeIdUtils(randomInt(1, 30), randomInt(1, 30));
  // Start timestamp (can be set to the time the program starts or any fixed value)
  private static final long START_TIMESTAMP = 1625076000000L; // 2021-07-01 00:00:00
  // Bit allocation for each part
  private static final long SEQUENCE_BITS = 12; // Bits allocated for sequence number
  private static final long WORKER_ID_BITS = 5; // Bits allocated for worker ID
  private static final long DATACENTER_ID_BITS = 5; // Bits allocated for datacenter ID
  // Maximum values for each part
  private static final long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BITS);
  private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);
  private static final long MAX_DATACENTER_ID = -1L ^ (-1L << DATACENTER_ID_BITS);
  // Bit shifts for each part
  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
  // Worker ID and Datacenter ID
  private long workerId;
  private long datacenterId;
  // Sequence number and the last timestamp when an ID was generated
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public SnowflakeIdUtils(long workerId, long datacenterId) {
    if (workerId > MAX_WORKER_ID || workerId < 0) {
      throw new IllegalArgumentException("Worker ID can't be greater than " + MAX_WORKER_ID + " or less than 0");
    }
    if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
      throw new IllegalArgumentException("Datacenter ID can't be greater than " + MAX_DATACENTER_ID + " or less than 0");
    }
    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  public synchronized long generateId() {
    long currentTimestamp = System.currentTimeMillis();
    if (currentTimestamp < lastTimestamp) {
      throw new RuntimeException("Clock moved backwards. Refusing to generate ID");
    }
    if (currentTimestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        // The sequence number for the current millisecond has reached its maximum, wait for the next millisecond
        currentTimestamp = getNextTimestamp(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }
    lastTimestamp = currentTimestamp;
    return (currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT | datacenterId << DATACENTER_ID_SHIFT | workerId << WORKER_ID_SHIFT | sequence;
  }

  private long getNextTimestamp(long lastTimestamp) {
    long currentTimestamp = System.currentTimeMillis();
    while (currentTimestamp <= lastTimestamp) {
      currentTimestamp = System.currentTimeMillis();
    }
    return currentTimestamp;
  }

  public static int randomInt(int min, int max) {
    Random random = new Random();
    int randomNumber = random.nextInt(max - min + 1) + min;
    return randomNumber;
  }

  public static long id() {
    return snowflakeIdGenerator.generateId();
  }
}
