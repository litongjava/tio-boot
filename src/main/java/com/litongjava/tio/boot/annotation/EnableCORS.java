package com.litongjava.tio.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Michael Yang 杨福海 （fuhai999@gmail.com）
 * 
 * 每个参数意义的详情 : https://developer.mozilla.org/en-US/docs/Glossary/CORS
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface EnableCORS {

  String allowOrigin() default "*";

  String allowCredentials() default "true";

  String allowHeaders() default "Origin,X-Requested-With,Content-Type,Accept,Authorization,Jwt";

  String allowMethods() default "GET,PUT,POST,DELETE,PATCH,OPTIONS";

  String exposeHeaders() default "";

  String requestHeaders() default "";

  String requestMethod() default "";

  String origin() default "";

  String maxAge() default "3600";
}
