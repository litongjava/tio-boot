package nexus.io.tio.utils.crypto;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Pbkdf2PasswordUtils {
  private static final int ITERATIONS = 600000;
  private Pbkdf2PasswordUtils() { }
  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }
  public static boolean matches(String password, String encoded) {
    try {
      String[] parts = encoded.split("\\$");
      int rounds = Integer.parseInt(parts[1]);
      if (!"pbkdf2".equals(parts[0]) || rounds < ITERATIONS || rounds > 1000000) { return false; }
      return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),
          derive(password, Base64.getDecoder().decode(parts[2]), rounds));
    } catch (RuntimeException error) { return false; }
  }
  private static byte[] derive(String password, byte[] salt, int rounds) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
    try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
    catch (GeneralSecurityException error) { throw new IllegalStateException(error); }
    finally { spec.clearPassword(); }
  }
}
