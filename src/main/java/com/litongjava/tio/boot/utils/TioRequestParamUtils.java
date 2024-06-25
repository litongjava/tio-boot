package com.litongjava.tio.boot.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jfinal.kit.StrKit;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.utils.json.Json;

public class TioRequestParamUtils {
  public static final String to_type = "to_type";
  public static final String toType = "toType";
  public static final String _to_type = "_to_type";
  public static final String _toType = "_toType";

  public static final String input_type = "input_type";
  public static final String inputType = "inutType";
  public static final String _input_type = "_input_type";
  public static final String _inputType = "_inputType";

  public static List<String> types = new ArrayList<>();

  static {
    types.add("int");
    types.add("long");
    types.add("bool");
    types.add("int[]");
    types.add("long[]");
    types.add("string[]");
    types.add("ISO8601");
  }

  public static Map<String, Object> getRequestMap(HttpRequest request) {
    Map<String, Object> map = new HashMap<>();
    // String contentType = request.getHeader(HttpConst.RequestHeaderKey.Content_Type);
    String contentType = request.getContentType();

    Map<String, Object> requestMap = new HashMap<>();
    Map<String, List<Object>> arrayParams = new HashMap<>();
    Map<String, String> paramType = new HashMap<>();
    Map<String, String> inputTypeMap = new HashMap<>();

    Map<String, String> toTypeMap = new HashMap<>();

    if (contentType != null && contentType.contains("application/json")) {
      String bodyString = request.getBodyString();
      requestMap = Json.getJson().parseToMap(bodyString, String.class, Object.class);
    } else {
      // Form data handling
      Enumeration<String> parameterNames = request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        String paramName = parameterNames.nextElement();
        String paramValue = request.getParameter(paramName);
        requestMap.put(paramName, paramValue);
      }
    }

    Set<Entry<String, Object>> entrySet = requestMap.entrySet();
    for (Entry<String, Object> entry : entrySet) {
      String paramName = entry.getKey();
      Object paramValue = entry.getValue();

      if (paramName.contains("[")) {
        // This is an array paramValue
        String arrayName = paramName.substring(0, paramName.indexOf('['));
        if (!arrayParams.containsKey(arrayName)) {
          arrayParams.put(arrayName, new ArrayList<>());
        }
        arrayParams.get(arrayName).add(paramValue);
      } else if (paramName.endsWith(_inputType) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(_inputType);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          inputTypeMap.put(paramKey, (String) paramValue);
        }
      } else if (paramName.endsWith(_input_type) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(_input_type);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          inputTypeMap.put(paramKey, (String) paramValue);
        }
      } else if (paramName.endsWith(input_type) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(input_type);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          inputTypeMap.put(paramKey, (String) paramValue);
        }
      } else if (paramName.endsWith(inputType) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(inputType);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          inputTypeMap.put(paramKey, (String) paramValue);
        }
      } else if (paramName.endsWith(_to_type) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(_to_type);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          toTypeMap.put(paramKey, (String) paramValue);
        }

      } else if (paramName.endsWith(_toType) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(_toType);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          toTypeMap.put(paramKey, (String) paramValue);
        }

      } else if (paramName.endsWith(toType) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(toType);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          toTypeMap.put(paramKey, (String) paramValue);
        }
      } else if (paramName.endsWith(to_type) && types.contains(paramValue)) {
        int lastIndexOf = paramName.lastIndexOf(to_type);
        if (lastIndexOf != -1) {
          String paramKey = paramName.substring(0, lastIndexOf);
          toTypeMap.put(paramKey, (String) paramValue);
        }

      } else if (paramName.endsWith("Type") || paramName.endsWith("type")) {
        if (types.contains(paramValue)) {
          // 前端传递指定数缺定数据类型
          paramType.put(paramName, (String) paramValue);
        }
      } else {
        // This is a regular paramValue
        map.put(paramName, paramValue);
      }
    }

    // Convert the lists to arrays and add them to the map
    convertValueType(map, arrayParams, paramType, inputTypeMap, toTypeMap);
    return map;
  }

  @SuppressWarnings("unchecked")
  public static void convertValueType(Map<String, Object> map, Map<String, List<Object>> arrayParams,
      Map<String, String> paramType, Map<String, String> inputTypeMap, Map<String, String> toTypeMap) {
    // convert type
    for (Map.Entry<String, List<Object>> entry : arrayParams.entrySet()) {
      map.put(entry.getKey(), entry.getValue().toArray(new String[0]));
    }
    // convert type
    for (Map.Entry<String, String> entry : paramType.entrySet()) {
      // idType=long
      String typeKey = entry.getKey();
      // 支持id_type and idType
      int lastIndexOf = typeKey.lastIndexOf("Type");
      String paramKey = null;
      if (lastIndexOf != -1) {
        paramKey = typeKey.substring(0, lastIndexOf);
      } else {
        lastIndexOf = typeKey.lastIndexOf("_");
        paramKey = typeKey.substring(0, lastIndexOf);
      }
      Object paramValue = map.get(paramKey);
      if (StrKit.notNull(paramValue)) {
        Object paramTypeValue = entry.getValue();

        if (paramValue instanceof String) {
          String stringValue = (String) paramValue;
          if (StrKit.notBlank(stringValue)) {
            if ("int".equals(paramTypeValue)) {
              map.put(paramKey, Integer.parseInt(stringValue));

            } else if ("long".equals(paramTypeValue)) {
              map.put(paramKey, Long.parseLong(stringValue));

            } else if ("bool".equals(paramTypeValue)) {
              map.put(paramKey, Boolean.parseBoolean(stringValue));

            } else if ("ISO8601".equals(paramTypeValue)) {
              map.put(paramKey, DateParseUtils.parseIso8601Date(stringValue));
            }
          }
        } else if (paramValue instanceof List) {
          @SuppressWarnings("rawtypes")
          List list = (List) paramValue;
          int size = list.size();
          if ("string[]".equals(paramTypeValue)) {
            String inputType = inputTypeMap.get(paramKey);
            if (StrKit.notNull(inputType)) {
              if ("ISO8601".equals(inputType)) {
                list = DateParseUtils.convertToIso8601Date(list);
              }
            }

            String toType = toTypeMap.get(paramKey);

            if (StrKit.notNull(toType)) {
              if ("ISO8601".equals(toType)) {
                list = DateParseUtils.convertToIso8601FromDefault(list);
              }
            }

            map.put(paramKey, list);

          } else if ("int[]".equals(paramTypeValue)) {
            Integer[] values = new Integer[size];
            for (int i = 0; i < size; i++) {
              values[i] = Integer.parseInt((String) list.get(i));
            }
            map.put(paramKey, values);
          } else if ("long[]".equals(paramTypeValue)) {
            // List<Long> collect = array.stream().map((item) -> Long.parseLong((String) item)).collect(Collectors.toList());
            Long[] values = new Long[size];
            for (int i = 0; i < size; i++) {
              values[i] = Long.parseLong((String) list.get(i));
            }
            map.put(paramKey, values);
          }
        }
      }
    }
  }
}
