package com.litongjava.tio.boot.token;

public class PredicateResult {
  private boolean ok;
  private String userId;

  public PredicateResult() {
  }

  public PredicateResult(boolean ok) {
    this.ok = ok;
  }

  public PredicateResult(boolean ok, String userId) {
    this.ok = ok;
    this.userId = userId;
  }

  public boolean isOk() {
    return ok;
  }

  public void setOk(boolean ok) {
    this.ok = ok;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

}
