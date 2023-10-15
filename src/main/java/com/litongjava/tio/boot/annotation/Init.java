package com.litongjava.tio.boot.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只启动,不放入bean中
 * @author 
 *
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Init {

  String value() default "";
  
  String destroyMethod() default "";

  String initMethod() default "";

}
