# tio-utils

`tio-utils` 是 `tio-boot` 生态中的基础工具模块，提供高复用、低依赖的通用工具能力，帮助开发者减少重复代码，提升开发效率，并保持项目结构的整洁与一致性。

该模块遵循 **轻量、稳定、实用** 的设计原则，适用于各类后端服务与基础设施组件。

---

## 特性

* 精简设计：避免臃肿依赖，专注核心工具能力
* 高复用性：可在多个服务间共享
* 易扩展：支持按需增加工具类
* 生产可用：面向真实业务场景设计

---

## 模块结构示例

```
tio-utils
├── date        // 时间处理工具
├── string      // 字符串工具
├── collection  // 集合相关工具
├── crypto      // 加密与安全工具
├── json        // JSON 辅助工具
├── net         // 网络相关工具
└── common      // 通用基础能力
```

> 实际结构可根据项目演进调整。

---

## 安装

### Maven

```xml
<dependency>
    <groupId>your.group.id</groupId>
    <artifactId>tio-utils</artifactId>
    <version>latest</version>
</dependency>
```

### Gradle

```gradle
implementation "your.group.id:tio-utils:latest"
```

---

## 快速开始

示例：

```java

public class Demo {
    public static void main(String[] args) {
        boolean empty = StrUtil.isEmpty("");
        System.out.println(empty);
    }
}
```

建议优先使用本模块中的工具方法，而不是在业务代码中重复实现。

---

## 使用建议

### 1. 保持工具“工具化”

工具类应：

* 无业务语义
* 无状态
* 可复用
* 线程安全

避免将业务逻辑混入 `tio-utils`。

---

### 2. 优先静态方法

推荐使用：

```
public final class XxxUtils {
    private XxxUtils() {}
}
```

防止误实例化。

---

### 3. 控制依赖

新增工具前请评估：

* 是否已有成熟实现
* 是否值得引入新依赖
* 是否会增加模块复杂度

`tio-utils` 应尽可能保持为 **基础层模块**。

---

## 版本策略

建议遵循：

```
MAJOR.MINOR.PATCH
```

* **MAJOR**：不兼容变更
* **MINOR**：向后兼容的功能新增
* **PATCH**：问题修复

---

## 贡献指南

欢迎提交：

* 新工具类
* 性能优化
* Bug 修复
* 文档改进

提交前建议：

1. 保证代码风格一致
2. 添加必要的单元测试
3. 更新相关文档

---

## 适用场景

`tio-utils` 适合作为：

* 微服务基础工具库
* 企业内部通用组件
* 中台基础模块
* 脚手架依赖库

---

## License

请根据你的项目选择合适的开源协议，例如：

```
Apache License 2.0
MIT
```