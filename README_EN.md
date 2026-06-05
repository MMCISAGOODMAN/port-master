# Port Master — Port & Process Management Tool

**English** | [简体中文](README.md)

A cross-platform B/S port and process management tool. Scan ports, manage processes, and monitor alerts — all from your browser, with no extra system software required.

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License"/>
</p>

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Permissions by OS](#permissions-by-os)
- [User Guide](#user-guide)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [FAQ](#faq)
- [Technical Notes](#technical-notes)
- [License](#license)

---

## Features

### Port Management

| Feature | Description |
|---------|-------------|
| Full Port Scan | One-click scan of all TCP/UDP ports with pagination, sorting, and global search |
| Multi-dimension Query | Single/batch port, port range (`8000-8100,9090`), process name lookup, PID lookup |
| Well-known Services | Auto-identify MySQL, Redis, Nginx, Nacos, and other common port services |
| Free Port Generator | Find consecutive free ports with 80xx / 30xx / 90xx templates |
| Port Probe | TCP socket connectivity test, supports batch probing |
| Conflict Detection | Detect multiple processes listening on the same port, with highlighting |
| Scan Diff | Compare with previous scan, highlight new/changed ports |
| Kill by Port | Release a port by terminating all processes bound to it |
| Quick Open | One-click browser open for HTTP ports (80/8080/3000 etc.) in LISTEN state |

### Process Management

| Feature | Description |
|---------|-------------|
| Process Detail | PID, path, command line, CPU/memory, create time, bound ports (2s auto-refresh) |
| Process Termination | Graceful stop / force kill, with batch operations |
| Process List | View all running processes with search, kill, and port lookup |

### Monitoring & Data

| Feature | Description |
|---------|-------------|
| Dashboard | CPU, memory, listen ports, active connections, process count |
| Port Monitor Alerts | Custom monitor list with polling and popup alerts on state changes |
| Data Export | Export filtered list to Excel / Markdown / TXT |
| Auto Refresh | Configurable 10/30/60 second scan interval (stored in LocalStorage) |
| Config Backup | Export/import groups, monitor, history, and settings as JSON |

### UI & Personalization

| Feature | Description |
|---------|-------------|
| Port Groups | Custom groups (Java, Database, Middleware, etc.) with sidebar quick filter |
| Table Filters | Filter by protocol, connection state, bind address (127.0.0.1 / all interfaces) |
| Operation History | Auto-save scan/query/kill history in LocalStorage |
| Common Ports | Built-in shortcuts for MySQL 3306, Redis 6379, Nginx 80, etc. |
| Theme Switch | Light / dark theme with toolbar toggle and persistent settings |
| One-click Copy | Copy port row data to clipboard |

### Planned Extension

| Feature | Description |
|---------|-------------|
| SSH Remote | Layered code reserved in `controller/remote` and `service/remote`, not yet implemented |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17, Lombok |
| Frontend | Vue 3, Vite 5, Element Plus, Axios, XLSX |
| Storage | No database; all user config in browser LocalStorage |
| Deployment | Single jar; frontend embeddable in `static/` for all-in-one deploy |
| Cross-platform | Windows / Linux / macOS via native system commands only |

### Native Commands by OS

| OS | Port Scan | Process Info | Kill Process |
|----|-----------|-------------|--------------|
| Windows | `netstat -ano` | `tasklist` + `wmic` | `taskkill /F` |
| Linux | `ss` + `lsof` | `ps` | `kill` / `kill -9` |
| macOS | `lsof` + `netstat` | `ps` | `kill` / `kill -9` |

> macOS has no `ss` command; the system automatically falls back to `lsof + netstat`.

---

## Project Structure

```
port-master/
├── README.md                          # Chinese documentation
├── README_EN.md                       # English documentation (this file)
├── .gitignore
├── backend/                           # Spring Boot backend
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/portmaster/
│       │   ├── PortMasterApplication.java
│       │   ├── config/                # CORS, static resources
│       │   ├── controller/            # REST endpoints
│       │   │   ├── PortController.java
│       │   │   ├── ProcessController.java
│       │   │   ├── SystemController.java
│       │   │   └── remote/            # SSH remote (reserved)
│       │   ├── service/               # Business logic
│       │   │   ├── PortService.java
│       │   │   ├── ProcessService.java
│       │   │   ├── PortProbeService.java
│       │   │   ├── SystemMonitorService.java
│       │   │   └── remote/
│       │   ├── model/dto/
│       │   ├── util/                  # Command execution & parsers
│       │   │   ├── CommandExecutor.java
│       │   │   ├── OsDetector.java
│       │   │   └── parser/
│       │   └── exception/
│       └── resources/
│           ├── application.yml
│           └── static/                # Frontend build output
└── frontend/                          # Vue 3 frontend
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.vue
        ├── api/index.js
        ├── components/
        ├── utils/
        └── styles/global.css
```

---

## Requirements

| Dependency | Version |
|-----------|---------|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 18+ (frontend dev/build only) |

---

## Quick Start

### Option 1: Separate Frontend & Backend (Development)

**1. Start backend**

```bash
cd backend
mvn spring-boot:run
```

Backend: `http://localhost:8080`

**2. Start frontend**

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server: `http://localhost:5173` (API proxied to backend)

### Option 2: All-in-one Deployment (Production)

```bash
# 1. Build frontend (output → backend/src/main/resources/static/)
cd frontend && npm install && npm run build

# 2. Package backend
cd ../backend && mvn clean package -DskipTests

# 3. Run
java -jar target/port-master-1.0.0.jar
```

Open `http://localhost:8080`.

### Platform-specific Examples

```powershell
# Windows — Admin PowerShell (required for killing processes)
cd backend\target
java -jar port-master-1.0.0.jar
```

```bash
# Linux / macOS — sudo recommended for full permissions
sudo java -jar port-master-1.0.0.jar

# Custom port
java -jar port-master-1.0.0.jar --server.port=9090
```

---

## Permissions by OS

| Action | Windows | Linux | macOS |
|--------|---------|-------|-------|
| View ports | Normal user | Normal user | Normal user |
| View other users' processes | Administrator | root / sudo | root / sudo |
| Kill processes | Administrator | root / sudo | root / sudo |
| Full port details | Administrator | root / sudo | Partially requires sudo |

When permissions are insufficient, a dismissible warning banner appears at the top of the page.

---

## User Guide

### Basics

1. **Full Scan** — Click "Full Scan" to list all TCP/UDP ports
2. **Search** — Search by port, PID, process name, or service name (e.g. `mysql`)
3. **Query** — Port field supports `8080` or `8000-8100,9090`; also query by process name / PID
4. **Filters** — Filter by protocol, state, bind address; check "Listen only"

### Process Operations

5. **Details** — Click a row for process details (CPU/memory refreshes every 2s)
6. **Kill** — "Stop" for graceful termination, "Force kill" for SIGKILL; batch supported
7. **Release Port** — Click "Release" on LISTEN rows to kill processes on that port
8. **Process List** — Toolbar "Process List" shows all running processes

### Advanced

9. **Groups** — Click "Favorite" to add ports to custom groups; filter via sidebar
10. **Free Ports** — "Free Ports" generator with one-click copy
11. **Probe** — "Connectivity Probe" or row-level "Probe" for TCP reachability test
12. **Conflicts** — "Conflict Detection" for multi-process port conflicts
13. **Scan Diff** — Scan twice to see "New" / "Changed" tags; filter with "Changes only"
14. **Monitor** — Configure ports to watch; 5s polling with popup alerts
15. **Export** — Export to Excel / Markdown / TXT
16. **Backup** — Export/import JSON config via "Backup" button
17. **Theme** — Moon/sun icon in toolbar, or Settings → Light/Dark

---

## API Reference

Base path: `/api`

### Ports

| Method | Path | Description |
|--------|------|-------------|
| GET | `/ports/scan` | Full port scan |
| GET | `/ports/query/{port}` | Single port query |
| GET | `/ports/query?ports=8080,9090` | Batch query (supports `8000-8100` range) |
| GET | `/ports/query/range?start=8000&end=8100` | Port range query |
| GET | `/ports/query/process?name=java` | Lookup by process name |
| GET | `/ports/query/pid/{pid}` | Lookup by PID |
| GET | `/ports/free?start=8080&count=5` | Generate free ports |
| GET | `/ports/conflicts` | Port conflict detection |
| GET | `/ports/summary` | Scan summary statistics |
| GET | `/ports/probe?port=8080&host=127.0.0.1` | TCP connectivity probe |
| POST | `/ports/probe/batch` | Batch TCP probe |
| POST | `/ports/monitor` | Port monitor check |

### Processes

| Method | Path | Description |
|--------|------|-------------|
| GET | `/process/list` | List all running processes |
| GET | `/process/{pid}` | Process detail |
| DELETE | `/process/{pid}` | Graceful stop |
| DELETE | `/process/{pid}/force` | Force kill |
| DELETE | `/process/by-port/{port}` | Release port (kill bound processes) |
| DELETE | `/process/by-port/{port}/force` | Force release port |
| POST | `/process/kill/batch` | Batch kill |

### System

| Method | Path | Description |
|--------|------|-------------|
| GET | `/system/stats` | System monitor stats |
| GET | `/system/info` | OS info and permission hints |

### Remote (Reserved)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/remote/scan` | Remote scan (501 not implemented) |
| POST | `/remote/kill` | Remote kill (501 not implemented) |

### Response Format

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

---

## Configuration

### Backend `application.yml`

```yaml
server:
  port: 8080          # Override with --server.port

portmaster:
  monitor:
    poll-interval-ms: 5000
```

### Frontend LocalStorage Keys

| Key | Content |
|-----|---------|
| `portmaster_groups` | Port group favorites |
| `portmaster_monitor` | Port monitor config |
| `portmaster_history` | Operation history |
| `portmaster_settings` | Theme, auto-refresh, pagination, etc. |

Use the "Backup" feature to export as JSON.

---

## FAQ

**Q: Scan returns empty or PID is blank?**

A: On macOS/Linux, run with `sudo` for complete info. On Windows, run as Administrator.

**Q: Kill process fails?**

A: Ensure the backend runs with Administrator (Windows) or sudo (Linux/macOS).

**Q: Frontend changes not reflected?**

A: In all-in-one mode, re-run `npm run build` and repackage the jar. Use `npm run dev` for hot reload during development.

**Q: Port 8080 already in use?**

A: Use a different port: `java -jar port-master-1.0.0.jar --server.port=9090`

**Q: Where is data stored?**

A: User config is in browser LocalStorage, never uploaded to the server. Clear browser data will erase config — use "Backup" regularly.

**Q: How to show only listening ports?**

A: Check "Listen only" in the filter bar, or enable "Default listen only" in Settings.

---

## Technical Notes

- All system info is collected via **native commands only** — no third-party tools required
- User data (groups, monitor, history, settings) is stored in browser **LocalStorage**, not on the server
- SSH remote management is reserved in `controller/remote` and `service/remote` for future JSch or Apache MINA SSHD integration
- Frontend build output goes to `backend/src/main/resources/static/` for single-jar deployment

---

## License

[MIT](LICENSE)
