package com.litongjava.tio.utils.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.litongjava.model.type.TioTypeReference;

/**
 * Json 转换 JFinal 实现.
 * 
 * json 到 java 类型转换规则: string java.lang.String number java.lang.Number
 * true|false java.lang.Boolean null null array java.util.List object
 * java.util.Map
 */
public class TioJson extends Json {

  private String notSupportJsonToObjectMesage = "The default json implementation currently does not support json to object conversion. It is recommended to use MixedJsonFactory and support it by setting Json.setDefaultJsonFactory(new MixedJsonFactory()).";

  protected static final TioJsonKit kit = TioJsonKit.me;

  protected static final ThreadLocal<JsonResult> TL = ThreadLocal.withInitial(() -> new JsonResult());

  protected static int defaultConvertDepth = 16;

  protected int convertDepth = defaultConvertDepth;

  // 是否跳过 null 值的字段，不对其进行转换
  protected boolean skipNullValueField;

  public TioJson() {
  }

  public TioJson(boolean skipNullValueField) {
    this.skipNullValueField = skipNullValueField;
  }

  public static TioJson getJson() {
    return new TioJson();
  }

  public static TioJson getSkipNullTioJson() {
    return new TioJson(true);
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public byte[] toJsonBytes(Object object) {
    if (object == null) {
      return null;
    }

    JsonResult ret = TL.get();
    try {

      // 重入型转换场景，需要新建对象使用
      if (ret.isInUse()) {
        ret = new JsonResult();
      }

      // 优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
      String dp = datePattern != null ? datePattern : getDefaultDatePattern();
      ret.init(dp, getTimestampPattern(), Json.isLongToString());
      TioToJson toJson = kit.getToJson(object, skipNullValueField);
      toJson.toJson(object, convertDepth, ret);
      return ret.toBytes();
    } finally {
      ret.clear();
    }
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public String toJson(Object object) {
    if (object == null) {
      return "null";
    }

    JsonResult ret = TL.get();
    try {

      // 重入型转换场景，需要新建对象使用
      if (ret.isInUse()) {
        ret = new JsonResult();
      }

      // 优先使用对象级的属性 datePattern, 然后才是全局性的 defaultDatePattern
      String dp = datePattern != null ? datePattern : getDefaultDatePattern();
      ret.init(dp, getTimestampPattern(), Json.isLongToString());

      TioToJson toJson = kit.getToJson(object, skipNullValueField);

      toJson.toJson(object, convertDepth, ret);
      return ret.toString();
    } finally {
      ret.clear();
    }
  }

  /**
   * 添加 ToJson 转换接口实现类，自由定制任意类型数据的转换规则
   * 
   * <pre>
   * 例子：
   *     ToJson<Timestamp> toJson = (value, depth, ret) -> {
   *       ret.addLong(value.getTime());
   *     };
   *     
   *     JFinalJson.addToJson(Timestamp.class, toJson);
   *     
   *     以上代码为 Timestamp 类型的 json 转换定制了转换规则
   *     将其转换成了 long 型数据
   * </pre>
   */
  public static void addToJson(Class<?> type, TioToJson<?> toJson) {
    TioJsonKit.addToJson(type, toJson);
  }

  /**
   * 设置全局性默认转换深度
   */
  public static void setDefaultConvertDepth(int defaultConvertDepth) {
    if (defaultConvertDepth < 2) {
      throw new IllegalArgumentException("defaultConvertDepth depth can not less than 2.");
    }
    TioJson.defaultConvertDepth = defaultConvertDepth;
  }

  public TioJson setConvertDepth(int convertDepth) {
    if (convertDepth < 2) {
      throw new IllegalArgumentException("convert depth can not less than 2.");
    }
    this.convertDepth = convertDepth;
    return this;
  }

  public static void setMaxBufferSize(int maxBufferSize) {
    TioJsonKit.setMaxBufferSize(maxBufferSize);
  }

  /**
   * 将 Model 当成 Bean 只对 getter 方法进行转换
   * 
   * 默认值为 false，将使用 Model 内的 Map attrs 属性进行转换，不对 getter 方法进行转换 优点是可以转换 sql
   * 关联查询产生的动态字段，还可以转换 Model.put(...) 进来的数据
   * 
   * 配置为 true 时，将 Model 当成是传统的 java bean 对其 getter 方法进行转换， 使用生成器生成过 base model
   * 的情况下才可以使用此配置
   */
  public static void setTreatModelAsBean(boolean treatModelAsBean) {
    TioJsonKit.setTreatModelAsBean(treatModelAsBean);
  }

  /**
   * 配置 Model、Record 字段名的转换函数
   * 
   * <pre>
   * 例子：
   *    JFinalJson.setModelAndRecordFieldNameConverter(fieldName -> {
   *		   return StrKit.toCamelCase(fieldName, true);
   *	  });
   *  
   *  以上例子中的方法 StrKit.toCamelCase(...) 的第二个参数可以控制大小写转化的细节
   *  可以查看其方法上方注释中的说明了解详情
   * </pre>
   */
  public static void setModelAndRecordFieldNameConverter(Function<String, String> converter) {
    TioJsonKit.setModelAndRecordFieldNameConverter(converter);
  }

  /**
   * 配置将 Model、Record 字段名转换为驼峰格式
   * 
   * <pre>
   * toLowerCaseAnyway 参数的含义：
   * 1：true 值无条件将字段先转换成小写字母。适用于 oracle 这类字段名是大写字母的数据库
   * 2：false 值只在出现下划线时将字段转换成小写字母。适用于 mysql 这类字段名是小写字母的数据库
   * </pre>
   */
  public static void setModelAndRecordFieldNameToCamelCase(boolean toLowerCaseAnyway) {
    TioJsonKit.setModelAndRecordFieldNameToCamelCase(toLowerCaseAnyway);
  }

  /**
   * 配置将 Model、Record 字段名转换为驼峰格式
   * 
   * 先将字段名无条件转换成小写字母，然后再转成驼峰格式，适用于 oracle 这类字段名是大写字母的数据库
   * 
   * 如果是 mysql 数据库，建议使用: setModelAndRecordFieldNameToCamelCase(false);
   */
  public static void setModelAndRecordFieldNameToCamelCase() {
    TioJsonKit.setModelAndRecordFieldNameToCamelCase();
  }

  /**
   * 配置 ToJsonFactory，便于接管 ToJson 对象的创建
   * 
   * <pre>
   * 例子：
   *    JFinalJson.setToJsonFactory(value -> {
   *        if (value instanceof Model) {
   *            // 返回 MyModelToJson 接管对于 Model 类型的转换
   *            return new MyModelToJson();
   *        } else {
   *            // 返回 null 时将使用系统默认的转换类
   *            return null;
   *        }
   *    });
   * </pre>
   */
  public static void setToJsonFactory(Function<Object, TioToJson<?>> toJsonFactory) {
    TioJsonKit.setToJsonFactory(toJsonFactory);
  }

  /**
   * 是否跳过 null 值的字段，配置为 true 值将跳过，默认值为 false 本配置作用于 Model、Record、Map、java
   * bean(getter 方法对应的属性) 这四种类型
   */
  public void setSkipNullValueField(boolean skipNullValueField) {
    this.skipNullValueField = skipNullValueField;
  }


  public <T> T parse(String jsonString, Class<T> type) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public Map<?, ?> parseToMap(String bodyString) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <K, V> Map<K, V> parseToMap(String json, Class<K> kType, Class<V> vType) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public Object parseObject(String jsonString) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public Object parseArray(String jsonString) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <K, V> List<Map<K, V>> parseToListMap(String stringValue, Class<K> kType, Class<V> vType) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public Object parse(String stringValue) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <T> T parse(String body, Type type) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <T> T parse(byte[] body, Type type) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <T> List<T> parseArray(String str, Class<T> elementType) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

  @Override
  public <T> T parse(String body, TioTypeReference<T> tioTypeReference) {
    throw new RuntimeException(notSupportJsonToObjectMesage);
  }

}