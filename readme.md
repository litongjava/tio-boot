# tio-boot

## Introduction
tio-boot is a high-performance web framework based on Java AIO and t-io. It's asynchronous, non-blocking, and capable of handling tens of thousands of concurrent connections on a single server. This makes it particularly suitable for the development of high-performance web applications.

## Features
- **Fast Startup**: tio-boot typically starts up in under one second, providing a quicker launch.
- **Compilation to Binary**: It supports compilation to binary files through GraalVM, allowing it to run in non-JVM environments.
- **Integration with External Services**: tio-boot supports integration with various external services such as MySQL, Redis, MQ, XXL-JOB, etc.

## Quick Start
maven center [tio-boot](https://central.sonatype.com/artifact/com.litongjava/tio-boot)  

`pom.xml`
```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <java.version>1.8</java.version>
  <maven.compiler.source>${java.version}</maven.compiler.source>
  <maven.compiler.target>${java.version}</maven.compiler.target>
  <tio-boot.version>1.2.9</tio-boot.version>
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

import com.litongjava.jfinal.aop.annotation.ComponentScan;
import com.litongjava.tio.boot.TioApplication;

@ComponentScan
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

import com.litongjava.jfinal.aop.annotation.Controller;
import com.litongjava.tio.http.server.annotation.RequestPath;

@Controller
@RequestPath("/")
public class IndexController {
  @RequestPath()
  public String index() {
    return "index";
  }
}
```
## Documentation
[github](https://litongjava.github.io/tio-boot-docs/)
[gitee](https://gitee.com/ppnt/tio-boot-docs/tree/main/docs)

## Common Questions and Answers
If you encounter any issues during usage, please contact me using the information provided.

## Contribution Guide
Fork and PR

## License
[MIT License](LICENSE)

## Contact Information
- WeChat: jdk131219
- Email: litongjava@qq.com