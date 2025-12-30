# Tio-Boot
[English](readme.md) | [中文](readme_cn.md)

## Document
[Document](https://tio-boot.com/) |
[gitee](https://gitee.com/ppnt/tio-boot) |
[github](https://github.com/litongjava/tio-boot)

## Overview

Tio-Boot is a new generation Java web development framework that is faster, smaller, and simpler! It offers 2 to 3 times higher concurrency, saves 1/3 to 1/2 of memory, starts up 5 to 10 times faster, and reduces package size by 1/2 to 1/10. Built on Java AIO, Tio-Boot enables a 2-core 4G Linux server to handle tens of thousands of concurrent connections.

### Key Features

1. **Based on Java AIO and T-IO:** Utilizes Java asynchronous I/O and T-IO for high efficiency.
2. **Incorporates Spring-Boot Configuration Principles:** Supports commonly used Spring-Boot annotations without using Spring's IOC and AOP.
3. **Integrates JFinal AOP:** Supports DI, IOC, and AOP for dependency injection.
4. **Includes JFinal Enjoy Template Engine and Active Record:** Provides support for database operations and template engine.
5. **Supports Common Web Components:** Includes interceptors, WebSocket, handlers, and controllers.

### Slogan

Simplicity, ease of use, rapid development, and fast execution.

### Philosophy

Striving for simplicity, returning to basics, staying lightweight, and developing efficiently.

## Pros and Cons

### Advantages

1. **No Servlet:** Uses Java AIO for network connections, supporting asynchronous, non-blocking, and high performance.
2. **Multi-Protocol Support:** Supports UDP, TCP, HTTP, and WebSocket on a single port.
3. **Compatible with Embedded Devices:** Can run on Android systems.
4. **Fast Startup and Small Size:** When providing HTTP service only, the packaged JAR file is 3MB, with a startup time of 300ms.
5. **Hot Reload Support in Development Environment:** When used with `hotswap-classloader`, it enables reloading within 20ms without restarting the application, significantly improving development efficiency.
6. **Supports Compilation into Binary Files:** Can be compiled into binary files using GraalVM.

### Resource Optimization

1. **Memory Usage Halved**
2. **Server Count Halved**

### T-IO Performance Metrics

1. **Performance Test 1:** 1.9G memory stably supports 300,000 TCP long connections. [Details](https://www.tiocloud.com/61)
2. **Performance Test 2:** T-IO achieves 10.51 million chat messages per second. [Details](https://www.tiocloud.com/41)
3. **Performance Test 3:** Comparison test results between Netty and T-IO. [Details](https://www.tiocloud.com/154)

### Disadvantages

1. **High Learning Curve:** Requires a solid programming foundation to understand related concepts.

Tio-Boot offers developers a high-performance and efficient development tool. However, due to its complex concepts and steep learning curve, a deep programming background is necessary to fully leverage its advantages.

## Quick Start

Tio-Boot is available in the Maven repository: [Tio-Boot](https://central.sonatype.com/artifact/com.litongjava/tio-boot)  

Add the following to your `pom.xml`:
```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <java.version>1.8</java.version>
  <maven.compiler.source>${java.version}</maven.compiler.source>
  <maven.compiler.target>${java.version}</maven.compiler.target>
  <tio-boot.version>2.0.4</tio-boot.version>
  <jfinal-aop.version>1.3.8</jfinal-aop.version>
</properties>
<dependencies>
  <dependency>
    <groupId>com.litongjava</groupId>
    <artifactId>tio-boot</artifactId>
    <version>${tio-boot.version}</version>
  </dependency>
  <dependency>
    <groupId>com.litongjava</groupId>
    <artifactId>jfinal-aop</artifactId>
    <version>${jfinal-aop.version}</version>
  </dependency>  
</dependencies>
```

### Sample Code
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

```java
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

## FAQ
If you encounter any issues while using Tio-Boot, feel free to reach out using the contact information provided below.

## Contribution Guide
Feel free to fork the repository and submit a pull request.

## License
[MIT License](LICENSE)

## Contact Information
- WeChat: jdk131219
- Email: litongjava@qq.com

