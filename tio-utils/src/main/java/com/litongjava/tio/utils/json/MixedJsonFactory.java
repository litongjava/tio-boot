package com.litongjava.tio.utils.json;

/**
 * IJsonFactory 的 jfinal + fastjson 组合实现
 * 
 * 1：toJson 用 JFinalJson，parse 用 FastJson
 * 2：需要添加 fastjson 相关 jar 包
 * 3：parse 方法转对象依赖于 setter 方法
 */
public class MixedJsonFactory implements IJsonFactory {

  private static final MixedJsonFactory me = new MixedJsonFactory();

  public MixedJsonFactory() {
    // 尽早触发 fastjson 的配置代码
    // new FastJson2();
  }

  public static MixedJsonFactory me() {
    return me;
  }

  public Json getJson() {
    return new MixedJson();
  }

  @Override
  public Json getSkipNullJson() {
    return new MixedJson(true);
  }
}
