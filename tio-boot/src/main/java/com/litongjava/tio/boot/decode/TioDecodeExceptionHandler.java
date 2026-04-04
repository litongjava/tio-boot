package com.litongjava.tio.boot.decode;

import java.nio.ByteBuffer;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.exception.TioDecodeException;
import nexus.io.tio.http.common.HttpConfig;

public interface TioDecodeExceptionHandler {

  void handle(ByteBuffer buffer, ChannelContext channelContext, HttpConfig httpConfig, TioDecodeException e);

}
