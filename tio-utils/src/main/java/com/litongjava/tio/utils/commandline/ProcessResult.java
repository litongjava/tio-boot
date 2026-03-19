package com.litongjava.tio.utils.commandline;

import java.io.File;
import java.util.List;

public class ProcessResult {
  private int exitCode;
  private String stdOut;
  private String stdErr;
  private Long elapsed;

  private Boolean cached;

  private Long prt;
  private Long sessionId;
  private Long taskId;
  private String sources;
  private String executeCode;
  private String output;
  private String json;
  private String message;
  private String path;
  private String text;
  private String subtitle;
  private String image;
  private String audio;
  private String video;
  private String hlsUrl;
  private String ppt;
  private Double video_length;
  private List<String> texts;
  private List<String> images;
  private List<String> audios;
  private List<String> videos;
  private Object data;

  public ProcessResult(String output) {
    this.output = output;
  }

  public ProcessResult(String output, boolean cached) {
    this.output = output;
    this.cached = cached;
  }

  public static ProcessResult buildMessage(String message) {
    return new ProcessResult().setMessage(message);
  }

  public ProcessResult() {
  }

  public ProcessResult(int exitCode, String stdOut, String stdErr, Long elapsed, Boolean cached, Long prt,
      Long sessionId, Long taskId, String executeCode, String output, String json, String message, String file,
      String text, String subtitle, String image, String audio, String video, String hlsUrl, String ppt,
      Double video_length, List<String> texts, List<String> images, List<String> audios, List<String> videos,
      Object data) {
    super();
    this.exitCode = exitCode;
    this.stdOut = stdOut;
    this.stdErr = stdErr;
    this.elapsed = elapsed;
    this.cached = cached;
    this.prt = prt;
    this.sessionId = sessionId;
    this.taskId = taskId;
    this.executeCode = executeCode;
    this.output = output;
    this.json = json;
    this.message = message;
    this.path = file;
    this.text = text;
    this.subtitle = subtitle;
    this.image = image;
    this.audio = audio;
    this.video = video;
    this.hlsUrl = hlsUrl;
    this.ppt = ppt;
    this.video_length = video_length;
    this.texts = texts;
    this.images = images;
    this.audios = audios;
    this.videos = videos;
    this.data = data;
  }

  public int getExitCode() {
    return exitCode;
  }

  public void setExitCode(int exitCode) {
    this.exitCode = exitCode;
  }

  public String getStdOut() {
    return stdOut;
  }

  public void setStdOut(String stdOut) {
    this.stdOut = stdOut;
  }

  public String getStdErr() {
    return stdErr;
  }

  public void setStdErr(String stdErr) {
    this.stdErr = stdErr;
  }

  public Long getElapsed() {
    return elapsed;
  }

  public void setElapsed(Long elapsed) {
    this.elapsed = elapsed;
  }

  public Boolean getCached() {
    return cached;
  }

  public void setCached(Boolean cached) {
    this.cached = cached;
  }

  public Long getPrt() {
    return prt;
  }

  public void setPrt(Long prt) {
    this.prt = prt;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public String getExecuteCode() {
    return executeCode;
  }

  public void setExecuteCode(String executeCode) {
    this.executeCode = executeCode;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public String getJson() {
    return json;
  }

  public void setJson(String json) {
    this.json = json;
  }

  public String getMessage() {
    return message;
  }

  public ProcessResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String file) {
    this.path = file;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(String subtitle) {
    this.subtitle = subtitle;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getAudio() {
    return audio;
  }

  public void setAudio(String audio) {
    this.audio = audio;
  }

  public String getVideo() {
    return video;
  }

  public void setVideo(String video) {
    this.video = video;
  }

  public String getHlsUrl() {
    return hlsUrl;
  }

  public void setHlsUrl(String hlsUrl) {
    this.hlsUrl = hlsUrl;
  }

  public String getPpt() {
    return ppt;
  }

  public void setPpt(String ppt) {
    this.ppt = ppt;
  }

  public Double getVideo_length() {
    return video_length;
  }

  public void setVideo_length(Double video_length) {
    this.video_length = video_length;
  }

  public List<String> getTexts() {
    return texts;
  }

  public void setTexts(List<String> texts) {
    this.texts = texts;
  }

  public List<String> getImages() {
    return images;
  }

  public void setImages(List<String> images) {
    this.images = images;
  }

  public List<String> getAudios() {
    return audios;
  }

  public void setAudios(List<String> audios) {
    this.audios = audios;
  }

  public List<String> getVideos() {
    return videos;
  }

  public void setVideos(List<String> videos) {
    this.videos = videos;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public String getSources() {
    return sources;
  }

  public void setSources(String sources) {
    this.sources = sources;
  }
  
  public static ProcessResult fromFile(File file, boolean b) {
    ProcessResult processResult = new ProcessResult();
    processResult.setCached(b);
    processResult.setPath(file.getPath());
    return processResult;
  }
  
}