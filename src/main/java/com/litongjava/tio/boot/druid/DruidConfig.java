package com.litongjava.tio.boot.druid;

import java.util.ArrayList;
import java.util.List;

public class DruidConfig {
  private String loginUsername;
  private String loginPassword;
  private boolean resetEnable;
  private String jmxUrl;
  private String jmxUsername;
  private String jmxPassword;
  private List<String> allowIps = new ArrayList<>();
  private List<String> denyIps = new ArrayList<>();
  private boolean removeAdvertise = true;
  public DruidConfig() {
    super();
    // TODO Auto-generated constructor stub
  }
  public DruidConfig(String loginUsername, String loginPassword, boolean resetEnable, String jmxUrl, String jmxUsername,
      String jmxPassword, List<String> allowIps, List<String> denyIps, boolean removeAdvertise) {
    super();
    this.loginUsername = loginUsername;
    this.loginPassword = loginPassword;
    this.resetEnable = resetEnable;
    this.jmxUrl = jmxUrl;
    this.jmxUsername = jmxUsername;
    this.jmxPassword = jmxPassword;
    this.allowIps = allowIps;
    this.denyIps = denyIps;
    this.removeAdvertise = removeAdvertise;
  }
  public String getLoginUsername() {
    return loginUsername;
  }
  public void setLoginUsername(String loginUsername) {
    this.loginUsername = loginUsername;
  }
  public String getLoginPassword() {
    return loginPassword;
  }
  public void setLoginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
  }
  public boolean isResetEnable() {
    return resetEnable;
  }
  public void setResetEnable(boolean resetEnable) {
    this.resetEnable = resetEnable;
  }
  public String getJmxUrl() {
    return jmxUrl;
  }
  public void setJmxUrl(String jmxUrl) {
    this.jmxUrl = jmxUrl;
  }
  public String getJmxUsername() {
    return jmxUsername;
  }
  public void setJmxUsername(String jmxUsername) {
    this.jmxUsername = jmxUsername;
  }
  public String getJmxPassword() {
    return jmxPassword;
  }
  public void setJmxPassword(String jmxPassword) {
    this.jmxPassword = jmxPassword;
  }
  public List<String> getAllowIps() {
    return allowIps;
  }
  public void setAllowIps(List<String> allowIps) {
    this.allowIps = allowIps;
  }
  public List<String> getDenyIps() {
    return denyIps;
  }
  public void setDenyIps(List<String> denyIps) {
    this.denyIps = denyIps;
  }
  public boolean isRemoveAdvertise() {
    return removeAdvertise;
  }
  public void setRemoveAdvertise(boolean removeAdvertise) {
    this.removeAdvertise = removeAdvertise;
  }
}
