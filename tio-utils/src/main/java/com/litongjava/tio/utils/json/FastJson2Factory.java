package com.litongjava.tio.utils.json;

/**
 * IJsonFactory 的 fastjson 实现.
 */
public class FastJson2Factory implements IJsonFactory {

  private static final FastJson2Factory me = new FastJson2Factory();

  public FastJson2Factory() {
    // 尽早触发 fastjson 的配置代码
    new FastJson2();
  }

  public static FastJson2Factory me() {
    return me;
  }

  public Json getJson() {
    return new FastJson2();
  }

  @Override
  public Json getSkipNullJson() {
    return null;
  }

}
