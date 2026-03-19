# t-io
The package is distributed through Maven Central.
[t-io](https://central.sonatype.com/artifact/com.litongjava/t-io),
[tio-core](https://central.sonatype.com/artifact/com.litongjava/tio-core),
[tio-http-server](https://central.sonatype.com/artifact/com.litongjava/tio-http-server),
[tio-websocket-server](https://central.sonatype.com/artifact/com.litongjava/tio-websocket-server),
[tio-server](https://central.sonatype.com/artifact/com.litongjava/tio--server),

## Packaging tio-http-server into a Binary File
### Source Code Location
https://github.com/litongjava/java-ee-tio-boot-study/tree/main/tio-http-server-study/tio-http-server-hello

### Advantages of Packaging Java Applications into Binary Files
Packaging Java applications as binary files brings a series of benefits that are very beneficial for improving the performance, distribution, and operational aspects of the application. Here are some of the main advantages:

1. **Faster Startup Time**: Binary files usually start much faster than traditional JVM startup methods. This is because they are directly compiled into native code, reducing the time required for JVM initialization and class loading. This is especially useful in microservices and cloud functions (such as AWS Lambda) that require quick startup and execution.

2. **Reduced Memory Footprint**: Applications compiled into binary files typically have a smaller memory footprint. This is because they avoid some of the overheads of the runtime environment, such as JVM garbage collection and JIT compilation.

3. **Simplified Deployment**: Binary files simplify the deployment process. You only need a single file, without the need to separately install and configure the JVM environment. This simplifies the deployment and migration process in different environments.

4. **Improved Performance**: Direct compilation into machine code can improve application performance, especially for compute-intensive applications. This approach can make better use of hardware resources and improve operational efficiency.

5. **Cross-Platform Compatibility**: With proper configuration and environmental settings, specialized binary files can be created for different operating systems and hardware architectures, increasing the portability of the application.

6. **Enhanced Security**: Compiling into binary files can improve security to some extent, as it reduces the possibility of runtime code injection and other JVM-based attacks.

7. **Reduced Dependencies**: Since all necessary libraries and dependencies are included in a single binary file, it reduces the dependence on external libraries and environments.

8. **Optimized Resource Utilization**: In containerized and cloud infrastructure environments, the optimization of resource utilization is particularly important. The low memory footprint and quick startup characteristics of binary files make them very suitable for these environments.

### 1.1. Creating the Project
First, create a Maven project and add the necessary dependencies and configuration. It is specifically pointed out to use version `3.7.3.v20231223-RELEASE` of `tio-http-server`, which has been optimized to use a custom `mapcache` instead of `caffeine`. The example provides the configuration of the `pom.xml` file, covering the Java version, dependencies, and specific build configurations.
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
The dependencies reveal that tio-http-server only depends on fastjson2 and slf4j-api.

### 1.2. Writing the Code
Next, write a simple controller `IndexController` and a startup class `DemoHttpServer`. These classes define basic HTTP request processing logic and set up routing.
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
### 1.3. Setting Up the Environment
To package into a binary file, it's necessary to install GraalVM and Maven. The document provides detailed installation steps, including download links, extraction instructions, and setting environment variables.
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

## 1.4.打包
The following describes how to use Maven to package a Java Jar file and how to build a binary image.

### Build Java Jar (Optional)

```shell
mvn package
```

### Build Binary Image

```shell
mvn clean package -DskipTests -Pserver-graalvm
```

#### Actual Packaging Command Executed
```
/root/program/graalvm-jdk-21.0.1+12.1/lib/svm/bin/native-image -cp /root/.m2/repository/com/litongjava/tio-http-server/3.7.3.v20231223-RELEASE/tio-http-server-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-http-common/3.7.3.v20231223-RELEASE/tio-http-common-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-core/3.7.3.v20231223-RELEASE/tio-core-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/litongjava/tio-utils/3.7.3.v20231223-RELEASE/tio-utils-3.7.3.v20231223-RELEASE.jar:/root/.m2/repository/com/alibaba/fastjson2/fastjson2/2.0.43/fastjson2-2.0.43.jar:/root/.m2/repository/org/slf4j/slf4j-jdk14/1.7.31/slf4j-jdk14-1.7.31.jar:/root/.m2/repository/org/slf4j/slf4j-api/1.7.31/slf4j-api-1.7.31.jar:/root/code/java-ee-tio-boot-study/tio-http-server-study/tio-http-server-hello/target/tio-http-server-graal.jar -H:+RemoveSaturatedTypeFlows --allow-incomplete-classpath --no-fallback -H:Class=demo.DemoHttpServer -H:Name=tio-http-server-hello
```
#### Excerpts from the Packaging Log
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

The generated binary file tio-http-server-hello is 42MB.

### 1.5. Startup Testing
Start the server. The startup time is only 13ms. The server startup log includes server configuration, startup time, process ID, and other information.
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