package com.litongjava.tio.utils.json;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.map.SyncWriteMap;

/**
 * Tio JsonKit
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TioJsonKit {

  public static final TioJsonKit me = new TioJsonKit();

  // 缓存 ToJson 对象
  protected static SyncWriteMap<Class<?>, TioToJson<?>> cache = new SyncWriteMap<>(512, 0.25F);

  protected static SyncWriteMap<Class<?>, TioToJson<?>> cacheSkipNull = new SyncWriteMap<>(512, 0.25F);

  // StringBuilder 最大缓冲区大小
  protected static int maxBufferSize = 1024 * 512;

  // 将 Model 当成 Bean 只对 getter 方法进行转换
  protected static boolean treatModelAsBean = false;


  // 对 Model 和 Record 的字段名进行转换的函数。例如转成驼峰形式对 oracle 支持更友好
  protected static Function<String, String> modelAndRecordFieldNameConverter = null;

  protected static Function<Object, TioToJson<?>> toJsonFactory = null;

  protected boolean skipNullValueField;

  public TioToJson getToJson(Object object, boolean skipNullValueField) {
    this.skipNullValueField = skipNullValueField;
    TioToJson<?> ret = null;
    if (skipNullValueField) {
      ret = cacheSkipNull.get(object.getClass());
      if (ret == null) {
        ret = createToJson(object, skipNullValueField);
        cacheSkipNull.putIfAbsent(object.getClass(), ret);
      }

    } else {
      ret = cache.get(object.getClass());
      if (ret == null) {
        ret = createToJson(object, false);
        cache.putIfAbsent(object.getClass(), ret);
      }
    }

    return ret;
  }

  /**
   * 添加 ToJson 转换接口实现类，自由定制任意类型数据的转换规则
   * <pre>
   * 例子：
   *     ToJson<Timestamp> toJson = (value, depth, ret) -> {
   *       ret.addLong(value.getTime());
   *     };
   *     
   *     TioJson.addToJson(Timestamp.class, toJson);
   *     
   *     以上代码为 Timestamp 类型的 json 转换定制了转换规则
   *     将其转换成了 long 型数据
   * </pre>
   */
  public static void addToJson(Class<?> type, TioToJson<?> toJson) {
    Objects.requireNonNull(type, "type can not be null");
    Objects.requireNonNull(toJson, "toJson can not be null");
    cache.put(type, toJson);
  }

  protected TioToJson<?> createToJson(Object value, boolean skipNullValueField) {
    // 优先使用 toJsonFactory 创建 ToJson 实例，方便用户优先接管 ToJson 转换器的创建
    if (toJsonFactory != null) {
      TioToJson<?> tj = toJsonFactory.apply(value);
      if (tj != null) {
        return tj;
      }
    }

    // 基础类型 -----------------------------------------
    if (value instanceof String) {
      return new StrToJson();
    }

    if (value instanceof Number) {
      if (value instanceof Integer) {
        return new IntToJson();
      }
      if (value instanceof Long) {
        return new LongToJson();
      }
      if (value instanceof Double) {
        return new DoubleToJson();
      }
      if (value instanceof Float) {
        return new FloatToJson();
      }
      return new NumberToJson();
    }

    if (value instanceof Boolean) {
      return new BooleanToJson();
    }

    if (value instanceof Character) {
      return new CharacterToJson();
    }

    if (value instanceof Enum) {
      return new EnumToJson();
    }

    if (value instanceof java.util.Date) {
      if (value instanceof Timestamp) {
        return new TimestampToJson();
      }
      if (value instanceof Time) {
        return new TimeToJson();
      }
      return new DateToJson();
    }

    if (value instanceof Temporal) {
      if (value instanceof LocalDateTime) {
        return new LocalDateTimeToJson();
      }
      if (value instanceof LocalDate) {
        return new LocalDateToJson();
      }
      if (value instanceof LocalTime) {
        return new LocalTimeToJson();
      }
    }

    if (value instanceof Map) {
      return new MapToJson(skipNullValueField);
    }

    if (value instanceof Collection) {
      return new CollectionToJson(skipNullValueField);
    }

    if (value.getClass().isArray()) {
      return new ArrayToJson(skipNullValueField);
    }

    if (value instanceof Enumeration) {
      return new EnumerationToJson(skipNullValueField);
    }

    if (value instanceof Iterator) {
      return new IteratorToJson(skipNullValueField);
    }

    if (value instanceof Iterable) {
      return new IterableToJson(skipNullValueField);
    }

    if (value instanceof UUID) {
      return new UUIDToJson();
    }

    BeanToJson beanToJson = buildBeanToJson(value, skipNullValueField);
    if (beanToJson != null) {
      return beanToJson;
    }

    return new UnknownToJson();
  }

  public static boolean checkDepth(int depth, JsonResult ret) {
    if (depth < 0) {
      ret.addNull();
      return true;
    } else {
      return false;
    }
  }

  static class StrToJson implements TioToJson<String> {
    public void toJson(String str, int depth, JsonResult ret) {
      escape(str, ret.sb);
    }
  }

  static class CharacterToJson implements TioToJson<Character> {
    public void toJson(Character ch, int depth, JsonResult ret) {
      escape(ch.toString(), ret.sb);
    }
  }

  static class IntToJson implements TioToJson<Integer> {
    public void toJson(Integer value, int depth, JsonResult ret) {
      ret.addInt(value);
    }

  }

  static class LongToJson implements TioToJson<Long> {
    public void toJson(Long value, int depth, JsonResult ret) {
      ret.addLong(value);
    }
  }

  static class DoubleToJson implements TioToJson<Double> {
    public void toJson(Double value, int depth, JsonResult ret) {
      if (value.isInfinite() || value.isNaN()) {
        ret.addNull();
      } else {
        ret.addDouble(value);
      }
    }
  }

  static class FloatToJson implements TioToJson<Float> {
    public void toJson(Float value, int depth, JsonResult ret) {
      if (value.isInfinite() || value.isNaN()) {
        ret.addNull();
      } else {
        ret.addFloat(value);
      }
    }
  }

  // 接管 int、long、double、float 之外的 Number 类型
  static class NumberToJson implements TioToJson<Number> {
    public void toJson(Number value, int depth, JsonResult ret) {
      ret.addNumber(value);
    }
  }

  static class BooleanToJson implements TioToJson<Boolean> {
    public void toJson(Boolean value, int depth, JsonResult ret) {
      ret.addBoolean(value);
    }
  }

  static class EnumToJson implements TioToJson<Enum> {
    public void toJson(Enum en, int depth, JsonResult ret) {
      ret.addEnum(en);
    }
  }

  static class TimestampToJson implements TioToJson<Timestamp> {
    public void toJson(Timestamp ts, int depth, JsonResult ret) {
      ret.addTimestamp(ts);
    }
  }

  static class TimeToJson implements TioToJson<Time> {
    public void toJson(Time t, int depth, JsonResult ret) {
      ret.addTime(t);
    }
  }

  static class DateToJson implements TioToJson<Date> {
    public void toJson(Date value, int depth, JsonResult ret) {
      ret.addDate(value);
    }
  }

  static class LocalDateTimeToJson implements TioToJson<LocalDateTime> {
    public void toJson(LocalDateTime value, int depth, JsonResult ret) {
      ret.addLocalDateTime(value);
    }
  }

  static class LocalDateToJson implements TioToJson<LocalDate> {
    public void toJson(LocalDate value, int depth, JsonResult ret) {
      ret.addLocalDate(value);
    }
  }

  static class LocalTimeToJson implements TioToJson<LocalTime> {
    public void toJson(LocalTime value, int depth, JsonResult ret) {
      ret.addLocalTime(value);
    }
  }

  static class UUIDToJson implements TioToJson<UUID> {
    public void toJson(UUID value, int depth, JsonResult ret) {
      escape(value.toString(), ret.sb);
    }
  }

  public void modelAndRecordToJson(Map<String, Object> map, int depth, JsonResult ret) {
    Iterator iter = map.entrySet().iterator();
    boolean first = true;
    ret.addChar('{');
    while (iter.hasNext()) {
      Map.Entry<String, Object> entry = (Map.Entry) iter.next();
      Object value = entry.getValue();

      if (value == null && skipNullValueField) {
        continue;
      }

      if (first) {
        first = false;
      } else {
        ret.addChar(',');
      }

      String fieldName = entry.getKey();
      if (modelAndRecordFieldNameConverter != null) {
        fieldName = modelAndRecordFieldNameConverter.apply(fieldName);
      }
      ret.addStrNoEscape(fieldName);

      ret.addChar(':');

      if (value != null) {
        TioToJson tj = me.getToJson(value, skipNullValueField);
        tj.toJson(value, depth, ret);
      } else {
        ret.addNull();
      }
    }
    ret.addChar('}');
  }

  static class MapToJson implements TioToJson<Map<?, ?>> {
    boolean skipNullValueField;

    public MapToJson() {

    }

    public MapToJson(boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Map<?, ?> map, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      mapToJson(map, depth, ret, skipNullValueField);
    }
  }

  public static void mapToJson(Map<?, ?> map, int depth, JsonResult ret, boolean skipNullValueField) {
    Iterator iter = map.entrySet().iterator();
    boolean first = true;
    ret.addChar('{');
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      Object value = entry.getValue();

      if (value == null && skipNullValueField) {
        continue;
      }

      if (first) {
        first = false;
      } else {
        ret.addChar(',');
      }

      ret.addMapKey(entry.getKey());

      ret.addChar(':');

      if (value != null) {
        TioToJson tj = me.getToJson(value, skipNullValueField);
        tj.toJson(value, depth, ret);
      } else {
        ret.addNull();
      }
    }
    ret.addChar('}');
  }

  static class CollectionToJson implements TioToJson<Collection> {
    boolean skipNullValueField;

    public CollectionToJson(Boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Collection c, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      iteratorToJson(c.iterator(), depth, ret, skipNullValueField);
    }
  }

  static class ArrayToJson implements TioToJson<Object> {
    boolean skipNullValueField;

    public ArrayToJson(Boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Object object, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      iteratorToJson(new ArrayIterator(object), depth, ret, skipNullValueField);
    }
  }

  static class ArrayIterator implements Iterator<Object> {
    private Object array;
    private int size;
    private int index;

    public ArrayIterator(Object array) {
      this.array = array;
      this.size = Array.getLength(array);
      this.index = 0;
    }

    public boolean hasNext() {
      return index < size;
    }

    public Object next() {
      return Array.get(array, index++);
    }
  }

  static class EnumerationToJson implements TioToJson<Enumeration> {

    boolean skipNullValueField;

    public EnumerationToJson(Boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Enumeration en, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      ArrayList list = Collections.list(en);
      iteratorToJson(list.iterator(), depth, ret, skipNullValueField);
    }
  }

  static class IteratorToJson implements TioToJson<Iterator> {

    boolean skipNullValueField;

    public IteratorToJson(Boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Iterator it, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      iteratorToJson(it, depth, ret, skipNullValueField);
    }
  }

  public static void iteratorToJson(Iterator it, int depth, JsonResult ret, boolean skipNullValueField) {
    boolean first = true;
    ret.addChar('[');
    while (it.hasNext()) {
      if (first) {
        first = false;
      } else {
        ret.addChar(',');
      }

      Object value = it.next();
      if (value != null) {
        TioToJson tj = me.getToJson(value, skipNullValueField);
        tj.toJson(value, depth, ret);
      } else {
        ret.addNull();
      }
    }
    ret.addChar(']');
  }

  static class IterableToJson implements TioToJson<Iterable> {
    boolean skipNullValueField;

    public IterableToJson(Boolean skipNullValueField) {
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Iterable iterable, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      iteratorToJson(iterable.iterator(), depth, ret, skipNullValueField);
    }
  }

  static class BeanToJson implements TioToJson<Object> {
    private static final Object[] NULL_ARGS = new Object[0];
    private String[] fields;
    private Method[] methods;
    private boolean skipNullValueField;

    public BeanToJson(String[] fields, Method[] methods, boolean skipNullValueField) {
      if (fields.length != methods.length) {
        throw new IllegalArgumentException("fields 与 methods 长度必须相同");
      }

      this.fields = fields;
      this.methods = methods;
      this.skipNullValueField = skipNullValueField;
    }

    public void toJson(Object bean, int depth, JsonResult ret) {
      if (checkDepth(depth--, ret)) {
        return;
      }

      try {
        ret.addChar('{');
        boolean first = true;
        for (int i = 0; i < fields.length; i++) {
          Object value = methods[i].invoke(bean, NULL_ARGS);

          if (value == null && skipNullValueField) {
            continue;
          }

          if (first) {
            first = false;
          } else {
            ret.addChar(',');
          }

          ret.addStrNoEscape(fields[i]);

          ret.addChar(':');

          if (value != null) {
            TioToJson tj = me.getToJson(value, skipNullValueField);
            tj.toJson(value, depth, ret);
          } else {
            ret.addNull();
          }
        }
        ret.addChar('}');
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * 存在 getter/is 方法返回 BeanToJson，否则返回 null
   */
  public BeanToJson buildBeanToJson(Object bean,boolean skipNullValueField) {
    List<String> fields = new ArrayList<>();
    List<Method> methods = new ArrayList<>();

    Method[] methodArray = bean.getClass().getMethods();
    for (Method m : methodArray) {
      if (m.getParameterCount() != 0 || m.getReturnType() == void.class) {
        continue;
      }

      String methodName = m.getName();
      int indexOfGet = methodName.indexOf("get");
      if (indexOfGet == 0 && methodName.length() > 3) { // Only getter
        String attrName = methodName.substring(3);
        if (!attrName.equals("Class")) { // Ignore Object.getClass()
          fields.add(StrUtil.firstCharToLowerCase(attrName));
          methods.add(m);
        }
      } else {
        int indexOfIs = methodName.indexOf("is");
        if (indexOfIs == 0 && methodName.length() > 2) {
          String attrName = methodName.substring(2);
          fields.add(StrUtil.firstCharToLowerCase(attrName));
          methods.add(m);
        }
      }
    }

    int size = fields.size();
    if (size > 0) {
      return new BeanToJson(fields.toArray(new String[size]), methods.toArray(new Method[size]), skipNullValueField);
    } else {
      return null;
    }
  }

  static class UnknownToJson implements TioToJson<Object> {
    public void toJson(Object object, int depth, JsonResult ret) {
      // 未知类型无法处理时当作字符串处理，否则 ajax 调用返回时 js 无法解析
      ret.addUnknown(object);
    }
  }

  /**
   * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
   */
  public static void escape(String s, StringBuilder sb) {
    sb.append('\"');

    for (int i = 0, len = s.length(); i < len; i++) {
      char ch = s.charAt(i);
      switch (ch) {
      case '"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      case '\b':
        sb.append("\\b");
        break;
      case '\f':
        sb.append("\\f");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case '\r':
        sb.append("\\r");
        break;
      case '\t':
        sb.append("\\t");
        break;
      // case '/':
      // sb.append("\\/");
      // break;
      default:
        if ((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
          String str = Integer.toHexString(ch);
          sb.append("\\u");
          for (int k = 0; k < 4 - str.length(); k++) {
            sb.append('0');
          }
          sb.append(str.toUpperCase());
        } else {
          sb.append(ch);
        }
      }
    }

    sb.append('\"');
  }

  public static void setMaxBufferSize(int maxBufferSize) {
    int size = 1024 * 1;
    if (maxBufferSize < size) {
      throw new IllegalArgumentException("maxBufferSize can not less than " + size);
    }
    TioJsonKit.maxBufferSize = maxBufferSize;
  }

  /**
   * 将 Model 当成 Bean 只对 getter 方法进行转换
   * 
   * 默认值为 false，将使用 Model 内的 Map attrs 属性进行转换，不对 getter 方法进行转换
   * 优点是可以转换 sql 关联查询产生的动态字段，还可以转换 Model.put(...) 进来的数据
   * 
   * 配置为 true 时，将 Model 当成是传统的 java bean 对其 getter 方法进行转换，
   * 使用生成器生成过 base model 的情况下才可以使用此配置
   */
  public static void setTreatModelAsBean(boolean treatModelAsBean) {
    TioJsonKit.treatModelAsBean = treatModelAsBean;
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
    TioJsonKit.modelAndRecordFieldNameConverter = converter;
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
    modelAndRecordFieldNameConverter = (fieldName) -> {
      return StrUtil.toCamelCase(fieldName, toLowerCaseAnyway);
    };
  }

  /**
   * 配置将 Model、Record 字段名转换为驼峰格式
   * 
   * 先将字段名无条件转换成小写字母，然后再转成驼峰格式，适用于 oracle 这类字段名是大写字母的数据库
   * 
   * 如果是 mysql 数据库，建议使用: setModelAndRecordFieldNameToCamelCase(false);
   */
  public static void setModelAndRecordFieldNameToCamelCase() {
    setModelAndRecordFieldNameToCamelCase(true);
  }

  public static void setToJsonFactory(Function<Object, TioToJson<?>> toJsonFactory) {
    TioJsonKit.toJsonFactory = toJsonFactory;
  }

}
