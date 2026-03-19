package com.litongjava.tio.websocket.common;

import java.util.concurrent.ThreadLocalRandom;

import com.litongjava.aio.AioId;
import com.litongjava.tio.utils.hutool.Snowflake;


public class WebSocketSnowflakeId implements AioId {
  private Snowflake snowflake;

  public WebSocketSnowflakeId() {
    snowflake = new Snowflake(ThreadLocalRandom.current().nextInt(1, 30), ThreadLocalRandom.current().nextInt(1, 30));
  }

  public WebSocketSnowflakeId(long workerId, long datacenterId) {
    snowflake = new Snowflake(workerId, datacenterId);
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public String id() {
    return snowflake.nextId() + "";
  }
}
