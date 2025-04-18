package com.litongjava.tio.boot.druid;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
}
