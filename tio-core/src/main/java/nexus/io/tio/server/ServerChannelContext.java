package nexus.io.tio.server;

import java.nio.channels.AsynchronousSocketChannel;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.TioConfig;

/**
 *
 * @author tanyaowu
 *
 */
public class ServerChannelContext extends ChannelContext {

  /**
   * 创建一个虚拟ChannelContext，主要用来模拟一些操作，真实场景中用得少
   * @param tioConfig
   */
  public ServerChannelContext(TioConfig tioConfig) {
    super(tioConfig);
  }

  /**
   * @param tioConfig
   * @param asynchronousSocketChannel
   */
  public ServerChannelContext(TioConfig tioConfig, AsynchronousSocketChannel asynchronousSocketChannel) {
    super(tioConfig, asynchronousSocketChannel);
  }

  public ServerChannelContext(TioConfig tioConfig, AsynchronousSocketChannel clientSocketChannel, String clientIp, int port) {
    super(tioConfig, clientSocketChannel, clientIp, port);
  }

  /**
   * 创建一个虚拟ChannelContext，主要用来模拟一些操作，譬如压力测试，真实场景中用得少
   * @param tioConfig
   * @param id ChannelContext id
   * @author tanyaowu
   */
  public ServerChannelContext(TioConfig tioConfig, String id) {
    super(tioConfig, id);
  }


  /** 
   * @return
   * @author tanyaowu
   */
  @Override
  public boolean isServer() {
    return true;
  }

}
