# t-io
The package is distributed through Maven Central.
[t-io](https://central.sonatype.com/artifact/com.litongjava/t-io),
[tio-core](https://central.sonatype.com/artifact/com.litongjava/tio-core),
[tio-http-server](https://central.sonatype.com/artifact/com.litongjava/tio-http-server),
[tio-websocket-server](https://central.sonatype.com/artifact/com.litongjava/tio-websocket-server),
[tio-server](https://central.sonatype.com/artifact/com.litongjava/tio--server),

## tio-http-server打包为二进制文件
### 源码地址
https://github.com/litongjava/java-ee-tio-boot-study/tree/main/tio-http-server-study/tio-http-server-hello


### 打包 Java 应用为二进制文件的优势
打包 Java 应用为二进制文件，带来了一系列的好处，这些好处对于提升应用的性能、分发、以及运维方面都非常有益。以下是一些主要优势：

1. **更快的启动时间**：二进制文件通常比传统的 JVM 启动方式快得多。这是因为它们直接编译到了本地代码，减少了JVM初始化和类加载所需的时间。这在需要快速启动和执行的微服务和云函数（如 AWS Lambda）中特别有用。

2. **减少内存占用**：编译成二进制文件的应用通常有更小的内存占用。这是因为它们避免了运行时环境的一些开销，如 JVM 的垃圾收集和 JIT 编译。

3. **简化部署**：二进制文件使得部署过程更加简单。你只需要一个文件，不再需要单独安装和配置 JVM 环境。这简化了在不同环境中的部署和迁移过程。

4. **提高性能**：直接编译为机器码可以提高应用性能，尤其是对于计算密集型应用。这种方式可以更好地利用硬件资源，提高运行效率。

5. **跨平台兼容性**：通过适当的配置和环境设置，可以为不同的操作系统和硬件架构创建专门的二进制文件，增加应用的可移植性。

6. **安全性增强**：编译成二进制文件可以在一定程度上提高安全性，因为它减少了运行时代码注入和其他基于 JVM 的攻击的可能性。

7. **减少依赖**：由于所有必需的库和依赖都被包含在单个二进制文件中，因此减少了对外部库和环境的依赖。

8. **优化资源利用**：在容器化和云基础设施环境中，资源利用的优化尤为重要。二进制文件的低内存占用和快速启动特性使得它们非常适合这些环境。

### 1.1.创建工程
首先，创建一个 Maven 工程并添加所需的依赖和配置。这里特别指出了使用 `3.7.3.v20231223-RELEASE` 版本的 `tio-http-server`，该版本做了优化，使用了自定义的 `mapcache` 替代了 `caffeine`。示例中提供了 `pom.xml` 文件的配置，涵盖了 Java 版本、依赖库、以及特定的构建配置。
```
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>1.8</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>

    <graalvm.version>23.1.1</graalvm.version>
    <mica-mqtt.version>2.2.6</mica-mqtt.version>
    <mica-net.version>0.1.6</mica-net.version>
    <tinylog.version>2.6.2</tinylog.version>
    <mainClass.server>demo.DemoHttpServer</mainClass.server>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.litongjava</groupId>
      <artifactId>tio-http-server</artifactId>
      <version>3.7.3.v20231223-RELEASE</version>
    </dependency>
  </dependencies>
  <build>
    <finalName>${project.artifactId}</finalName>
  </build>
  <profiles>
    <profile>
      <id>jar</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <dependencies>
        <!-- 非 GraalVM 环境用 tinylog -->
        <dependency>
          <groupId>org.tinylog</groupId>
          <artifactId>slf4j-tinylog</artifactId>
          <version>${tinylog.version}</version>
        </dependency>
        <dependency>
          <groupId>org.tinylog</groupId>
          <artifactId>tinylog-impl</artifactId>
          <version>${tinylog.version}</version>
        </dependency>
      </dependencies>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.2.0</version>
          </plugin>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.1.1</version>
            <configuration>
              <archive>
                <manifest>
                  <mainClass>${mainClass.server}</mainClass>
                </manifest>
              </archive>
              <descriptorRefs>
                <descriptorRef>jar-with-dependencies</descriptorRef>
              </descriptorRefs>
              <appendAssemblyId>false</appendAssemblyId>
            </configuration>
            <executions>
              <execution>
                <id>make-assembly</id>
                <phase>package</phase>
                <goals>
                  <goal>single</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
    </profile>
    <profile>
      <id>server-graalvm</id>
      <dependencies>
        <!-- GraalVM 环境使用 jdk log -->
        <dependency>
          <groupId>org.slf4j</groupId>
          <artifactId>slf4j-jdk14</artifactId>
          <version>1.7.31</version>
        </dependency>
        <!-- GraalVM -->
        <dependency>
          <groupId>org.graalvm.sdk</groupId>
          <artifactId>graal-sdk</artifactId>
          <version>${graalvm.version}</version>
          <scope>provided</scope>
        </dependency>
      </dependencies>
      <build>
        <finalName>tio-http-server-graal</finalName>
        <plugins>
          <plugin>
            <groupId>org.graalvm.nativeimage</groupId>
            <artifactId>native-image-maven-plugin</artifactId>
            <version>21.2.0</version>
            <executions>
              <execution>
                <goals>
                  <goal>native-image</goal>
                </goals>
                <phase>package</phase>
              </execution>
            </executions>
            <configuration>
              <skip>false</skip>
              <imageName>${project.artifactId}</imageName>
              <mainClass>${mainClass.server}</mainClass>
              <buildArgs>
                -H:+RemoveSaturatedTypeFlows
                --allow-incomplete-classpath
                --no-fallback
              </buildArgs>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
```
通过依赖可以分析得出tio-http-server仅仅依赖了fastjson2和slf4j-api
### 1.2.编写代码
接下来，编写一个简单的控制器 `IndexController` 和一个启动类 `DemoHttpServer`。这些类定义了基本的 HTTP 请求处理逻辑，并设置了路由。
```
package demo.controller;

import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.util.Resps;

public class IndexController {

  public HttpResponse index(HttpRequest request) {
    return Resps.txt(request, "index");

  }

  public HttpResponse login(HttpRequest request) {
    return Resps.txt(request, "login");
  }

  public HttpResponse exception(HttpRequest request) {
    throw new RuntimeException("error");
  }
}
```
```
package demo;

import java.io.IOException;

import com.litongjava.tio.http.common.HttpConfig;
import com.litongjava.tio.http.common.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.HttpServerStarter;
import com.litongjava.tio.http.server.handler.HttpRoutes;
import com.litongjava.tio.http.server.handler.SimpleHttpDispatcherHandler;
import com.litongjava.tio.http.server.handler.SimpleHttpRoutes;

import demo.controller.IndexController;

public class DemoHttpServer {

  public static void main(String[] args) throws IOException {

    // 实例化Controller
    IndexController controller = new IndexController();

    // 手动添加路由
    HttpRoutes simpleHttpRoutes = new SimpleHttpRoutes();
    simpleHttpRoutes.add("/", controller::index);
    simpleHttpRoutes.add("/login", controller::login);
    simpleHttpRoutes.add("/exception", controller::exception);

    // 配置服务服务器
    HttpConfig httpConfig;
    HttpRequestHandler requestHandler;
    HttpServerStarter httpServerStarter;

    httpConfig = new HttpConfig(80, null, null, null);
    requestHandler = new SimpleHttpDispatcherHandler(httpConfig, simpleHttpRoutes);
    httpServerStarter = new HttpServerStarter(httpConfig, requestHandler);
    // 启动服务器
    httpServerStarter.start();
  }
}
```
#### 1.3.配置环境
为了打包成二进制文件，需要安装 GraalVM 和 Maven。文档中提供了详细的安装步骤，包括下载链接、解压指令和环境变量设置。
#### Install GraalVM

1. Download and extract GraalVM:

   ```shell
   wget https://download.oracle.com/graalvm/21/latest/graalvm-jdk-21_linux-x64_bin.tar.gz
   mkdir -p ~/program/
   tar -xf graalvm-jdk-21_linux-x64_bin.tar.gz -C ~/program/
   ```

2. Set environment variables:

   ```shell
   export JAVA_HOME=~/program/graalvm-jdk-21.0.1+12.1
   export GRAALVM_HOME=~/program/graalvm-jdk-21.0.1+12.1
   export PATH=$JAVA_HOME/bin:$PATH
   ```

#### Install Maven

1. Download and extract Maven:

   ```shell
   wget https://dlcdn.apache.org/maven/maven-3/3.8.8/binaries/apache-maven-3.8.8-bin.zip
   unzip apache-maven-3.8.8-bin.zip -d ~/program/
   ```

2. Set environment variables:

   ```shell
   export MVN_HOME=~/program/apache-maven-3.8.8/
   export PATH=$MVN_HOME/bin:$PATH
   ```

### 1.4.打包
下面介绍如何使用 Maven 打包 Java Jar 文件，以及如何构建二进制镜像。

#### Build Java Jar (Optional)

```shell
mvn package
```

#### Build Binary Image

```shell
mvn clean package -DskipTests -Pserver-graalvm
```

#### 实际执行的打包命令是
```
/root/program/graalvm-jdk-21.0.1+12.1/lib/svm/bin/native-image -cp /root/.m2/repository/com/litongjava/tio-http-server/3.7.3.v20231223-RELEASE/tio-http-server-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-http-common/3.7.3.v20231223-RELEASE/tio-http-common-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-core/3.7.3.v20231223-RELEASE/tio-core-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-utils/3.7.3.v20231223-RELEASE/tio-utils-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/alibaba/fastjson2/fastjson2/2.0.43/fastjson2-2.0.43.jar:/root/.m2/repository/org/slf4j/slf4j-jdk14/1.7.31/slf4j-jdk14-1.7.31.jar:/root/.m2/repository/org/slf4j/slf4j-api/1.7.31/slf4j-api-1.7.31.jar:/root/code/java-ee-tio-boot-study/tio-http-server-study/tio-http-server-hello/target/tio-http-server-graal.jar -H:+RemoveSaturatedTypeFlows --allow-incomplete-classpath --no-fallback -H:Class=demo.DemoHttpServer -H:Name=tio-http-server-hello
```
#### 打包过程中的部分日志如下
```
========================================================================================================================
GraalVM Native Image: Generating 'tio-http-server-hello' (executable)...
========================================================================================================================
[1/8] Initializing...                                                                                    (7.5s @ 0.08GB)
 Java version: 21.0.1+12, vendor version: Oracle GraalVM 21.0.1+12.1
 Graal compiler: optimization level: 2, target machine: x86-64-v3, PGO: ML-inferred
 C compiler: gcc (linux, x86_64, 9.4.0)
 Garbage collector: Serial GC (max heap size: 80% of RAM)
 1 user-specific feature(s):
 - com.oracle.svm.thirdparty.gson.GsonFeature
------------------------------------------------------------------------------------------------------------------------
 1 experimental option(s) unlocked:
 - '-H:Name' (alternative API option(s): -o tio-http-server-hello; origin(s): command line)
------------------------------------------------------------------------------------------------------------------------
Build resources:
 - 5.80GB of memory (75.6% of 7.67GB system memory, determined at start)
 - 4 thread(s) (100.0% of 4 available processor(s), determined at start)
[2/8] Performing analysis...  [*****]                                                                   (86.7s @ 0.67GB)
    7,459 reachable types   (84.7% of    8,809 total)
   11,123 reachable fields  (57.5% of   19,337 total)
   39,511 reachable methods (59.1% of   66,902 total)
    2,219 types,   129 fields, and 1,856 methods registered for reflection
       60 types,    58 fields, and    55 methods registered for JNI access
        4 native libraries: dl, pthread, rt, z
[3/8] Building universe...                                                                              (10.6s @ 0.80GB)
[4/8] Parsing methods...      [******]                                                                  (34.7s @ 1.04GB)
[5/8] Inlining methods...     [***]                                                                      (4.2s @ 0.82GB)
[6/8] Compiling methods...    [*************]                                                          (188.6s @ 1.06GB)
[7/8] Layouting methods...    [[7/8] Layouting methods...    [***]                                                                      (8.2s @ 0.97GB)
[8/8] Creating image...       [[8/8] Creating image...       [***]                                                                      (5.5s @ 1.07GB)
  23.13MB (56.26%) for code area:    23,354 compilation units
  16.49MB (40.11%) for image heap:  223,378 objects and 49 resources
   1.49MB ( 3.63%) for other data
  41.11MB in total
------------------------------------------------------------------------------------------------------------------------
Top 10 origins of code area:                                Top 10 object types in image heap:
  12.91MB java.base                                            6.30MB byte[] for code metadata
   4.43MB fastjson2-2.0.43.jar                                 2.77MB byte[] for java.lang.String
   3.47MB svm.jar (Native Image)                               1.57MB java.lang.String
 353.79kB java.rmi                                             1.31MB java.lang.Class
 265.89kB java.naming                                        616.51kB byte[] for general heap data
 261.14kB tio-core-3.7.3.v20231223-RELEASE.jar               431.95kB byte[] for reflection metadata
 249.74kB jdk.crypto.ec                                      349.64kB com.oracle.svm.core.hub.DynamicHubCompanion
 168.27kB com.oracle.svm.svm_enterprise                      309.25kB java.util.HashMap$Node
 157.43kB java.logging                                       223.13kB java.lang.String[]
 130.24kB jdk.naming.dns                                     218.28kB c.o.svm.core.hub.DynamicHub$ReflectionMetadata
 627.32kB for 21 more packages                                 2.44MB for 2015 more object types
                              Use '-H:+BuildReport' to create a report with more details.
------------------------------------------------------------------------------------------------------------------------
Security report:
 - Binary includes Java deserialization.
 - Use '--enable-sbom' to embed a Software Bill of Materials (SBOM) in the binary.
------------------------------------------------------------------------------------------------------------------------
Recommendations:
 G1GC: Use the G1 GC ('--gc=G1') for improved latency and throughput.
 PGO:  Use Profile-Guided Optimizations ('--pgo') for improved throughput.
 INIT: Adopt '--strict-image-heap' to prepare for the next GraalVM release.
 HEAP: Set max heap for improved and more predictable memory usage.
 CPU:  Enable more CPU features with '-march=native' for improved performance.
------------------------------------------------------------------------------------------------------------------------
                       30.1s (8.6% of total time) in 442 GCs | Peak RSS: 2.16GB | CPU load: 3.80
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /root/code/java-ee-tio-boot-study/tio-http-server-study/tio-http-server-hello/target/tio-http-server-hello (executable)
```

生成的二进制文件tio-http-server-hello有42M
### 1.5.启动测试
启动服务器,启动时间仅为13ms,服务器启动的日志如下包括服务器配置、启动时间和进程 ID 等信息。
```
root@ping-Inspiron-3458:~/code/java-ee-tio-boot-study/tio-http-server-study/tio-http-server-hello# ./target/tio-http-server-hello 
Dec 26, 2023 6:29:33 PM com.litongjava.tio.server.TioServer start
INFO: 
|----------------------------------------------------------------------------------------|
| t-io site         | https://www.litongjava.com/t-io                                    |
| t-io on gitee     | https://gitee.com/ppnt/t-io                                        |
| t-io on github    | https://github.com/litongjava/t-io                                 |
| t-io version      | 3.7.3.v20231223-RELEASE                                            |
| ---------------------------------------------------------------------------------------|
| TioConfig name    | Tio Http Server                                                    |
| Started at        | 2023-12-26 18:29:33                                                |
| Listen on         | 0.0.0.0:80                                                         |
| Main Class        | java.lang.invoke.LambdaForm$DMH/sa346b79c                          |
| Jvm start time    | 8ms                                                                |
| Tio start time    | 5ms                                                                |
| Pid               | 9426                                                               |
|----------------------------------------------------------------------------------------|