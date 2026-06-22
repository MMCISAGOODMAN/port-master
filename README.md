# Port Master — 端口 & 进程管理工具

[English](README_EN.md) | **简体中文**

跨平台 B/S 架构的端口与进程管理工具，前后端分离，开源免费。通过浏览器即可扫描本机端口、管理进程、监控告警，无需安装额外系统软件。

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/Version-2.1.0-blue" alt="Version 2.1"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License"/>
</p>

> **v2.1 更新亮点**：WebSocket 后台监控告警 · Kubernetes Pod/Service 端口 · 中英文界面切换

> **v2.0 更新亮点**：SSH 远程管理 · Docker 容器端口 · HTTP/TLS 探测 · 扫描历史趋势 · 网络接口 · 释端口 · 扫描缓存优化

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [各系统权限说明](#各系统权限说明)
- [使用教程](#使用教程)
- [API 接口](#api-接口)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [技术说明](#技术说明)
- [License](#license)

---

## 功能特性

### 端口管理

| 功能 | 说明 |
|------|------|
| 全量端口扫描 | 一键扫描本机全部 TCP/UDP 端口，支持分页、排序、全局模糊搜索 |
| 多维度查询 | 单端口 / 批量端口 / 端口段（`8000-8100,9090`）/ 进程名反查 / PID 反查 |
| 知名服务名 | 自动识别 MySQL、Redis、Nginx、Nacos 等常见端口服务 |
| 空闲端口生成 | 连续空闲端口检测，内置 80xx / 30xx / 90xx 模板，一键复制 |
| 端口连通探测 | TCP Socket 探测端口是否可达，支持批量探测 |
| 端口冲突检测 | 检测同一端口被多个进程监听，表格高亮 + 独立弹窗 |
| 扫描对比 | 与上次扫描结果对比，高亮新增 / 变化端口 |
| 按端口杀进程 | 一键释放指定端口（结束占用该端口的所有进程），表格「释端口」按钮 |
| 快速打开 | HTTP 类端口（80/8080/3000 等）LISTEN 状态可一键浏览器打开 |
| 扫描缓存 | 后端可配置 TTL 缓存，减少重复全量扫描开销 |
| HTTP/TLS 探测 | HTTP 健康检查 + TLS 证书过期天数检测 |

### 远程 & 容器

| 功能 | 说明 |
|------|------|
| SSH 远程管理 | 连接远程 Linux/macOS/Windows，扫描端口、远程杀进程，支持密码/私钥 |
| Docker 容器 | 列出容器端口映射，一键查端口、停止/重启容器 |
| Kubernetes | 通过 kubectl 查看 Pod/Service 端口映射，一键跳转本地端口查询 |
| 网络接口 | 查看本机网卡 IP/MAC/状态，一键复制 IP |

### 数据 & 趋势

| 功能 | 说明 |
|------|------|
| 扫描历史 | 每次全量扫描自动记录快照，对比监听端口/连接数变化趋势 |
| 远程主机收藏 | 保存常用 SSH 主机（不含密码），快速切换 |
| WebSocket 告警 | 启用监控后服务端后台轮询，WebSocket 推送告警（标签页在后台也能收到） |

### 进程管理

| 功能 | 说明 |
|------|------|
| 进程详情 | PID、程序路径、启动命令、CPU/内存、创建时间、绑定端口，2s 自动刷新 |
| 进程终止 | 正常结束 / 强制杀死，支持表格批量操作 |
| 进程列表 | 查看全部运行进程，支持搜索、杀进程、反查端口 |

### 监控 & 数据

| 功能 | 说明 |
|------|------|
| 首页大盘 | CPU、内存、监听端口数、活跃连接数、运行进程数 |
| 端口监控告警 | 自定义监控列表，后端定时轮询，异常占用/释放弹窗告警 |
| 数据导出 | 当前筛选列表导出 Excel / Markdown / TXT |
| 自动刷新 | 可配置 10 / 30 / 60 秒定时扫描（设置存 LocalStorage） |
| 配置备份 | 导出 / 导入分组、监控、历史、设置为 JSON 文件 |

### 界面 & 个性化

| 功能 | 说明 |
|------|------|
| 端口分组收藏 | 自定义分组（Java 服务、数据库、中间件等），侧边栏快速筛选 |
| 表格筛选 | 按协议、连接状态、绑定地址（127.0.0.1 / 全接口）筛选 |
| 操作历史 | 自动记录扫描 / 查询 / 杀进程操作，本地存储快速回看 |
| 常用端口库 | 内置 MySQL 3306、Redis 6379、Nginx 80 等快捷检索 |
| 主题切换 | 浅色 / 深色主题，工具栏一键切换，设置持久化 |
| 一键复制 | 表格行端口信息一键复制到剪贴板 |
| v2 工具栏 | 远程 SSH、Docker、K8s、扫描历史、网络接口等入口 |
| 多语言 | 简体中文 / English 界面切换（设置 → 界面语言） |

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2、Java 17、Lombok |
| 前端 | Vue 3、Vite 5、Element Plus、Axios、XLSX |
| 存储 | 无数据库，用户配置全部存浏览器 LocalStorage |
| 部署 | 单 jar 包运行，前端可独立开发或嵌入 static 一体部署 |
| 跨平台 | Windows / Linux / macOS，仅调用系统原生命令 |

### 各平台系统命令

| 系统 | 端口扫描 | 进程信息 | 杀进程 |
|------|---------|---------|--------|
| Windows | `netstat -ano` | `tasklist` + `wmic` | `taskkill /F` |
| Linux | `ss` + `lsof` | `ps` | `kill` / `kill -9` |
| macOS | `lsof` + `netstat` | `ps` | `kill` / `kill -9` |

> macOS 无 `ss` 命令，系统自动回退到 `lsof + netstat` 方案。

---

## 项目结构

```
port-master/
├── README.md                          # 中文文档（本文件）
├── README_EN.md                       # English documentation
├── .gitignore
├── backend/                           # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/portmaster/
│       │   ├── PortMasterApplication.java
│       │   ├── config/                # 跨域、静态资源
│       │   ├── controller/            # REST 接口
│       │   │   ├── PortController.java
│       │   │   ├── ProcessController.java
│       │   │   ├── SystemController.java
│       │   │   └── remote/            # SSH 远程（预留）
│       │   ├── service/               # 业务逻辑
│       │   │   ├── PortService.java
│       │   │   ├── ProcessService.java
│       │   │   ├── PortProbeService.java
│       │   │   ├── SystemMonitorService.java
│       │   │   └── remote/
│       │   ├── model/dto/             # 数据传输对象
│       │   ├── util/                  # 命令执行 & 端口解析
│       │   │   ├── CommandExecutor.java
│       │   │   ├── OsDetector.java
│       │   │   └── parser/
│       │   └── exception/
│       └── resources/
│           ├── application.yml
│           └── static/                # 前端构建产物（一体部署）
└── frontend/                          # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.vue                    # 主布局
        ├── api/index.js
        ├── components/
        │   ├── DashboardStats.vue     # 顶部仪表盘
        │   ├── PortTable.vue          # 端口数据表格
        │   ├── PortStatsBar.vue       # 统计标签栏
        │   ├── ProcessDetailDialog.vue
        │   ├── ProcessListDialog.vue
        │   ├── FreePortDialog.vue
        │   ├── MonitorDialog.vue
        │   ├── GroupManageDialog.vue
        │   ├── ConflictDialog.vue
        │   ├── PortProbeDialog.vue
        │   ├── ConfigBackupDialog.vue
        │   └── SettingsDialog.vue
        ├── utils/
        │   ├── storage.js             # LocalStorage
        │   ├── export.js              # 数据导出
        │   ├── theme.js               # 主题切换
        │   ├── portServices.js        # 知名端口映射
        │   ├── scanDiff.js            # 扫描对比
        │   └── configBackup.js        # 配置备份
        └── styles/global.css
```

---

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 18+（仅前端开发/构建时需要） |

---

## 快速开始

### 方式一：前后端分离开发

**1. 启动后端**

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`

**2. 启动前端**

```bash
cd frontend
npm install
npm run dev
```

前端开发地址：`http://localhost:5173`（已配置 API 代理到后端）

### 方式二：一体部署（推荐生产环境）

```bash
# 1. 构建前端（产物自动输出到 backend/src/main/resources/static/）
cd frontend && npm install && npm run build

# 2. 打包后端
cd ../backend && mvn clean package -DskipTests

# 3. 运行
java -jar target/port-master-1.0.0.jar
```

访问 `http://localhost:8080` 即可。

### 各平台启动示例

```powershell
# Windows — 管理员 PowerShell（杀进程需要）
cd backend\target
java -jar port-master-1.0.0.jar
```

```bash
# Linux / macOS — 建议 sudo（完整权限）
sudo java -jar port-master-1.0.0.jar

# 自定义端口
java -jar port-master-1.0.0.jar --server.port=9090
```

---

## 各系统权限说明

| 操作 | Windows | Linux | macOS |
|------|---------|-------|-------|
| 查看端口 | 普通用户 | 普通用户 | 普通用户 |
| 查看其他用户进程 | 管理员 | root / sudo | root / sudo |
| 终止进程 | 管理员 | root / sudo | root / sudo |
| 完整端口信息 | 管理员 | root / sudo | 部分需 sudo |

权限不足时，页面顶部会显示黄色提示条（可关闭），部分 PID / 路径信息可能无法获取。

---

## 使用教程

### 基础操作

1. **全量扫描** — 点击「全量扫描」，获取本机所有 TCP/UDP 端口
2. **搜索过滤** — 搜索框支持端口号、PID、进程名、服务名（如 `mysql`）
3. **精确查询** — 端口框支持 `8080` 或 `8000-8100,9090`；也可按进程名 / PID 查询
4. **表格筛选** — 按协议、连接状态、绑定地址筛选；勾选「仅监听端口」

### 进程操作

5. **查看详情** — 点击表格行，弹出进程详情（CPU/内存 2s 刷新）
6. **终止进程** — 「结束」正常终止，「强杀」强制杀死；支持批量勾选
7. **释放端口** — LISTEN 状态行点击「释端口」，结束占用该端口的进程
8. **进程列表** — 工具栏「进程列表」查看全部运行进程

### 高级功能

9. **分组收藏** — 点击「收藏」添加端口到自定义分组，左侧菜单快速筛选
10. **空闲端口** — 「空闲端口」生成器，输入起始端口和数量，一键复制
11. **连通探测** — 「连通探测」或行内「探测」，TCP 检测端口是否可达
12. **冲突检测** — 「冲突检测」查看多进程监听同一端口的情况
13. **扫描对比** — 连续扫描两次，表格标记「新」「变」，可勾选「仅看变化」
14. **端口监控** — 配置监控端口，启用后 5s 轮询，异常时右上角弹窗告警
15. **数据导出** — 「导出」下拉选择 Excel / Markdown / TXT
16. **配置备份** — 「备份」导出 JSON 配置，换环境后导入恢复
17. **主题切换** — 工具栏月亮/太阳图标，或「设置」中选择浅色/深色

---

## API 接口

基础路径：`/api`

### 端口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ports/scan?refresh=true` | 全量端口扫描（refresh 强制跳过缓存） |
| GET | `/ports/query/{port}` | 单端口查询 |
| GET | `/ports/query?ports=8080,9090` | 批量查询（支持 `8000-8100` 范围） |
| GET | `/ports/query/range?start=8000&end=8100` | 端口范围查询 |
| GET | `/ports/query/process?name=java` | 进程名反查 |
| GET | `/ports/query/pid/{pid}` | PID 反查 |
| GET | `/ports/free?start=8080&count=5` | 空闲端口生成 |
| GET | `/ports/conflicts` | 端口冲突检测 |
| GET | `/ports/summary` | 扫描汇总统计 |
| GET | `/ports/probe?port=8080&host=127.0.0.1` | TCP 连通探测 |
| POST | `/ports/probe/batch` | 批量 TCP 探测 |
| GET | `/ports/probe/http?port=8080&path=/` | HTTP 健康探测 |
| GET | `/ports/probe/tls?port=443` | TLS 证书探测 |
| POST | `/ports/monitor` | 端口监控探测 |

### 进程

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/process/list` | 列出所有运行进程 |
| GET | `/process/{pid}` | 进程详情 |
| DELETE | `/process/{pid}` | 正常结束进程 |
| DELETE | `/process/{pid}/force` | 强制杀死进程 |
| DELETE | `/process/by-port/{port}` | 按端口释放进程 |
| DELETE | `/process/by-port/{port}/force` | 按端口强制杀进程 |
| POST | `/process/kill/batch` | 批量杀进程 |

### 系统

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/stats` | 系统监控统计 |
| GET | `/system/info` | 系统信息与权限提示 |
| GET | `/system/config` | 服务端配置（轮询间隔、缓存 TTL、版本号） |

### 监控 WebSocket

| 类型 | 路径 | 说明 |
|------|------|------|
| WebSocket | `/ws/monitor` | 订阅监控告警推送（JSON `{type:"alert", alerts:[...]}`） |
| POST | `/monitor/config` | 同步监控配置到服务端（供后台轮询） |
| GET | `/monitor/status` | 监控运行状态 |

### Kubernetes

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/k8s/available` | kubectl 是否可用 |
| GET | `/k8s/context` | 当前 kubectl context |
| GET | `/k8s/pods?namespace=` | Pod 列表及容器端口 |
| GET | `/k8s/services?namespace=` | Service 列表及端口映射 |
| GET | `/k8s/summary?namespace=` | K8s 资源汇总 |

### Docker

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/docker/available` | Docker 是否可用 |
| GET | `/docker/containers?all=false` | 容器列表及端口映射 |
| POST | `/docker/stop` | 停止容器 |
| POST | `/docker/restart` | 重启容器 |

### 网络

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/network/interfaces` | 本机网络接口列表 |

### 远程 SSH

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/remote/test` | 测试 SSH 连接 |
| POST | `/remote/info` | 获取远程系统信息 |
| POST | `/remote/scan` | 远程端口扫描 |
| POST | `/remote/kill` | 远程杀进程 |

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

---

## 配置说明

### 后端 `application.yml`

```yaml
server:
  port: 8086          # 服务端口，可通过 --server.port 覆盖

portmaster:
  monitor:
    poll-interval-ms: 5000   # 监控轮询间隔，前端自动读取
  scan:
    cache-ttl-ms: 3000       # 扫描结果缓存 TTL，0=不缓存
  ssh:
    connect-timeout-ms: 10000
    command-timeout-sec: 60
```

### 前端 LocalStorage 键

| 键名 | 内容 |
|------|------|
| `portmaster_groups` | 端口分组收藏 |
| `portmaster_monitor` | 端口监控配置 |
| `portmaster_history` | 操作历史 |
| `portmaster_settings` | 主题、语言、自动刷新、分页等设置 |
| `portmaster_remote_hosts` | SSH 主机收藏（不含密码） |
| `portmaster_scan_history` | 扫描历史快照 |

可通过「备份」功能导出为 JSON 文件。

---

## 常见问题

**Q: 扫描结果为空或 PID 显示为空？**

A: macOS / Linux 下部分端口需要 `sudo` 启动 jar 才能获取完整信息。Windows 需管理员权限。

**Q: 杀进程失败？**

A: 请确认以管理员（Windows）或 sudo（Linux/macOS）方式启动后端。

**Q: 前端修改后页面没更新？**

A: 一体部署模式需重新 `npm run build` 并打包 jar。开发模式直接 `npm run dev` 即可热更新。

**Q: 端口 8080 被占用？**

A: 启动时指定其他端口：`java -jar port-master-1.0.0.jar --server.port=9090`

**Q: 数据存在哪里？**

A: 用户配置存在浏览器 LocalStorage，不上传服务器。清除浏览器数据会丢失配置，建议定期「备份」导出。

**Q: 如何只查看监听端口？**

A: 筛选栏勾选「仅监听端口」，或在设置中开启「默认仅监听」。

---

## 技术说明

- 所有系统信息采集均通过原生命令实现，**不依赖第三方系统软件**
- 用户数据（分组、监控、历史、设置）存储在浏览器 **LocalStorage**，不上传服务器
- SSH 远程管理已在 `controller/remote` 和 `service/remote` 分层预留，后续可接入 JSch 或 Apache MINA SSHD
- 前端构建产物输出至 `backend/src/main/resources/static/`，支持单 jar 部署

---

## License

[MIT](LICENSE)
