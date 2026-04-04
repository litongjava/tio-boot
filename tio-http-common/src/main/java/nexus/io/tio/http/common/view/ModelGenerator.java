package nexus.io.tio.http.common.view;

import nexus.io.tio.http.common.HttpRequest;

/**
 * 模板引擎model创建者
 * @author tanyaowu 
 * 2017年11月15日 下午1:12:39
 */
public interface ModelGenerator {

  /**
   * 
   * @param request
   * @return
   * @author tanyaowu
   * @throws Exception 
   */
  Object generate(HttpRequest request) throws Exception;

}
