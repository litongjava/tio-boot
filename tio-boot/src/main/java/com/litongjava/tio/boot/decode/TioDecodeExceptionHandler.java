package com.litongjava.tio.boot.decode;

import java.nio.ByteBuffer;

import com.litongjava.tio.http.common.HttpConfig;

import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.exception.TioDecodeException;

public interface TioDecodeExceptionHandler {

  void handle(ByteBuffer buffer, ChannelContext channelContext, HttpConfig httpConfig, TioDecodeException e);

}
