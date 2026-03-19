package com.litongjava.tio.utils.notification;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.litongjava.constants.ServerConfigKeys;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.network.IpUtils;

public class NotifactionWarmModel {
  // yyyy-MM-dd HH:mm:ssXXX -> e.g. 2025-07-08 10:30:00+08:00
  public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");

  private ZonedDateTime time;
  private String appEnv;
  private String appGroupName;
  private String appName;
  private String warningName;
  private String level;
  private String deviceName;
  private String userIp;
  private String userId;
  private String requestId;
  private String host;
  private String referer;
  private String userAgent;
  private String requestLine;
  private String requestBody;
  private Integer statusCode;
  private String exceptionId;
  private String stackTrace;
  private String sessionId;
  private String taskId;
  private String url;
  private String requestUrl;
  private String text;
  private String content;

  private Map<String, String> headers;
  private Map<String, Object[]> params;

  public NotifactionWarmModel(ZonedDateTime time, String env, String appGroupName, String appName, String warningName,
      String level, String deviceName, String content) {
    this.time = time;
    this.appEnv = env;
    this.appGroupName = appGroupName;
    this.appName = appName;
    this.warningName = warningName;
    this.level = level;
    this.deviceName = deviceName;
    this.content = content;
  }

  public NotifactionWarmModel() {
  }

  public String format() {
    StringBuilder sb = new StringBuilder();
    // Alarm Time
    if (time != null) {
      sb.append(String.format("- Time : %s\n", time.format(dateTimeFormatter)));
    }
    // App Env
    if (appEnv != null) {
      sb.append(String.format("- App Env : %s\n", appEnv));
    }
    // App Group Name
    if (appGroupName != null) {
      sb.append(String.format("- App Group Name : %s\n", appGroupName));
    }
    // App Name
    if (appName != null) {
      sb.append(String.format("- App Name : %s\n", appName));
    }
    // Alarm Name (Warning Name)
    if (warningName != null) {
      sb.append(String.format("- Name : %s\n", warningName));
    }
    // Alarm Level
    if (level != null) {
      sb.append(String.format("- Level : %s\n", level));
    }
    // Alarm Device
    if (deviceName != null) {
      sb.append(String.format("- Device : %s\n", deviceName));
    }
    // User Ip
    if (userIp != null) {
      sb.append(String.format("- User Ip : %s\n", userIp));
    }

    // user Id
    if (userId != null) {
      sb.append(String.format("- User Id : %s\n", userId));
    }

    // Request Id
    if (requestId != null) {
      sb.append(String.format("- Request Id : %s\n", requestId));
    }

    // Request Line
    if (requestLine != null) {
      sb.append(String.format("- Request Line : %s\n", requestLine));
    }
    // Host
    if (host != null) {
      sb.append(String.format("- Host : %s\n", host));
    }
    // Refer
    if (referer != null) {
      sb.append(String.format("- Referer : %s\n", referer));
    }
    //
    if (userAgent != null) {
      sb.append(String.format("- UserAgent : %s\n", userAgent));
    }
    if (headers != null) {
      sb.append(String.format("- UserAgent : %s\n", JsonUtils.toJson(headers)));
    }
    if (params != null) {
      sb.append(String.format("- UserAgent : %s\n", JsonUtils.toJson(params)));
    }
    // Request Body
    if (requestBody != null) {
      sb.append(String.format("- Request Body : %s\n", requestBody));
    }

    // Request Body
    if (statusCode != null) {
      sb.append(String.format("- Status Code : %d\n", statusCode));
    }

    if (exceptionId != null) {
      sb.append(String.format("- Exception Id : %s\n", exceptionId));
    }
    // Stack Trace
    if (stackTrace != null) {
      sb.append(String.format("- Stack Trace : %s\n", stackTrace));
    }
    if (sessionId != null) {
      sb.append("- SessionId : \n");
      sb.append(String.format("%s\n", sessionId));
    }

    if (taskId != null) {
      sb.append("- TaskId : \n");
      sb.append(String.format("%s\n", taskId));
    }

    if (url != null) {
      sb.append("- URL : \n");
      sb.append(String.format("%s\n", url));
    }

    if (requestUrl != null) {
      sb.append("- RequestUrl : \n");
      sb.append(String.format("%s\n", requestUrl));
    }

    if (text != null) {
      sb.append("- Text : \n");
      sb.append(String.format("%s\n", text));
    }

    // Alarm Content
    if (content != null) {
      sb.append("- Content : \n");
      sb.append(String.format("%s\n", content));
    }
    return sb.toString();
  }

  public ZonedDateTime getTime() {
    return time;
  }

  public NotifactionWarmModel setTime(ZonedDateTime time) {
    this.time = time;
    return this;
  }

  public String getAppEnv() {
    return appEnv;
  }

  public NotifactionWarmModel setAppEnv(String appEnv) {
    this.appEnv = appEnv;
    return this;
  }

  public String getAppGroupName() {
    return appGroupName;
  }

  public NotifactionWarmModel setAppGroupName(String appGroupName) {
    this.appGroupName = appGroupName;
    return this;
  }

  public String getAppName() {
    return appName;
  }

  public NotifactionWarmModel setAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public String getWarningName() {
    return warningName;
  }

  public NotifactionWarmModel setWarningName(String warningName) {
    this.warningName = warningName;
    return this;
  }

  public String getLevel() {
    return level;
  }

  public NotifactionWarmModel setLevel(String level) {
    this.level = level;
    return this;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public NotifactionWarmModel setDeviceName(String deviceName) {
    this.deviceName = deviceName;
    return this;
  }

  public String getUserIp() {
    return userIp;
  }

  public NotifactionWarmModel setUserIp(String userIp) {
    this.userIp = userIp;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public NotifactionWarmModel setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRequestId() {
    return requestId;
  }

  public NotifactionWarmModel setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getHost() {
    return host;
  }

  public NotifactionWarmModel setHost(String host) {
    this.host = host;
    return this;
  }

  public String getReferer() {
    return referer;
  }

  public NotifactionWarmModel setReferer(String referer) {
    this.referer = referer;
    return this;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public NotifactionWarmModel setUserAgent(String userAgent) {
    this.userAgent = userAgent;
    return this;
  }

  public String getRequestLine() {
    return requestLine;
  }

  public NotifactionWarmModel setRequestLine(String requestLine) {
    this.requestLine = requestLine;
    return this;
  }

  public String getRequestBody() {
    return requestBody;
  }

  public NotifactionWarmModel setRequestBody(String requestBody) {
    this.requestBody = requestBody;
    return this;
  }

  public Integer getStatusCode() {
    return statusCode;
  }

  public NotifactionWarmModel setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public String getExceptionId() {
    return exceptionId;
  }

  public NotifactionWarmModel setExceptionId(String exceptionId) {
    this.exceptionId = exceptionId;
    return this;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public NotifactionWarmModel setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getContent() {
    return content;
  }

  public NotifactionWarmModel setContent(String content) {
    this.content = content;
    return this;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public NotifactionWarmModel setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  public Map<String, Object[]> getParams() {
    return params;
  }

  public NotifactionWarmModel setParams(Map<String, Object[]> params) {
    this.params = params;
    return this;
  }

  public String getSessionId() {
    return sessionId;
  }

  public NotifactionWarmModel setSessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public String getTaskId() {
    return taskId;
  }

  public NotifactionWarmModel setTaskId(String taskId) {
    this.taskId = taskId;
    return this;
  }

  public String getText() {
    return text;
  }

  public NotifactionWarmModel setText(String text) {
    this.text = text;
    return this;
  }

  public static DateTimeFormatter getDatetimeformatter() {
    return dateTimeFormatter;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRequestUrl() {
    return requestUrl;
  }

  public void setRequestUrl(String requestUrl) {
    this.requestUrl = requestUrl;
  }

  public static NotifactionWarmModel fromException(String warningName, String level, String content,
      String stackTrace) {
    NotifactionWarmModel model = new NotifactionWarmModel();
    model.setStackTrace(stackTrace);
    String localIp = IpUtils.getLocalIp();
    model.setAppEnv(EnvUtils.env());
    model.setAppGroupName(ServerConfigKeys.APP_GROUP_NAME);
    model.setAppName(EnvUtils.get(ServerConfigKeys.APP_NAME));

    model.setWarningName(warningName);

    model.setLevel(level);
    model.setDeviceName(localIp);
    model.setTime(ZonedDateTime.now());
    model.setContent(content);
    return model;
  }

  public static NotifactionWarmModel fromException(String warningName, String level, String content, Exception e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    String stackTrace = sw.toString();
    return fromException(warningName, level, content, stackTrace);
  }
}
