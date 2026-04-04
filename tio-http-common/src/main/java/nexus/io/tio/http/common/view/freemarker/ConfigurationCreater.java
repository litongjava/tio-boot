package nexus.io.tio.http.common.view.freemarker;

import java.io.IOException;

import freemarker.template.Configuration;
import nexus.io.tio.http.common.HttpConfig;

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
