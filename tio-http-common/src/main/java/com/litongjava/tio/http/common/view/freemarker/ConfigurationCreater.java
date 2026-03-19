package com.litongjava.tio.http.common.view.freemarker;

import java.io.IOException;

import com.litongjava.tio.http.common.HttpConfig;

import freemarker.template.Configuration;

/**
 * @author tanyaowu
 *
 */
public interface ConfigurationCreater {
  /**
   * 
   * @param httpConfig
   * @param root
   * @return
   * @throws IOException
   */
  public Configuration createConfiguration(HttpConfig httpConfig, String root) throws IOException;

}
