# Tio-Boot
[English](readme.md) | [中文](readme_cn.md)

## 官网文档
[官网文档](https://tio-boot.com/) |
[gitee](https://gitee.com/ppnt/tio-boot) | 
[github](https://gitee.com/litongjava/tio-boot)
## 概述

Tio-Boot 是新一代 Java Web 开发框架：更快、更小、更简单！它提供 2 到 3 倍的高并发，节省 1/3 到 1/2 的内存，启动速度快 5 到 10 倍，打包体积可缩小到原来的 1/2 到 1/10。Tio-Boot 基于 Java AIO 构建，能够让一台 2 核 4G 的 Linux 服务器处理上万的并发连接。

### 主要特点

1. **基于 Java AIO 和 T-IO:** 利用 Java 异步 I/O 和 T-IO 提供高效性能。
2. **引入 Spring-Boot 配置理念:** 支持常用的 Spring-Boot 注解，但不使用 Spring 的 IOC 和 AOP。
3. **集成 JFinal AOP:** 用于支持依赖注入 (DI)、控制反转 (IOC) 和面向切面编程 (AOP)。
4. **集成 JFinal Enjoy 模板引擎** 提供模板引擎支持。
5. **集成 JFinal Active Record** 提供数据库操作支持。
6. **支持常见 Web 组件:** 包括拦截器、WebSocket、处理器和控制器。

### 口号

简洁、易用、开发快、运行快。

### 宗旨

去繁求简，返璞归真，轻装上阵，高效开发。

## 优缺点

### 优势

1. **无 Servlet:** 基于 Java AIO 重写网络连接，支持异步、非阻塞和高性能。
2. **多协议支持:** 一个端口同时支持 UDP、TCP、HTTP 和 WebSocket 协议。
3. **支持嵌入式设备:** Tio-Boot 可以在 Android 系统上运行。
4. **启动速度快，体积小:** 仅提供 HTTP 服务时，打包后的 JAR 文件仅为 3MB，启动速度仅需 300ms。
5. **开发环境支持热重载:** 配合 `hotswap-classloader` 使用，可以在 20ms 内完成重载，修改代码后无需重启即可测试，大大提高开发效率。
6. **支持编译成二进制文件:** 可以使用 GraalVM 将 JAR 包编译成二进制文件。

### 资源优化

1. **内存减少一半**
2. **服务器数量减少一半**

### T-IO 性能测试数据

1. **性能测试一:** 1.9G 内存稳定支持 30 万 TCP 长连接。[详情](https://www.tiocloud.com/61)
2. **性能测试二:** 使用 T-IO 实现每秒 1051 万条聊天消息。[详情](https://www.tiocloud.com/41)
3. **性能测试三:** Netty 和 T-IO 对比测试结果。[详情](https://www.tiocloud.com/154)

### 缺点

1. **学习难度高:** 需要深厚的编程基础才能理解相关概念。

Tio-Boot 为开发者提供了高性能和高效的开发工具。然而，由于其复杂的概念和较高的学习门槛，开发者需要具备深厚的编程基础才能充分利用这个框架的优势。

## 快速开始

Tio-Boot 已发布到 Maven 仓库：[Tio-Boot](https://central.sonatype.com/artifact/com.litongjava/tio-boot)

在 `pom.xml` 中添加以下内容：
```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <java.version>1.8</java.version>
  <maven.compiler.source>${java.version}</maven.compiler.source>
  <maven.compiler.target>${java.version}</maven.compiler.target>
  <tio-boot.version>1.9.3</tio-boot.version>
  <jfinal-aop.version>1.3.5</jfinal-aop.version>
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

### 示例代码
```java
package com.litongjava.tio.web.hello;

import com.litongjava.annotation.AComponentScan;
import com.litongjava.tio.boot.TioApplication;

@AComponentScan
public class HelloApp {
  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    //TioApplicationWrapper.run(HelloApp.class, args);
    TioApplication.run(HelloApp.class, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "ms");
  }
}

```

```java
package com.litongjava.open.chat.controller;

import com.litongjava.annotation.RequestPath;

@RequestPath("/")
public class IndexController {
  @RequestPath()
  public String index() {
    return "index";
  }
}
```

## 常见问题解答

如果在使用过程中遇到问题，可以通过以下联系方式联系我。

## 贡献指南

欢迎 Fork 并提交 PR。

## 许可证

[MIT License](LICENSE)

## 联系方式

- 微信: jdk131219
- 邮箱: litongjava@qq.com
