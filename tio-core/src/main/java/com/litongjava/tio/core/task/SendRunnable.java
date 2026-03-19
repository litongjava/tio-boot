//package com.litongjava.tio.core.task;
//
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.Executor;
//import java.util.concurrent.locks.ReentrantLock;
//
//import javax.net.ssl.SSLException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.litongjava.aio.Packet;
//import com.litongjava.tio.core.ChannelContext;
//import com.litongjava.tio.core.ChannelContext.CloseCode;
//import com.litongjava.tio.core.TcpConst;
//import com.litongjava.tio.core.Tio;
//import com.litongjava.tio.core.TioConfig;
//import com.litongjava.tio.core.WriteCompletionHandler.WriteCompletionVo;
//import com.litongjava.tio.core.intf.AioHandler;
//import com.litongjava.tio.core.ssl.SslUtils;
//import com.litongjava.tio.core.ssl.SslVo;
//import com.litongjava.tio.core.utils.TioUtils;
//import com.litongjava.tio.utils.queue.FullWaitQueue;
//import com.litongjava.tio.utils.queue.TioFullWaitQueue;
//import com.litongjava.tio.utils.thread.pool.AbstractQueueRunnable;
//
///**
// *
// * @author tanyaowu
// * 2017年4月4日 上午9:19:18
// */
//public class SendRunnable extends AbstractQueueRunnable<Packet> {
//  private static final Logger log = LoggerFactory.getLogger(SendRunnable.class);
//  
//  /** The msg queue. */
//   // new ConcurrentLinkedQueue<>();
//  public boolean canSend = true;
//
//
//
//  /**
//   *
//   * @param channelContext
//   * @param executor
//   * @author tanyaowu
//   */
//  public SendRunnable(ChannelContext channelContext, Executor executor) {
//    super(executor);
//    
//    getMsgQueue();
//  }
//
//  @Override
//  public boolean addMsg(Packet packet) {
//    if (this.isCanceled()) {
//      log.info("{}, The mission has been canceled，{} Failed to add to the send queue. Procedure", channelContext, packet.logstr());
//      return false;
//    }
//
//    if (channelContext.sslFacadeContext != null && !channelContext.sslFacadeContext.isHandshakeCompleted() && SslUtils.needSslEncrypt(packet, tioConfig)) {
//      return this.getForSendAfterSslHandshakeCompleted(true).add(packet);
//    } else {
//      return msgQueue.add(packet);
//    }
//  }
//
//  /**
//   * 清空消息队列
//   */
//  @Override
//  public void clearMsgQueue() {
//    Packet p = null;
//    forSendAfterSslHandshakeCompleted = null;
//    while ((p = msgQueue.poll()) != null) {
//      try {
//        channelContext.processAfterSent(p, false);
//      } catch (Throwable e) {
//        log.error(e.toString(), e);
//      }
//    }
//  }
//
//  /**
//   * 新旧值是否进行了切换
//   * @param oldValue
//   * @param newValue
//   * @return
//   */
//
//  //减掉1024是尽量防止溢出的一小部分还分成一个tcp包发出
//  private static final int MAX_CAPACITY_MIN = TcpConst.MAX_DATA_LENGTH - 1024;
//  private static final int MAX_CAPACITY_MAX = MAX_CAPACITY_MIN * 10;
//
//  @Override
//  public void runTask() {
//
//
//    int listInitialCapacity = Math.min(queueSize, canSend ? 300 : 1000);
//
//    Packet packet = null;
//    List<Packet> packets = new ArrayList<>(listInitialCapacity);
//    List<ByteBuffer> byteBuffers = new ArrayList<>(listInitialCapacity);
//    // int packetCount = 0;
//    int allBytebufferCapacity = 0;
//    Boolean needSslEncrypted = null;
//    boolean sslChanged = false;
//    
//    while ((packet = msgQueue.poll()) != null) {
//      ByteBuffer byteBuffer = getByteBuffer(packet);
//
//      packets.add(packet);
//      byteBuffers.add(byteBuffer);
//      // packetCount++;
//      allBytebufferCapacity += byteBuffer.limit();
//
//      if (isSsl) {
//        boolean _needSslEncrypted = !packet.isSslEncrypted();
//        if (needSslEncrypted == null) {
//          sslChanged = false;
//        } else {
//          sslChanged = needSslEncrypted != _needSslEncrypted;
//        }
//        needSslEncrypted = _needSslEncrypted;
//      }
//
//      if ((canSend && allBytebufferCapacity >= MAX_CAPACITY_MIN) || (allBytebufferCapacity >= MAX_CAPACITY_MAX) || sslChanged) {
//        break;
//      }
//    }
//
//    // System.out.println(packets.size());
//    if (allBytebufferCapacity == 0) {
//      return;
//    }
//    ByteBuffer allByteBuffer = ByteBuffer.allocate(allBytebufferCapacity);
//    for (ByteBuffer byteBuffer : byteBuffers) {
//      allByteBuffer.put(byteBuffer);
//    }
//
//    allByteBuffer.flip();
//
//    if (isSsl && needSslEncrypted) {
//      SslVo sslVo = new SslVo(allByteBuffer, packets);
//      try {
//        channelContext.sslFacadeContext.getSslFacade().encrypt(sslVo);
//        allByteBuffer = sslVo.getByteBuffer();
//      } catch (SSLException e) {
//        log.error(channelContext.toString() + ", An exception occurred during SSL encryption.", e);
//        Tio.close(channelContext, "An exception occurred during SSL encryption.", CloseCode.SSL_ENCRYPTION_ERROR);
//        return;
//      }
//    }
//
//    this.sendByteBuffer(allByteBuffer, packets);
//  }
//}
