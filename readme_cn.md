# tio-boot

宗旨:去繁求减,返璞归真,轻装上阵,高效开发  

## 简介
tio-boot基于java aio和t-io的异步,非阻塞,高性能web框架,可以让单台服务器承受上万并发,特别适用于高性能web应用开发 

## 功能特点
- **特点1**: 更快的启动速度,通常情况下可以在1秒之内完成服务启动
- **特点2**: 支持编译成二进制文件.通过graalvm可以编译成二进制文件,支持在非jvm的环境下运行
- **特点3**: 支持和多种外部服务进行整合mysql,redis,mq,xxl-job等等


## 快速开始
已经发布到maven仓库 [tio-boot](https://central.sonatype.com/artifact/com.litongjava/tio-boot)  
pom.mxl
```
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>1.8</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    <tio-boot.version>1.3.1</tio-boot.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>com.litongjava</groupId>
      <artifactId>tio-boot</artifactId>
      <version>${tio-boot.version}</version>
    </dependency>
  </dependencies>
```

```java
package com.litongjava.tio.web.hello;

import com.litongjava.jfinal.aop.annotation.AComponentScan;
import com.litongjava.tio.boot.TioApplication;

@AComponentScan
public class HelloApp {
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    TioApplication.run(HelloApp.class, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}
```
```
package com.litongjava.tio.web.hello;

import com.litongjava.tio.http.server.annotation.RequestPath;

@RequestPath("/")
public class IndexController {
  @RequestPath()
  public String index() {
    return "index";
  }
}
```

## 文档
[github](https://litongjava.github.io/tio-boot-docs/)
[gitee](https://gitee.com/ppnt/tio-boot-docs/tree/main/docs)

## 常见问题与解答
如果使用过程中遇到问题,可以通过联系方式联系我

## 贡献指南
Fork and PR

## 许可证
[MIT License](LICENSE)

## 联系方式
- 微信:jdk131219
- 邮箱:litongjava@qq.com

