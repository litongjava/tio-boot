package com.litongjava.tio.utils.http;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.tio.utils.hutool.StrUtil;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author tanyaowu
 */
public class HttpUtils {

  public static final MediaType MEDIATYPE_JSON_UTF8 = MediaType.parse("application/json; charset=utf-8");

  /**
   * 
   * @param url
   * @param headerMap
   * @return
   * @throws Exception
   */
  public static Response get(String url, Map<String, String> headerMap) throws Exception {
    Builder builder = new Request.Builder().url(url);
    if (headerMap != null) {
      Headers headers = Headers.of(headerMap);
      builder.headers(headers);
    }
    builder.get();

    Request request = builder.build();
    OkHttpClient client = OkHttpClientPool.getHttpClient();
    Response response = client.newCall(request).execute();
    return response;
  }

  /**
   * 
   * @param url
   * @return
   * @throws Exception
   */
  public static ResponseVo get(String url) {
    Request request = new Request.Builder().url(url).get().build();
    return call(request);
  }

  public static ResponseVo upload(String url, File file) {
    // Create the request body with file and image media type
    String contentType = ContentTypeUtils.getContentType(file.getName());
    RequestBody fileBody = RequestBody.create(file, MediaType.parse(contentType));

    // Create MultipartBody
    okhttp3.MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
    builder.addFormDataPart("file", file.getName(), fileBody);

    RequestBody requestBody = builder.build();

    // Create request
    Request request = new Request.Builder().url(url).post(requestBody).build();

    ResponseVo responseVo = HttpUtils.call(request);
    return responseVo;
  }

  /**
   * 
   * @param url
   * @param headerMap
   * @param mediaType
   * @param bodyString
   * @param paramMap
   * @param paramNames
   * @param paramValues
   * @return
   * @throws Exception
   */
  private static Response post(String url, Map<String, String> headerMap, MediaType mediaType, String bodyString,
      Map<String, String> paramMap, List<String> paramNames, List<String> paramValues) {
    Request.Builder builder = new Request.Builder().url(url);
    if (headerMap != null) {
      Headers headers = Headers.of(headerMap);
      builder.headers(headers);
    }

    if (false == StrUtil.isBlank(bodyString)) { // 提交bodyString
      if (mediaType == null) {
        mediaType = MEDIATYPE_JSON_UTF8;
      }
      @SuppressWarnings("deprecation")
      RequestBody body = RequestBody.create(mediaType, bodyString);
      builder.post(body);
    } else { // 提交form表单
      FormBody.Builder formBodyBuilder = new FormBody.Builder();
      if (paramMap != null && paramMap.size() > 0) {
        Set<Entry<String, String>> set = paramMap.entrySet();
        for (Entry<String, String> entry : set) {
          formBodyBuilder.add(entry.getKey(), entry.getValue());
        }
      } else if (paramNames != null) {
        int xx = paramNames.size();
        if (xx > 0) {
          for (int i = 0; i < xx; i++) {
            formBodyBuilder.add(paramNames.get(i), paramValues.get(i));
          }
        }
      }
      RequestBody formBody = formBodyBuilder.build();
      builder.post(formBody);
    }
    Request request = builder.build();
    Response response = null;
    OkHttpClient client = OkHttpClientPool.getHttpClient();
    try {
      response = client.newCall(request).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return response;
  }

  /**
   * 
   * @param url
   * @param headerMap
   * @param paramNames
   * @param paramValues
   * @return
   * @throws Exception
   */
  public static Response post(String url, Map<String, String> headerMap, List<String> paramNames,
      List<String> paramValues) throws Exception {
    return post(url, headerMap, (MediaType) null, null, null, paramNames, paramValues);
  }

  /**
   * 
   * @param url
   * @param headerMap
   * @param paramMap
   * @return
   * @throws Exception
   */
  public static Response post(String url, Map<String, String> headerMap, Map<String, String> paramMap)
      throws Exception {
    return post(url, headerMap, (MediaType) null, null, paramMap, null, null);
  }

  /**
   * 
   * @param url
   * @param headerMap
   * @param bodyString
   * @return
   * @throws Exception
   */
  public static Response post(String url, Map<String, String> headerMap, String bodyString) {
    return post(url, headerMap, (MediaType) null, bodyString, null, null, null);
  }

  /**
   * 
   * @param url
   * @param headerMap
   * @return
   * @throws Exception
   */
  public static Response post(String url, Map<String, String> headerMap) throws Exception {
    return post(url, headerMap, (MediaType) null, null, null, null, null);
  }

  /**
   * 
   * @param url
   * @return
   * @throws Exception
   */
  public static Response post(String url) throws Exception {
    return post(url, null);
  }

  public static ResponseVo call(Request request) {
    OkHttpClient httpClient = OkHttpClientPool.getHttpClient();
    return call(httpClient, request);
  }

  public static ResponseVo call(OkHttpClient httpClient, Request request) {
    Call call = httpClient.newCall(request);
    return call(call);
  }

  public static ResponseVo call(Call call) {
    try (Response response = call.execute()) {
      Headers headers = response.headers();
      int code = response.code();
      String body = response.body().string();

      if (response.isSuccessful()) {
        return ResponseVo.ok(code, headers, body);
      } else {
        return ResponseVo.fail(code, headers, body);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static ResponseVo get(String url, String key) {
    OkHttpClient client = OkHttpClientPool.get60HttpClient();
    Request request = new Request.Builder()
        //
        .get().url(url)
        //
        .addHeader("Authorization", "Bearer " + key).build();
    try (Response response = client.newCall(request).execute()) {
      int code = response.code();
      String string = response.body().string();
      return new ResponseVo(true, code, string);
    } catch (IOException e) {
      throw new RuntimeException("Failed to request:" + url, e);
    }
  }

  public static ResponseVo postText(String url, String payload) {
    OkHttpClient client = OkHttpClientPool.get60HttpClient();
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(payload, mediaType);

    Request.Builder builder = new Request.Builder();
    builder.url(url).post(body);

    Request request = builder.build();
    try (Response response = client.newCall(request).execute()) {
      String string = response.body().string();
      int code = response.code();
      if (response.isSuccessful()) {
        return new ResponseVo(true, code, string);
      } else {
        return new ResponseVo(false, code, string);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to request:" + url, e);
    }
  }

  public static ResponseVo postJson(String url, String payload) {
    OkHttpClient client = OkHttpClientPool.get60HttpClient();
    MediaType mediaType = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(payload, mediaType);
    Request request = new Request.Builder().url(url).post(body).build();

    try (Response response = client.newCall(request).execute()) {
      String string = response.body().string();
      int code = response.code();
      if (response.isSuccessful()) {
        return new ResponseVo(true, code, string);
      } else {
        return new ResponseVo(false, code, string);
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to request:" + url, e);
    }

  }

  public static ResponseVo postJson(String url, String key, String payload) {
    OkHttpClient client = OkHttpClientPool.get60HttpClient();
    MediaType mediaType = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(payload, mediaType);
    Request.Builder builder = new Request.Builder();
    builder.url(url).post(body);
    //
    if (key != null) {
      builder.addHeader("Authorization", "Bearer " + key);
    }
    Request request = builder.build();
    try (Response response = client.newCall(request).execute()) {
      String string = response.body().string();
      int code = response.code();
      if (response.isSuccessful()) {
        return new ResponseVo(true, code, string);
      } else {
        return new ResponseVo(false, code, string);
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to request:" + url, e);
    }
  }

  /**
   * downlaod.
   * 
   * @param url
   * @return
   */
  public static ResponseVo download(String url) {
    Request request = new Request.Builder().url(url).get().build();

    try (Response response = OkHttpClientPool.getHttpClient().newCall(request).execute()) {
      Headers headers = response.headers();

      int code = response.code();
      if (response.isSuccessful()) {
        byte[] bytes = response.body().bytes();
        ResponseVo responseVo = ResponseVo.ok(headers, bytes);
        responseVo.setCode(code);
        return responseVo;
      } else {
        // not 2xx
        String bodyString = response.body() != null ? response.body().string() : "";
        ResponseVo responseVo = ResponseVo.fail(headers, bodyString);
        responseVo.setCode(code);
        return responseVo;
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to download content from " + url, e);
    }
  }

}
