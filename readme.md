# tio-boot
基于java aio的高性能web框架,可以让单台服务器承受上万并发  
依赖组件  
- tio-http-server
- jfinal
- logback-classic


版本问题
如果使用3.8.2.v20220628-RELEASE可能会出现下面的问题
```
    <dependency>
      <groupId>com.litongjava</groupId>
      <artifactId>tio-boot</artifactId>
      <version>3.8.2.v20220628-RELEASE</version>
    </dependency>
```


```
Exception in thread "main" java.lang.UnsupportedClassVersionError: org/tio/utils/jfinal/P has been compiled by a more recent version of the Java Runtime (class file version 61.0), this version of the Java Runtime only recognizes class file versions up to 52.0

```
