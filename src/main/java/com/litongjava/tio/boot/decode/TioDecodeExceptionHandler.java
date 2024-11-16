package com.litongjava.tio.boot.decode;

import java.nio.ByteBuffer;

import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.exception.TioDecodeException;
import com.litongjava.tio.http.common.HttpConfig;

public interface TioDecodeExceptionHandler {

  void handle(ByteBuffer buffer, ChannelContext channelContext, HttpConfig httpConfig, TioDecodeException e);

}
