package com.litongjava.tio.utils.http.useragent;

import java.util.regex.Pattern;

import com.litongjava.model.http.useragent.UserAgent;
import com.litongjava.model.http.useragent.UserAgentBrowser;
import com.litongjava.model.http.useragent.UserAgentEngine;
import com.litongjava.model.http.useragent.UserAgentOS;
import com.litongjava.model.http.useragent.UserAgentPlatform;
import com.litongjava.tio.utils.hutool.ReUtil;


/**
 * User-Agent解析器
 * 
 * @author looly
 * @since 4.2.1
 */
public class UserAgentParser {

	/**
	 * 解析User-Agent
	 * 
	 * @param userAgentString User-Agent字符串
	 * @return {@link UserAgent}
	 */
	public static UserAgent parse(String userAgentString) {
		final UserAgent userAgent = new UserAgent();
		
		final UserAgentBrowser browser = parseBrowser(userAgentString);
		userAgent.setBrowser(parseBrowser(userAgentString));
		userAgent.setVersion(browser.getVersion(userAgentString));
		
		final UserAgentEngine engine = parseEngine(userAgentString);
		userAgent.setEngine(engine);
		if (false == engine.isUnknown()) {
			userAgent.setEngineVersion(parseEngineVersion(engine, userAgentString));
		}
		userAgent.setOs(parseOS(userAgentString));
		final UserAgentPlatform platform = parsePlatform(userAgentString);
		userAgent.setPlatform(platform);
		userAgent.setMobile(platform.isMobile() || browser.isMobile());
		

		return userAgent;
	}

	/**
	 * 解析浏览器类型
	 * 
	 * @param userAgentString User-Agent字符串
	 * @return 浏览器类型
	 */
	private static UserAgentBrowser parseBrowser(String userAgentString) {
		for (UserAgentBrowser brower : UserAgentBrowser.browers) {
			if (brower.isMatch(userAgentString)) {
				return brower;
			}
		}
		return UserAgentBrowser.Unknown;
	}
	
	/**
	 * 解析引擎类型
	 * 
	 * @param userAgentString User-Agent字符串
	 * @return 引擎类型
	 */
	private static UserAgentEngine parseEngine(String userAgentString) {
		for (UserAgentEngine engine : UserAgentEngine.engines) {
			if (engine.isMatch(userAgentString)) {
				return engine;
			}
		}
		return UserAgentEngine.Unknown;
	}

	/**
	 * 解析引擎版本
	 * 
	 * @param engine 引擎
	 * @param userAgentString User-Agent字符串
	 * @return 引擎版本
	 */
	private static String parseEngineVersion(UserAgentEngine engine, String userAgentString) {
		final String regexp = engine.getName() + "[\\/\\- ]([\\d\\w\\.\\-]+)";
		final Pattern pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
		return ReUtil.getGroup1(pattern, userAgentString);
	}

	/**
	 * 解析系统类型
	 * 
	 * @param userAgentString User-Agent字符串
	 * @return 系统类型
	 */
	private static UserAgentOS parseOS(String userAgentString) {
		for (UserAgentOS os : UserAgentOS.oses) {
			if (os.isMatch(userAgentString)) {
				return os;
			}
		}
		return UserAgentOS.Unknown;
	}

	/**
	 * 解析平台类型
	 * 
	 * @param userAgentString User-Agent字符串
	 * @return 平台类型
	 */
	private static UserAgentPlatform parsePlatform(String userAgentString) {
		for (UserAgentPlatform platform : UserAgentPlatform.platforms) {
			if (platform.isMatch(userAgentString)) {
				return platform;
			}
		}
		return UserAgentPlatform.Unknown;
	}
}
