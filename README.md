<div align="center">
<h1>HAProxyReduce</h1>
<p>✨ 一款为 Minecraft 服务器打造的代理兼容插件，同时支持 HAProxy 代理连接与直连连接 ✨</p>
<a href="https://github.com/Wuchang325/HAProxyReduce/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-LGPL3.0-green" alt="license"></a>
<a href="https://github.com/Wuchang325/HAProxyReduce/releases"><img src="https://img.shields.io/github/v/release/Wuchang325/HAProxyReduce" alt="release"></a>
<a href="#"><img src="https://img.shields.io/badge/Velocity-3.3-blue" alt="Velocity 3.3"></a>
<a href="#"><img src="https://img.shields.io/badge/Paper-1.20.1-green" alt="Paper 1.20.1"></a>
<a href="#"><img src="https://img.shields.io/badge/Folia-1.20.1-purple" alt="Folia 1.20.1"></a>
<a href="#"><img src="https://img.shields.io/badge/JDK-17-orange" alt="JDK 17"></a>
<a href="#"><img src="https://img.shields.io/badge/Kotlin-2.2.0-red" alt="Kotlin 2.2.0"></a>
</div>

## 介绍

本插件目的在于允许同时支持 HAProxy 代理连接与直连连接,可通过白名单设置允许启用 HAProxy 协议ip列表，支持ipv4/6双类型

本插件属于[HAProxyDetector](https://github.com/andylizi/haproxy-detector)插件的重写版，使用原仓库[LGPL-3.0](https://github.com/Wuchang325/HAProxyReduce#LGPL-3.0-1-ov-file)开源

贡献者：
- [Wuchang325](https://github.com/Wuchang325)

## 使用
从[Release](https://github.com/Wuchang325/HAProxyReduce/releases)下载最新版本，放入服务器的`plugins`文件夹中，重启服务器即可
paper/folia需要安装 ProtocolLib 5.1.0 或以上版本插件

## 配置
安装插件并重启后在`plugins/HAProxyReduce`文件夹并打开编辑`whitelist.conf`，配置ip以控制其ip是否允许启用HAProxy协议
### paper配置
打开根目录下`config\paper-global.yml`文件并修改`proxies`下`proxy-protocol`的值为`true`

`folia`配置方法类似

### velocity配置
打开根目录下`config\velocity.toml`文件并修改`proxy-protocol`的值为`true`

## 问题提交
在`issues`中提交问题

## 兼容性
本插件在`jdk17`,`kotlin 2.2.0`下编写，运行环境不低于`jdk17`,推荐在`jdk21`下运行

paper: 推荐在`paper 1.20.1+`(或其衍生端)运行，此插件已内置`folia`支持, 插件不支持`spigots`服务端

velocity: 推荐在`velocity 3.3+`(或其衍生端)运行
