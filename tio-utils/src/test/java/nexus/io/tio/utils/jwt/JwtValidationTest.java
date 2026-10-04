package nexus.io.tio.utils.jwt;

import static org.testng.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.testng.annotations.Test;

public class JwtValidationTest {
  private static final String KEY = "test-only-signing-key";

  private String signed(String header, String payload) throws Exception {
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    String input = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
        + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return input + "." + encoder.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void malformedInputIsRejectedWithoutThrowing() {
    for (String token : new String[] {null, "", "a.b.c", "a.%.c", "...", "a.b"}) {
      assertFalse(JwtUtils.verify(KEY, token));
    }
  }

  @Test
  public void trailingSegmentsAreRejected() {
    String token = JwtUtils.createTokenByUserId(KEY, 42L);
    assertTrue(JwtUtils.verify(KEY, token));
    assertFalse(JwtUtils.verify(KEY, token + "."));
    assertFalse(JwtUtils.verify(KEY, token + ".."));
  }

  @Test
  public void missingOrInvalidExpirationIsRejectedWithoutThrowing() throws Exception {
    for (String payload : new String[] {"{}", "{\"exp\":null}", "{\"exp\":\"tomorrow\"}"}) {
      assertFalse(JwtUtils.verify(KEY, signed("{\"alg\":\"HS256\"}", payload)));
    }
  }

  @Test
  public void algorithmMustMatchVerifier() throws Exception {
    assertFalse(JwtUtils.verify(KEY, signed("{\"alg\":\"none\"}", "{\"exp\":-1}")));
    assertFalse(JwtUtils.verify(KEY, signed("{\"alg\":\"HS512\"}", "{\"exp\":-1}")));
  }

  @Test
  public void expirationBoundaryAndLegacyUnlimitedToken() {
    long now = System.currentTimeMillis() / 1000;
    assertFalse(JwtUtils.verify(KEY, JwtUtils.createTokenByUserId(KEY, 42L, now)));
    assertTrue(JwtUtils.verify(KEY, JwtUtils.createTokenByUserId(KEY, 42L, -1)));
    assertFalse(JwtUtils.verify("another-key", JwtUtils.createTokenByUserId(KEY, 42L, -1)));
  }

  @Test
  public void legacyColonDelimitedSignatureRemainsSupported() throws Exception {
    String token = JwtUtils.createTokenByUserId(KEY, 42L, -1);
    String[] parts = token.split("\\.");
    String input = parts[0] + ":" + parts[1];
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    assertTrue(JwtUtils.verify(KEY, input + ":" + signature, JwtUtils.colon_delimiter));
    assertFalse(JwtUtils.verify(KEY, input + ":" + signature + ":", JwtUtils.colon_delimiter));
  }
}
