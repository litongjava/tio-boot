package com.litongjava.tio.http.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tanyaowu 
 * 2018年7月1日 上午9:51:04
 */
public class HeaderName implements Serializable {
  private static final long serialVersionUID = 312702602569090773L;
  private static final Map<String, HeaderName> map = new HashMap<>();
  public static final HeaderName SET_COOKIE = new HeaderName(ResponseHeaderKey.Set_Cookie);
  public static final HeaderName CONTENT_TYPE = new HeaderName(ResponseHeaderKey.Content_Type);
  public static final HeaderName CACHE_CONTROL = new HeaderName(ResponseHeaderKey.Cache_Control);
  public static final HeaderName LOCATION = new HeaderName(ResponseHeaderKey.Location);
  public static final HeaderName Connection = new HeaderName(ResponseHeaderKey.Connection);
  public static final HeaderName Keep_Alive = new HeaderName(ResponseHeaderKey.Keep_Alive);
  public static final HeaderName Content_Length = new HeaderName(ResponseHeaderKey.Content_Length);
  public static final HeaderName Access_Control_Allow_Origin = new HeaderName(ResponseHeaderKey.Access_Control_Allow_Origin);
  public static final HeaderName Access_Control_Allow_Headers = new HeaderName(ResponseHeaderKey.Access_Control_Allow_Headers);
  public static final HeaderName Access_Control_Allow_Methods = new HeaderName(ResponseHeaderKey.Access_Control_Allow_Methods);
  public static final HeaderName Access_Control_Max_Age = new HeaderName(ResponseHeaderKey.Access_Control_Max_Age);

  public static final HeaderName Content_Disposition = new HeaderName(ResponseHeaderKey.Content_Disposition);
  public static final HeaderName Transfer_Encoding = new HeaderName(ResponseHeaderKey.Transfer_Encoding);
  public static final HeaderName Content_Encoding = new HeaderName(ResponseHeaderKey.Content_Encoding);
  public static final HeaderName Date = new HeaderName(ResponseHeaderKey.Date);
  public static final HeaderName Expires = new HeaderName(ResponseHeaderKey.Expires);
  public static final HeaderName Last_Modified = new HeaderName(ResponseHeaderKey.Last_Modified);
  public static final HeaderName Refresh = new HeaderName(ResponseHeaderKey.Refresh);
  public static final HeaderName Sec_WebSocket_Accept = new HeaderName(ResponseHeaderKey.Sec_WebSocket_Accept);
  public static final HeaderName Server = new HeaderName(ResponseHeaderKey.Server);
  public static final HeaderName Upgrade = new HeaderName(ResponseHeaderKey.Upgrade);
  public static final HeaderName Content_Type = new HeaderName(ResponseHeaderKey.Content_Type);
  public static final HeaderName Location = new HeaderName(ResponseHeaderKey.Location);
  public static final HeaderName Cache_Control = new HeaderName(ResponseHeaderKey.Cache_Control);
  public static final HeaderName tio_from_cache = new HeaderName(ResponseHeaderKey.tio_from_cache);
  public static final HeaderName tio_webpack_used_cache = new HeaderName(ResponseHeaderKey.tio_webpack_used_cache);
  public static final HeaderName Access_Control_Allow_Credentials = new HeaderName(ResponseHeaderKey.Access_Control_Allow_Credentials);
  public static final HeaderName Vary = new HeaderName(ResponseHeaderKey.vary);
  public static final HeaderName Allow = new HeaderName(ResponseHeaderKey.allow);
  public static final HeaderName Origin = new HeaderName(ResponseHeaderKey.origin);
  public static final HeaderName X_Content_Type_Options = new HeaderName(ResponseHeaderKey.x_content_type_options);
  public static final HeaderName Referrer_Policy = new HeaderName(ResponseHeaderKey.referrer_policy);;
  public static final HeaderName Cross_Origin_Opener_Policy = new HeaderName(ResponseHeaderKey.cross_origin_opener_policy);;

  public final String name;

  public final byte[] bytes;

  private HeaderName(String name) {
    this.name = name;
    this.bytes = name.getBytes();
    map.put(name, this);
  }

  public static HeaderName from(String name) {
    HeaderName ret = map.get(name);
    if (ret == null) {
      synchronized (map) {
        ret = map.get(name);
        if (ret == null) {
          ret = new HeaderName(name);
        }
      }
    }
    return ret;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    HeaderName other = (HeaderName) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return name;
  }
}
