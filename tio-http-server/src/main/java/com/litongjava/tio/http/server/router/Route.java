package com.litongjava.tio.http.server.router;

import java.util.regex.Pattern;

import com.litongjava.tio.http.server.handler.HttpRequestHandler;

/** 路由结构：分段匹配；变量段可带约束正则；支持末尾可选段 */
public class Route {
  final String template;
  final HttpRequestHandler handler;

  final String[] segLiterals; // 非变量段的字面值；变量段为 null
  final String[] varNames; // 变量名；非变量段为 null
  final Pattern[] varPatterns; // 变量段正则；未约束为 null
  final boolean[] optionalMask; // 段是否可选（仅允许尾部连续可选）
  final int segmentsCount; // 总段数（含可选）
  final int requiredSegments; // 必需段数（从头开始的连续必需段）
  final String firstLiteral; // 首个静态段值（若存在）
  final int firstLiteralIndex; // 首个静态段下标（-1 表示不存在）

  Route(String template, HttpRequestHandler handler, String[] segLiterals, String[] varNames, Pattern[] varPatterns,
      boolean[] optionalMask, int segmentsCount, int requiredSegments, String firstLiteral, int firstLiteralIndex) {
    this.template = template;
    this.handler = handler;
    this.segLiterals = segLiterals;
    this.varNames = varNames;
    this.varPatterns = varPatterns;
    this.optionalMask = optionalMask;
    this.segmentsCount = segmentsCount;
    this.requiredSegments = requiredSegments;
    this.firstLiteral = firstLiteral;
    this.firstLiteralIndex = firstLiteralIndex;
  }
}