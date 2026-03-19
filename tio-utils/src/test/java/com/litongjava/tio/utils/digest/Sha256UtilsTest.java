package com.litongjava.tio.utils.digest;

import org.junit.Test;

public class Sha256UtilsTest {

  @Test
  public void test() {
    String myPassword = "user_password123";
    String wrongPassword = "wrong_password";

    System.out.println("迭代次数: " + Sha256Utils.ITERATIONS);

    // 1. 哈希密码
    System.out.println("\n正在哈希密码，这可能需要一点时间...");
    long startTime = System.currentTimeMillis();
    String hashedPassword = Sha256Utils.hashPassword(myPassword);
    long endTime = System.currentTimeMillis();
    System.out.println("哈希完成，耗时: " + (endTime - startTime) + " ms");

    System.out.println("原始密码: " + myPassword);
    System.out.println("哈希后的密码 (可存入数据库): " + hashedPassword);

    System.out.println("\n--- 验证密码 ---");

    // 2. 验证正确的密码
    boolean isCorrect = Sha256Utils.checkPassword(myPassword, hashedPassword);
    System.out.println("使用正确密码 '" + myPassword + "' 进行验证: " + (isCorrect ? "成功" : "失败"));

    // 3. 验证错误的密码
    boolean isWrong = Sha256Utils.checkPassword(wrongPassword, hashedPassword);
    System.out.println("使用错误密码 '" + wrongPassword + "' 进行验证: " + (isWrong ? "成功" : "失败"));

    // 4. 验证一个格式错误的哈希
    boolean isInvalid = Sha256Utils.checkPassword(myPassword, "invalid-hash-format");
    System.out.println("使用格式错误的哈希进行验证: " + (isInvalid ? "成功" : "失败"));
  }

}
