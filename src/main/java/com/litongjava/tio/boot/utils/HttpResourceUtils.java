package com.litongjava.tio.boot.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpResource;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.tio.utils.hutool.StrUtil;

public class HttpResourceUtils {

  /**
   * 
   * @param request
   * @param path    形如 /xx/aa.html
   * @return
   * @throws Exception
   */
  public static HttpResource getResource(String pageRoot, String path) {
    HttpResource httpResource = null;
    if (pageRoot != null) {
      if (StrUtil.endWith(path, "/")) {
        path = path + "index.html";
      }
      String complatePath = pageRoot + path;
      File file = new File(complatePath);
      if (file.exists()) {
        httpResource = new HttpResource(path, null, file);
      }
    }

    return httpResource;
  }

  /**
   * read file
   * @param httpResponse
   * @param httpResource
   * @return
   * @throws IOException
   */
  public static HttpResponse buildFileResponse(HttpResponse httpResponse, HttpResource httpResource) throws IOException {
    File file = httpResource.getFile();
    byte[] fileContent = Files.readAllBytes(file.toPath());
    String extension = getFileExtension(file.getName());

    HttpResponse response = Resps.bytes(httpResponse, fileContent, extension);
    response.setStaticRes(true);
    response.setLastModified(HeaderValue.from(String.valueOf(file.lastModified())));
    return response;
  }

  private static String getFileExtension(String filename) {
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(dotIndex + 1) : "";
  }

}
