package com.litongjava.tio.core.exception;

/**
 *
 * @author tanyaowu
 * 2017年4月1日 上午9:33:24
 */
public class TioDecodeException extends java.lang.Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8207465969738755041L;

	/**
	 *
	 *
	 * @author tanyaowu
	 *
	 */
	public TioDecodeException() {
	}

	/**
	 * @param message
	 *
	 * @author tanyaowu
	 *
	 */
	public TioDecodeException(String message) {
		super(message);

	}

	/**
	 * @param message
	 * @param cause
	 *
	 * @author tanyaowu
	 *
	 */
	public TioDecodeException(String message, Throwable cause) {
		super(message, cause);

	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 *
	 * @author tanyaowu
	 *
	 */
	public TioDecodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	/**
	 * @param cause
	 *
	 * @author tanyaowu
	 *
	 */
	public TioDecodeException(Throwable cause) {
		super(cause);

	}

}
