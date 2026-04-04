package nexus.io.tio.http.common.session.id.impl;

import java.util.UUID;

import nexus.io.tio.http.common.HttpConfig;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.session.id.ISessionIdGenerator;

/**
 * @author tanyaowu
 * 2017年8月15日 上午10:53:39
 */
public class UUIDSessionIdGenerator implements ISessionIdGenerator {
  public final static UUIDSessionIdGenerator INSTANCE = new UUIDSessionIdGenerator();

  /**
   *
   * @author tanyaowu
   */
  private UUIDSessionIdGenerator() {
  }

  /**
   * @return
   * @author tanyaowu
   */
  @Override
  public String sessionId(HttpConfig httpConfig, HttpRequest request) {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
