package com.litongjava.tio.core.ssl.facade;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.ssl.SslVo;

public class Handshaker {
  /*
   * The purpose of this class is to conduct a SSL handshake. To do this it
   * requires a SSLEngine as a provider of SSL knowhow. Byte buffers that are
   * required by the SSLEngine to execute its wrap and unwrap methods. And a
   * ITaskHandler callback that is used to delegate the responsibility of
   * executing long-running/IO tasks to the host application. By providing a
   * ITaskHandler the host application gains the flexibility of executing these
   * tasks in compliance with its own compute/IO strategies.
   */

  private static Logger log = LoggerFactory.getLogger(Handshaker.class);
  private final ITaskHandler _taskHandler;
  private final SSLFacdeWorker _worker;
  private boolean _finished;
  private IHandshakeCompletedListener _hscl;
  private ISessionClosedListener _sessionClosedListener;
  @SuppressWarnings("unused")
  private boolean _client;
  private ChannelContext channelContext;

  public Handshaker(boolean client, SSLFacdeWorker worker, ITaskHandler taskHandler, ChannelContext channelContext) {
    this.channelContext = channelContext;
    _worker = worker;
    _taskHandler = taskHandler;
    _finished = false;
    _client = client;
  }

  private void debug(final String msg, final String... args) {
    SSLLog.debug(channelContext.toString(), msg, args);
  }

  void begin() throws SSLException {
    _worker.beginHandshake();
    shakehands();
  }

  void carryOn() throws SSLException {
    shakehands();
  }

  void handleUnwrapResult(SSLEngineResult result) throws SSLException {
    if (result.getHandshakeStatus().equals(SSLEngineResult.HandshakeStatus.FINISHED)) {
      handshakeFinished(); // 客户端会走到这一行
    } else {
      shakehands();
    }
  }

  void addCompletedListener(IHandshakeCompletedListener hscl) {
    _hscl = hscl;
  }

  void removeCompletedListener(IHandshakeCompletedListener hscl) {
    _hscl = hscl;
  }

  boolean isFinished() {
    return _finished;
  }

  /**
   * 
   * @throws SSLException
   */
  private void shakehands() throws SSLException {
    HandshakeStatus handshakeStatus = _worker.getHandshakeStatus();
    //log.info("{}, handshakeStatus:{}", this.channelContext, handshakeStatus);

    switch (handshakeStatus) {
      case NOT_HANDSHAKING:
        /*
         * Occurs after handshake is over.
         * 关键：这里必须兜底。
         * 某些情况下 FINISHED 不一定会被你捕捉到（尤其是 TLS1.3 / 状态切换时机差异），但最终会进入 NOT_HANDSHAKING。
         * 如果此时还没标记完成，会导致上层一直等待握手完成。
         */
        if (!_finished) {
          handshakeFinished();
        } else {
          // 已完成则确保 completed 标志存在（防止 handshakeFinished() 里因空指针等未设置）
          markHandshakeCompleted();
        }
        break;

      case FINISHED: // 握手刚刚完成
        handshakeFinished();
        break;

      case NEED_TASK: // 运行任务
        _taskHandler.process(new Tasks(_worker, this));
        break;

      case NEED_WRAP: // 加密
        SSLEngineResult w_result = _worker.wrap(new SslVo(), null);
        if (w_result.getStatus().equals(SSLEngineResult.Status.CLOSED) && null != _sessionClosedListener) {
          _sessionClosedListener.onSessionClosed();
        }
        if (w_result.getHandshakeStatus().equals(SSLEngineResult.HandshakeStatus.FINISHED)) {
          handshakeFinished();
        } else {
          shakehands();
        }
        break;

      case NEED_UNWRAP:
        if (_worker.pendingUnwrap()) {
          SSLEngineResult u_result = _worker.unwrap(null);
          debug("Unwrap result " + u_result);
          if (u_result.getHandshakeStatus().equals(SSLEngineResult.HandshakeStatus.FINISHED)) {
            handshakeFinished();
          }
          if (u_result.getStatus().equals(SSLEngineResult.Status.OK)) {
            shakehands();
          }
        } else {
          debug("No pending data to unwrap");
        }
        break;
    }
  }

  /**
   * 只负责把 sslFacadeContext 的握手完成标志置为 true（幂等）
   */
  private void markHandshakeCompleted() {
    try {
      if (channelContext != null
          && channelContext.sslFacadeContext != null
          && !channelContext.sslFacadeContext.isHandshakeCompleted()) {
        channelContext.sslFacadeContext.setHandshakeCompleted(true);
        log.info("{}, SSL handshake completed flag set", channelContext);
      }
    } catch (Throwable t) {
      // 这里不要让异常影响握手状态机
      log.warn("{}, Failed to set handshakeCompleted flag: {}", channelContext, t.toString());
    }
  }

  /**
   * 握手完成：幂等，避免重复回调/重复设置
   */
  private synchronized void handshakeFinished() {
    if (_finished) {
      // 已完成则确保标志存在
      markHandshakeCompleted();
      return;
    }

    _finished = true;

    // 先标记完成，保证上层等待能放行
    markHandshakeCompleted();

    // 再通知监听器（避免监听器异常导致标志没设上）
    try {
      if (_hscl != null) {
        _hscl.onComplete();
      } else {
        log.warn("{}, handshake finished but no IHandshakeCompletedListener is set", channelContext);
      }
    } catch (Throwable t) {
      log.error("{}, exception in handshake completion listener", channelContext, t);
    }
  }
}
