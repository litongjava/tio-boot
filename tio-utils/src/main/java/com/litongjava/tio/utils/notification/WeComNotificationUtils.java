package com.litongjava.tio.utils.notification;

import java.util.HashMap;
import java.util.Map;

import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.http.HttpUtils;
import com.litongjava.tio.utils.json.JsonUtils;

import okhttp3.Response;

public class WeComNotificationUtils {

  public static Response send(Map<String, Object> reqMap) {
    String webHookUrl = EnvUtils.get("notification.webhook.url");
    return send(webHookUrl, reqMap);
  }

  /**
   * 调用群机器人
   *
   * @param reqMap 接口请求参数
   * @throws Exception 可能有IO异常
   */
  public static Response send(String webHookUrl, Map<String, Object> reqMap) {
    String reqBody = JsonUtils.toJson(reqMap);

    Map<String, String> header = new HashMap<>();
    header.put("cache-control", "no-cache");

    // 调用群机器人
    return HttpUtils.post(webHookUrl, header, reqBody);
  }

  /**
   * @param warningName 告警名称
   * @param level       告警级别
   * @param deviceId    设备信息
   * @param content     告警内容
   * @return
   */
  public static Response sendWarm(NotifactionWarmModel model) {
    String webHookUrl = EnvUtils.get("notification.webhook.url");
    return sendWarm(webHookUrl, model);
  }

  public static Response sendWarm(String webHookUrl, NotifactionWarmModel model) {
    Map<String, Object> reqMap = getReqMap(model.format());

    return send(webHookUrl, reqMap);
  }

  /**
   * 发送MarKDown消息
   * 
   * @param msg 需要发送的消息
   * @return
   * @throws Exception
   */
  public static Map<String, Object> getReqMap(String msg) {
    Map<String, Object> markdown = new HashMap<>();
    markdown.put("content", msg);

    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put("msgtype", "markdown");
    reqMap.put("markdown", markdown);
    reqMap.put("safe", 0);
    return reqMap;

  }

  /**
   * 发送文字消息
   *
   * @param msg 需要发送的消息
   * @return
   * @throws Exception
   */
  public static Response sendTextMsg(String msg) {
    Map<String, Object> text = new HashMap<>();
    text.put("content", msg);

    Map<String, Object> req = new HashMap<>();
    req.put("msgtype", "text");
    req.put("text", text);
    req.put("safe", 0);

    String webHookUrl = EnvUtils.get("notification.webhook.url");

    return send(webHookUrl, req);
  }

}
