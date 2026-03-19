package com.litongjava.tio.utils.lang;

import org.junit.Test;

public class ChineseUtilsTest {

  @Test
  public void testContainsChinese() {
    System.out.println(ChineseUtils.containsChinese("去繁求减\r\n"
        + "追求极致简约、返璞归真。将复杂变简短，让简短更简短，轻装上阵从容应对。\r\n"
        + "\r\n"
        + "简单好用\r\n"
        + "封装简洁、上手容易。开发飞快、运行高效，性能表现出色，让你事半功倍。\r\n"
        + "\r\n"
        + "快速开发\r\n"
        + "学习门槛低，开发效率高，代码量更少却功能更强，让你用最少的代码完成更多的工作。\r\n"
        + "\r\n"
        + "高并发\r\n"
        + "高可用、高扩展、高性能，轻松应对大规模并发访问，稳定可靠表现卓越。\r\n"
        + "\r\n"
        + "节约时间\r\n"
        + "拥有 Java 的全部优势，同时具备类似 Ruby、Python 等动态语言的动态加载,无须重启,保存即用。为你节省更多宝贵时间，去陪伴挚爱、家人和朋友。\r\n"
        + "\r\n"
        + "一体化\r\n" + "内置 Controller、Handler、Cache、AOP、TCP.对 Database、MQ、AI 等多种关键服务 也内置了 支持，节省大量的技术选型工作。"));
  }
  
  @Test
  public void testContainsChinese2() {
    System.out.println(ChineseUtils.containsChinese("去繁求减"));
  }
}
