package nexus.io.tio.boot.token;

import java.util.Objects;

/** Verified request identity; realms keep user and administrator IDs distinct. */
public final class RequestIdentity implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  private final Object userId;
  private final String realm;
  private final Object tenantId;
  public RequestIdentity(Object userId, String realm, Object tenantId) {
    this.userId = Objects.requireNonNull(userId, "userId");
    this.realm = Objects.requireNonNull(realm, "realm");
    this.tenantId = tenantId;
  }
  public Object getUserId() { return userId; }
  public String getRealm() { return realm; }
  public Object getTenantId() { return tenantId; }
}
