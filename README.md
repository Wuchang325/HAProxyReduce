<div align="center">
  <h1>HAProxyReduce</h1>
  <p>一款 Minecraft 服务器插件，为 Velocity 与 Paper/Folia 提供 HAProxy 代理协议支持。</p>

  <a href="https://github.com/Wuchang325/HAProxyReduce/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-LGPL3.0-green" alt="license"></a>
  <a href="https://github.com/Wuchang325/HAProxyReduce/releases"><img src="https://img.shields.io/github/v/release/Wuchang325/HAProxyReduce" alt="release"></a>
  <a href="#"><img src="https://img.shields.io/badge/Velocity-3.5.0--SNAPSHOT-blue" alt="Velocity 3.5.0-SNAPSHOT"></a>
  <a href="#"><img src="https://img.shields.io/badge/Paper-1.20.1-green" alt="Paper 1.20.1"></a>
  <a href="#"><img src="https://img.shields.io/badge/Folia-1.20.1-purple" alt="Folia 1.20.1"></a>
  <a href="#"><img src="https://img.shields.io/badge/JDK-21-orange" alt="JDK 21"></a>
  <a href="#"><img src="https://img.shields.io/badge/Kotlin-2.2.0-red" alt="Kotlin 2.2.0"></a>
  <a href="#"><img src="https://img.shields.io/badge/bStats-3.0.2-blue" alt="bStats-3.0.2"></a>
</div>

## 简介

HAProxyReduce 处理 HAProxy 代理协议与直连连接的自动识别，让 Velocity 和 Paper/Folia 服务端都能正常获取真实客户端 IP。内置访问控制与配置热重载，也提供对外 API 供其他插件集成。

## 核心功能

- 支持 Velocity 与 Paper/Folia 双平台
- 自动识别 HAProxy 代理连接与直连
- 白名单 / 黑名单访问控制
- 配置热重载，改完即生效
- 对外 API，方便其他插件调用
- 连接日志记录与调试模式
- 底层基于 Netty，协议处理开销低

## 安装

1. 从 [Releases](https://github.com/Wuchang325/HAProxyReduce/releases) 下载对应平台的 JAR。
2. 放入服务端 `plugins/` 目录。
3. 重启服务端。

### 平台依赖

- **Paper / Folia**：建议安装 ProtocolLib 5.1.0+
- **Velocity**：无额外依赖

### 启用代理协议

**Paper / Folia**

编辑 `config/paper-global.yml`：

```yaml
proxies:
  proxy-protocol: true
```

**Velocity**

编辑 `velocity.toml`：

```toml
proxy-protocol = true
```

## 配置

首次启动后，在 `plugins/HAProxyReduce/` 生成 `config.yml`。

### 示例

```yaml
whitelist:
  mode: EMPTY_DENY_ALL
  ips:
    - "127.0.0.1"
    - "192.168.1.0/24"
    - "::1"

blacklist:
  enabled: false
  ips: []

connectionTracker:
  timeout: 300000
  cleanupInterval: 30

hotReload:
  enabled: true
  checkInterval: 10

logging:
  warnOnce: true
```

### 白名单模式

| 模式 | 行为 |
|------|------|
| `DISABLED` | 关闭白名单，只做 HAProxy 协议检测 |
| `EMPTY_ALLOW_ALL` | 白名单为空时放行所有；非空时仅放行列表内 IP |
| `EMPTY_DENY_ALL` | 白名单为空时拒绝所有；非空时仅放行列表内 IP |

### 黑名单

- 启用后，黑名单 IP 跳过 HAProxy 协议解析
- 黑名单优先级高于白名单

### 热重载

开启后自动监听配置变更，改完保存即生效，无需重启。

## 命令

需 OP 权限：

| 命令 | 说明 |
|------|------|
| `/haproxyreload` | 手动重载配置 |
| `/haproxystatus` | 查看状态与统计 |

## 兼容性

- **JDK**：21+
- **Paper / Folia**：1.20.1+
- **Velocity**：3.5.0-SNAPSHOT 或兼容版本

不支持 Spigot、CraftBukkit 等服务端。

## 问题反馈

提交 Issue 时建议附上：

- 服务端平台与版本
- 插件版本
- 完整错误日志
- 配置文件内容
- 重现步骤

## bStats 统计

Paper / Folia 平台：
![bStats Bukkit](https://bstats.org/signatures/bukkit/HAProxyReduce.svg)

Velocity 平台：
![bStats Velocity](https://bstats.org/signatures/velocity/HAProxyReduce.svg)

## 星星

[![Stargazers over time](https://starchart.cc/Wuchang325/HAProxyReduce.svg?variant=adaptive)](https://starchart.cc/Wuchang325/HAProxyReduce)
