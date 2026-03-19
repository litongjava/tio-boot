package com.litongjava.tio.core.ssl.facade;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.consts.TioConst;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Node;
import com.litongjava.tio.core.ssl.SslVo;
import com.litongjava.tio.core.utils.ByteBufferUtils;

public class SSLFacade implements ISSLFacade {
  private static final Logger log = LoggerFactory.getLogger(SSLFacade.class);


  private final AtomicLong sslSeq = new AtomicLong();

  private Handshaker _handshaker;
  private IHandshakeCompletedListener _hcl;
  private final SSLFacdeWorker _worker;
  private final boolean _clientMode;
  private final ChannelContext channelContext;

  public SSLFacade(ChannelContext channelContext, SSLContext context, boolean client, boolean clientAuthRequired,
      ITaskHandler taskHandler) {
    this.channelContext = channelContext;
    final String who = client ? "client" : "server";

    SSLEngine engine = makeSSLEngine(context, client, clientAuthRequired, channelContext);

    Buffers buffers = new Buffers(engine.getSession(), channelContext);
    _worker = new SSLFacdeWorker(who, engine, buffers, channelContext);
    _handshaker = new Handshaker(client, _worker, taskHandler, channelContext);
    _clientMode = client;
  }

  @Override
  public boolean isClientMode() {
    return _clientMode;
  }

  @Override
  public void setHandshakeCompletedListener(IHandshakeCompletedListener hcl) {
    _hcl = hcl;
    attachCompletionListener();
  }

  @Override
  public void setSSLListener(ISSLListener l) {
    _worker.setSSLListener(l);
  }

  @Override
  public void setCloseListener(ISessionClosedListener l) {
    _worker.setSessionClosedListener(l);
  }

  @Override
  public void beginHandshake() throws SSLException {
    _handshaker.begin();
  }

  @Override
  public boolean isHandshakeCompleted() {
    return (_handshaker == null) || _handshaker.isFinished();
  }

  @Override
  public void encrypt(SslVo sslVo) throws SSLException {
    long seq = sslSeq.incrementAndGet();

    ByteBuffer src = sslVo.getByteBuffer();
    ByteBuffer[] byteBuffers = ByteBufferUtils.split(src, 1024 * 8);
    if (byteBuffers == null) {
      SSLEngineResult result = _worker.wrap(sslVo, sslVo.getByteBuffer());
      log.debug("{}, SSL wrap seq={}, result={}", channelContext, channelContext.getId() + "_" + seq, result);
    } else {
      ByteBuffer[] encryptedByteBuffers = new ByteBuffer[byteBuffers.length];
      int alllen = 0;
      for (int i = 0; i < byteBuffers.length; i++) {
        SslVo sslVo1 = new SslVo(byteBuffers[i], sslVo.getObj());
        SSLEngineResult result = _worker.wrap(sslVo1, byteBuffers[i]);
        ByteBuffer encryptedByteBuffer = sslVo1.getByteBuffer();
        encryptedByteBuffers[i] = encryptedByteBuffer;
        alllen += encryptedByteBuffer.limit();
        log.debug("{}, SSL wrap split seq={}, part={}, result={}", channelContext, channelContext.getId() + "_" + seq,
            (i + 1), result);
      }

      ByteBuffer encryptedByteBuffer = ByteBuffer.allocate(alllen);
      for (ByteBuffer bb : encryptedByteBuffers) {
        encryptedByteBuffer.put(bb);
      }
      encryptedByteBuffer.flip();
      sslVo.setByteBuffer(encryptedByteBuffer);
    }
  }

  @Override
  public void decrypt(ByteBuffer byteBuffer) throws SSLException {
    long seq = sslSeq.incrementAndGet();
    SSLEngineResult result = _worker.unwrap(byteBuffer);
    log.debug("{}, SSL unwrap seq={}, result={}", channelContext, channelContext.getId() + "_" + seq, result);
    _handshaker.handleUnwrapResult(result);
  }

  @Override
  public void close() {
    _worker.close(true);
  }

  @Override
  public boolean isCloseCompleted() {
    return _worker.isCloseCompleted();
  }

  @Override
  public void terminate() {
    _worker.close(false);
  }

  private void attachCompletionListener() {
    _handshaker.addCompletedListener(new IHandshakeCompletedListener() {
      @Override
      public void onComplete() {
        if (_hcl != null) {
          _hcl.onComplete();
          _hcl = null;
        }
      }
    });
  }

  private SSLEngine makeSSLEngine(SSLContext context, boolean client, boolean clientAuthRequired, ChannelContext cc) {
    SSLEngine engine;

    if (client) {
      String peerHost = null;
      int peerPort = -1;

      // 1) 优先用显式 TLS 目标（防止代理场景 serverNode 变成 127.0.0.1）
      try {
        Object h = (cc != null) ? cc.getAttribute(TioConst.ATTR_TLS_PEER_HOST) : null;
        Object p = (cc != null) ? cc.getAttribute(TioConst.ATTR_TLS_PEER_PORT) : null;
        if (h instanceof String) {
          peerHost = (String) h;
        }
        if (p instanceof Integer) {
          peerPort = (Integer) p;
        } else if (p instanceof String) {
          try {
            peerPort = Integer.parseInt((String) p);
          } catch (Exception ignore) {
          }
        }
      } catch (Throwable ignore) {
      }

      // 2) fallback：用 serverNode（正常情况下就是目标域名:443）
      if (peerHost == null || peerPort <= 0) {
        try {
          Node serverNode = (cc != null) ? cc.getServerNode() : null;
          if (serverNode != null) {
            peerHost = serverNode.getHost();
            peerPort = serverNode.getPort();
          }
        } catch (Throwable ignore) {
        }
      }

      if (peerHost != null && peerPort > 0) {
        engine = context.createSSLEngine(peerHost, peerPort);
        log.info("{}, SSLEngine peerHost={}, peerPort={}", channelContext, peerHost, peerPort);
      } else {
        engine = context.createSSLEngine();
        log.warn("{}, createSSLEngine() without peerHost/peerPort, SNI may be missing", channelContext);
      }

      engine.setUseClientMode(true);

      // 可选：开启 HTTPS 域名校验（建议），但只在看起来像域名时启用，避免 IP/localhost 噪音
      try {
        if (peerHost != null && looksLikeHostname(peerHost)) {
          SSLParameters params = engine.getSSLParameters();
          params.setEndpointIdentificationAlgorithm("HTTPS");
          engine.setSSLParameters(params);
        }
      } catch (Throwable t) {
        log.debug("{}, unable to set endpointIdentificationAlgorithm: {}", channelContext, t.toString());
      }

    } else {
      engine = context.createSSLEngine();
      engine.setUseClientMode(false);
      engine.setNeedClientAuth(clientAuthRequired);
    }

    return engine;
  }

  private static boolean looksLikeHostname(String host) {
    if (host == null)
      return false;
    String h = host.trim();
    if (h.isEmpty())
      return false;
    if ("localhost".equalsIgnoreCase(h))
      return false;
    // 粗略判断：含字母/连字符/点，且不是纯数字点号形式
    boolean hasLetter = false;
    for (int i = 0; i < h.length(); i++) {
      char c = h.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
        hasLetter = true;
        break;
      }
    }
    if (!hasLetter)
      return false; // 防止 127.0.0.1 这种
    return h.indexOf('.') >= 0;
  }
}
