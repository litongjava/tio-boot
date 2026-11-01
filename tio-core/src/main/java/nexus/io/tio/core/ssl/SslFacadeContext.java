package nexus.io.tio.core.ssl;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.ssl.facade.DefaultTaskHandler;
import nexus.io.tio.core.ssl.facade.ISSLFacade;
import nexus.io.tio.core.ssl.facade.SSLFacade;
import nexus.io.tio.core.task.DecodeTask;

public class SslFacadeContext {
  private static Logger log = LoggerFactory.getLogger(SslFacadeContext.class);

  private ChannelContext channelContext = null;

  private SSLContext sslContext;
  private ISSLFacade sslFacade = null;

  // ssl握手是否已经完成, true: 已经完成， false: 还没有完成
  private volatile boolean handshakeCompleted = false;

  /**
   * Application data decrypted before the handshake is marked complete. With
   * TLS 1.3 the client sends its first request right after its Finished
   * message; when both arrive in one read, the request is decrypted before the
   * server finishes the handshake. It must be decoded later, not dropped.
   */
  private final Queue<ByteBuffer> earlyPlainData = new ArrayDeque<>();

  // 关键：同一条连接唯一的 DecodeTask（用于累计半包）
  private final DecodeTask decodeTask;

  public SslFacadeContext(ChannelContext channelContext) throws Exception {
    this(channelContext, null);
  }
  
  public SslFacadeContext(ChannelContext channelContext, DecodeTask decodeTask) throws Exception {
    this.channelContext = channelContext;
    this.channelContext.setSslFacadeContext(this);

    this.handshakeCompleted = false;
    this.decodeTask = decodeTask == null ? new DecodeTask() : decodeTask;

    sslContext = SSLContext.getInstance("TLS");
    KeyManager[] keyManagers = channelContext.tioConfig.sslConfig.getKeyManagerFactory().getKeyManagers();
    TrustManager[] trustManagers = channelContext.tioConfig.sslConfig.getTrustManagerFactory().getTrustManagers();
    sslContext.init(keyManagers, trustManagers, null);

    DefaultTaskHandler taskHandler = new DefaultTaskHandler();

    boolean isClient = true;
    if (this.channelContext.isServer()) {
      isClient = false;
    }

    sslFacade = new SSLFacade(this.channelContext, sslContext, isClient, false, taskHandler);
    sslFacade.setHandshakeCompletedListener(new SslHandshakeCompletedListener(this.channelContext));
    sslFacade.setSSLListener(new SslListener(this.channelContext));
    sslFacade.setCloseListener(new SslSessionClosedListener(this.channelContext));
  }

  public void beginHandshake() throws Exception {
    log.info("Start SSL Handshake {}", channelContext);
    sslFacade.beginHandshake();
  }

  public boolean isHandshakeCompleted() {
    return handshakeCompleted;
  }

  public void setHandshakeCompleted(boolean handshakeCompleted) {
    this.handshakeCompleted = handshakeCompleted;
    if (handshakeCompleted) {
      flushEarlyPlainData();
    }
  }

  /**
   * Decodes decrypted application data, or holds it until the handshake is
   * marked complete. Held data is decoded first, so arrival order is kept.
   */
  public void onPlainData(ByteBuffer plainBuffer) {
    synchronized (earlyPlainData) {
      if (!handshakeCompleted) {
        earlyPlainData.add(plainBuffer);
        log.debug("{}, holding {} decrypted bytes until the SSL handshake completes", channelContext,
            plainBuffer.remaining());
        return;
      }
      decodeEarlyPlainData();
      decodeTask.decode(channelContext, plainBuffer);
    }
  }

  private void flushEarlyPlainData() {
    synchronized (earlyPlainData) {
      decodeEarlyPlainData();
    }
  }

  private void decodeEarlyPlainData() {
    ByteBuffer buffer;
    while ((buffer = earlyPlainData.poll()) != null) {
      decodeTask.decode(channelContext, buffer);
    }
  }

  public DecodeTask getDecodeTask() {
    return decodeTask;
  }

  public ChannelContext getChannelContext() {
    return channelContext;
  }

  public SSLContext getSslContext() {
    return sslContext;
  }

  public ISSLFacade getSslFacade() {
    return sslFacade;
  }
}