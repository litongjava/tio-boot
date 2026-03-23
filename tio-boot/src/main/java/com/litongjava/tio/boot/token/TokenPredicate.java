package com.litongjava.tio.boot.token;

@FunctionalInterface
public interface TokenPredicate {
  public PredicateResult validate(String token);
}