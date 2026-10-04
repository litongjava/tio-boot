package nexus.io.tio.utils.hutool;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtils {

  /**
   * Returns a random integer in the inclusive range, including integer extremes.
   *
   * @throws IllegalArgumentException if min is greater than max
   */
  public static int nextInt(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("min must not be greater than max");
    }
    return (int) ThreadLocalRandom.current().nextLong((long) min, (long) max + 1L);
  }
}
