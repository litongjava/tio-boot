package com.litongjava.tio.boot.http.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.jfinal.template.Template;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.TioAsmUtils;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.server.util.ClassUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.server.ServerChannelContext;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestActionUtils {

  /**
   * 
   * @param request
   * @param response
   * @param obj
   * @return
   */
  public static HttpResponse afterExecuteAction(Object actionRetrunValue) {
    HttpRequest request = TioRequestContext.getRequest();
    HttpResponse response = TioRequestContext.getResponse();

    if (actionRetrunValue == null) {
      return response;
    }
    if (actionRetrunValue instanceof HttpResponse) {
      // action return http response
      response = (HttpResponse) actionRetrunValue;
    } else if (actionRetrunValue instanceof String) {
      // action return string
      response = Resps.txt(response, (String) actionRetrunValue);
    } else if (actionRetrunValue instanceof Integer) {
      response = Resps.txt(response, actionRetrunValue.toString());

    } else if (actionRetrunValue instanceof Long) {
      response = Resps.txt(response, actionRetrunValue.toString());

    } else if (actionRetrunValue instanceof byte[]) {
      response.setBody((byte[]) actionRetrunValue);

    } else if (actionRetrunValue instanceof Template) {
      // action return Template
      Map<Object, Object> data = new HashMap<Object, Object>();
      for (Enumeration<String> attrs = request.getAttributeNames(); attrs.hasMoreElements();) {
        String attrName = attrs.nextElement();
        data.put(attrName, request.getAttribute(attrName));
      }

      String renderToString = ((Template) actionRetrunValue).renderToString(data);

      response = Resps.html(response, renderToString);
    } else {
      // action return 其他值
      response = Resps.json(response, actionRetrunValue);
    }

    return response;
  }
  
  public static Object[] buildFunctionParamValues(HttpRequest request, HttpConfig httpConfig, boolean compatibilityAssignment,
      //
      String[] paramNames, Class<?>[] parameterTypes) {
    // 赋值这段代码待重构，先用上
    Object[] paramValues = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> paramType = parameterTypes[i];
      if (paramType == HttpRequest.class) {
        paramValues[i] = request;
        continue;
      } else if (paramType == HttpResponse.class) {
        paramValues[i] = TioRequestContext.getResponse();
        continue;
      } else {
        if (compatibilityAssignment) {
          if (paramType == HttpSession.class) {
            paramValues[i] = request.getHttpSession();
            continue;
          } else if (paramType == HttpConfig.class) {
            paramValues[i] = httpConfig;
            continue;
          } else if (paramType == ServerChannelContext.class) { // paramType.isAssignableFrom(ServerChannelContext.class)
            paramValues[i] = request.channelContext;
            continue;
          }
        }

        Map<String, Object[]> params = request.getParams();
        if (params != null && params.size() > 0) {
          try {
            TioAsmUtils.injectParametersIntoObject(params, i, paramNames[i], paramType, paramValues);
          } catch (Exception e) {
            log.error("error while inject request parameters:{},{}", paramType, paramValues[i]);
          }
        } else {
          String bodyString = request.getBodyString();
          if (StrUtil.isNotBlank(bodyString)) {
            paramValues[i] = parseJson(bodyString, paramType);
          }
        }
      }
    }
    return paramValues;
  }
  
  /**
   * JSON 请求体并转换为所需类型
   * 
   * @param bodyString
   * @param paramType
   * @return
   */
  private static Object parseJson(String bodyString, Class<?> paramType) {
    if (!ClassUtils.isSimpleTypeOrArray(paramType)) {
      try {
        // 解析 JSON 为指定类型的对象
        return JsonUtils.parse(bodyString, paramType);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    } else {
      log.error("{}:Attempting to deserialize JSON into a simple type or array, which is not supported directly.", paramType);
      return null;
    }
  }
}
