package com.litongjava.tio.core.ssl;

import com.litongjava.aio.Packet;
import com.litongjava.tio.core.TioConfig;

/**
 * @author tanyaowu
 *
 */
public class SslUtils {

	private SslUtils() {

	}

	/**
	 * 是否需要对这个packet进行SSL加密 
	 * @param packet
	 * @param tioConfig
	 * @return
	 */
	public static boolean needSslEncrypt(Packet packet, TioConfig tioConfig) {
		if (!packet.isSslEncrypted() && tioConfig.sslConfig != null) {
			return true;
		}
		return false;
	}

	/**
	 * 是否是SSL连接
	 * @param tioConfig
	 * @return
	 */
  public static boolean isSsl(TioConfig tioConfig) {
		return tioConfig.isSsl();
	}

}
