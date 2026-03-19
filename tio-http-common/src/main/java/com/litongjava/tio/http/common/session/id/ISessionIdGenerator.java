package com.litongjava.tio.http.common.session.id;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;

/**
 * @author tanyaowu
 * 2017年8月15日 上午10:49:58
 */
public interface ISessionIdGenerator {

	/**
	 *
	 * @return
	 * @author tanyaowu
	 */
	String sessionId(HttpConfig httpConfig, HttpRequest request);

}
