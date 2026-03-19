package com.litongjava.tio.utils.dsn;

import com.litongjava.model.dsn.JdbcInfo;

public class DbDSNParser {

  public JdbcInfo parse(String dsn) {
    int userStart = dsn.indexOf("//") + 2;
    int userEnd = dsn.indexOf(":", userStart);
    String user = dsn.substring(userStart, userEnd);

    int passStart = userEnd + 1;
    int passEnd = dsn.indexOf("@");
    String pswd = dsn.substring(passStart, passEnd);

    String url = "jdbc:" + dsn.substring(0, dsn.indexOf("//")) + "//" + dsn.substring(passEnd + 1);

    return new JdbcInfo(url, user, pswd);
  }

}
