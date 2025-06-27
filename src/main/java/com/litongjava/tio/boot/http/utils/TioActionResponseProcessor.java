package com.litongjava.tio.boot.http.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.template.Template;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.boot.encrypt.TioEncryptor;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.boot.utils.TioAsmUtils;
import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.session.HttpSession;
import com.litongjava.tio.http.common.utils.MimeTypeUtils;
import com.litongjava.tio.http.server.util.ClassUtils;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.server.ServerChannelContext;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.Json;
import com.litongjava.tio.utils.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TioActionResponseProcessor {

  /**
   * afterExecuteAction
   * @param request
   * @param response
   * @param obj
   * @return
   */
  public static HttpResponse afterExecuteAction(Object actionRetrunValue) {
    HttpRequest request = TioRequestContext.getRequest();
    HttpResponse response = TioRequestContext.getResponse();
    String charset = request.getCharset();

    if (actionRetrunValue == null) {
      return response;
    }
    if (actionRetrunValue instanceof HttpResponse) {
      // action return http response
      response = (HttpResponse) actionRetrunValue;

    } else if (actionRetrunValue instanceof String) {
      // action return string
      String string = (String) actionRetrunValue;
      processString(response, charset, string);

    } else if (actionRetrunValue instanceof Integer) {
      processString(response, charset, actionRetrunValue.toString());

    } else if (actionRetrunValue instanceof Long) {
      processString(response, charset, actionRetrunValue.toString());

    } else if (actionRetrunValue instanceof byte[]) {
      processBytes(actionRetrunValue, response);

    } else if (actionRetrunValue instanceof Template) {
      // action return Template
      Map<Object, Object> data = new HashMap<Object, Object>();
      for (Enumeration<String> attrs = request.getAttributeNames(); attrs.hasMoreElements();) {
        String attrName = attrs.nextElement();
        data.put(attrName, request.getAttribute(attrName));
      }
      String renderToString = ((Template) actionRetrunValue).renderToString(data);
      response = Resps.html(response, renderToString);

    } else if (actionRetrunValue instanceof ResponseVo) {
      ResponseVo responseVo = (ResponseVo) actionRetrunValue;
      int code = responseVo.getCode();
      response.setStatus(code);
      if (responseVo.getBody() != null) {
        processJson(response, responseVo.getBody(), charset);
      }

      if (responseVo.getBodyString() != null) {
        processString(response, responseVo.getBodyString(), charset);
      }

      if (responseVo.getBodyBytes() != null) {
        processBytes(responseVo.getBodyBytes(), response);
      }

    } else {
      processJson(response, actionRetrunValue, charset);
    }

    return response;
  }

  private static void processJson(HttpResponse response, Object actionRetrunValue, String charset) {
    byte[] bytes = Json.getJson().toJsonBytes(actionRetrunValue);
    TioEncryptor tioEncryptor = TioBootServer.me().getTioEncryptor();
    if (tioEncryptor != null) {
      bytes = tioEncryptor.encrypt(bytes);
    }
    response.setBody(bytes);
    String mimeTypeStr = MimeTypeUtils.getJson(charset);
    response.setContentType(mimeTypeStr);
  }

  private static void processBytes(Object actionRetrunValue, HttpResponse response) {
    byte[] bytes = (byte[]) actionRetrunValue;
    TioEncryptor tioEncryptor = TioBootServer.me().getTioEncryptor();
    if (tioEncryptor != null) {
      bytes = tioEncryptor.encrypt(bytes);
    }

    response.setBody(bytes);
  }

  private static void processString(HttpResponse response, String charset, String string) {
    try {
      byte[] bytes = string.getBytes(charset);
      TioEncryptor tioEncryptor = TioBootServer.me().getTioEncryptor();
      if (tioEncryptor != null) {
        bytes = tioEncryptor.encrypt(bytes);
      }
      response.setBody(bytes);
    } catch (UnsupportedEncodingException e) {
      log.error(e.toString(), e);
    }

    String mimeTypeStr = MimeTypeUtils.getText(charset);
    response.setContentType(mimeTypeStr);
  }

  public static Object[] buildFunctionParamValues(HttpRequest request, HttpConfig httpConfig, boolean compatibilityAssignment,
      //
      String[] paramNames, Class<?>[] parameterTypes, Type[] types) {
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
            paramValues[i] = parseJson(bodyString, paramType, types[0]);
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
  private static Object parseJson(String bodyString, Class<?> paramType, Type genericType) {
    if (!ClassUtils.isSimpleTypeOrArray(paramType)) {
      try {
        if (List.class.isAssignableFrom(paramType)) {
          if (genericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
              Class<?> genericClass = (Class<?>) actualTypeArguments[0];
              if (bodyString.startsWith("[") && bodyString.endsWith("]")) {
                return JsonUtils.parseArray(bodyString, genericClass);
              }
            }
          }
          return JsonUtils.parse(bodyString, paramType);
        } else {
          return JsonUtils.parse(bodyString, paramType);
        }
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
