# VPN Anomaly Detection — Spring Boot Backend

REST API backend for the VPN Anomaly Detection system. Manages VPN service control, network traffic captures, ML model orchestration, and real-time client monitoring. Designed to run on the same Linux server as the OpenVPN daemon.

---

## Architecture Overview

```
Angular Frontend
      │
      ▼
Spring Boot API  ──►  OpenVPN (systemctl)
      │           ──►  Python ML Pipeline (Controller.py / train_models.py)
      │           ──►  Wireshark CSV files (daily/)
      │           ──►  openvpn-status.log
      ▼
  MySQL / JPA
```

## API Reference

### VPN Control — `/api/vpn`

Manages the OpenVPN service lifecycle via `systemctl`. Uses a command whitelist to prevent arbitrary code execution.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/vpn/execute?command={action}` | Execute a VPN control action |

Available actions: `start`, `stop`, `restart`, `status`

```bash
POST /api/vpn/execute?command=restart
```

```json
{
  "status": "success",
  "message": "Command 'restart' executed successfully.",
  "output": "..."
}
```

---

### Dashboard — `/api/dashboard`

Queries OpenVPN service metadata directly from systemd for real-time health monitoring.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard` | Get current VPN service status |

```json
{
  "timestamp": "2026-02-10T03:20:01",
  "active": "active",
  "since": "Mon 2026-02-09 18:00:00 UTC",
  "main_pid": "1234",
  "status_message": ""
}
```

---

### Connected Clients — `/api/clients`

Parses the OpenVPN status log in real time to list all active VPN connections.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/clients` | List currently connected VPN clients |

```json
[
  {
    "commonName": "client-device-01",
    "realAddress": "203.0.113.45:51820",
    "bytesReceived": 204800,
    "bytesSent": 102400,
    "connectedSince": "2026-02-10 01:15:00",
    "status": "online"
  }
]
```

---

### Traffic Captures — `/api/capture`

Triggers Wireshark capture scripts on the server. Duration defaults to 10 seconds if not specified.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/capture` | Start a traffic capture |

```bash
POST /api/capture
Content-Type: application/json

{ "duration": "60" }
```

```json
{
  "message": "Capture completed for 60 seconds",
  "output": "..."
}
```

---

### PCAP Files & CSV Reports — `/api/csv_files`

Manages the inventory of capture files and their processing status. Each `.pcap` file gets converted to a `.csv` by an external Python script.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/csv_files` | List all PCAP files with their processing status |
| POST | `/api/csv_files/download` | Download a specific CSV report |
| POST | `/api/csv_files/reparar/{filename}` | Re-trigger processing for a stuck file |

**Processing states:**

| Status | Meaning |
|--------|---------|
| `true` | CSV successfully generated |
| `pending` | Still within expected processing window |
| `false` | Processing timed out (exceeded 3x capture duration) |

**Download a CSV:**
```bash
POST /api/csv_files/download
Content-Type: application/json

{ "CSVFILE": "traffic_2026-02-10_03-20-01_(10.0_minutes)_(0.06_input)_(0.05_output).csv" }
```

---

### Traffic Analytics — `/api/traffic` (Charts)

Provides aggregated daily traffic statistics for frontend charts and dashboards.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/traffic/last/{days}` | Stats for the last N days |
| GET | `/api/traffic/range?from=&to=` | Stats between two dates |
| GET | `/api/traffic/top?limit=5` | Days with highest traffic volume |

```bash
GET /api/traffic/last/7
GET /api/traffic/range?from=2026-02-01&to=2026-02-10
GET /api/traffic/top?limit=3
```

---

### ML Analysis & Training — `/api/traffic` (ML)

Orchestrates the Python anomaly detection pipeline. Triggers scoring and model training, and exposes logs for inspection.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/traffic/score` | Run anomaly scoring on a CSV |
| POST | `/api/traffic/train` | Retrain detection models |
| GET | `/api/traffic/history` | Download full analysis history (JSON) |
| GET | `/api/traffic/models_info` | Get metadata of loaded models |
| GET | `/api/traffic/training_log` | Download last training log (JSON) |

**Run scoring:**
```bash
POST /api/traffic/score
Content-Type: application/json

{
  "CSVFILE": "traffic_2026-02-10_03-20-01_(10.0_minutes)_(0.06_input)_(0.05_output).csv",
  "range": "2026-01-01_2026-01-31"
}
```

**Retrain models:**
```bash
POST /api/traffic/train
Content-Type: application/json

{
  "mode": "normal",
  "fromDate": "2026-01-01",
  "toDate": "2026-01-31"
}
```

Training modes: `low` / `normal` / `hardcore`

---

### Auth — `/api/login`, `/register`

Dual-mode authentication: REST API for the Angular SPA and HTML view rendering for web fallback.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/login` | Login (JSON response) |
| GET | `/login` | Login page (HTML view) |
| GET | `/register` | Registration form |
| POST | `/register` | Submit new user |

```bash
POST /api/login
Content-Type: application/json

{ "email": "admin@vpn.local", "password": "secret" }
```

```json
{ "ok": true }
```

---

## Configuration

`src/main/resources/application.properties`

```properties
# Directory where Wireshark CSV/PCAP files are stored
traffic.dir=/home/pi/captures

# Path to the Wireshark capture script
script.path=/home/pi/scripts/capture.sh

# Path to the Python registration/processing script
reg.script.path=/home/pi/scripts/process_pcap.py
```

## Installation

```bash
git clone https://github.com/nathanvargas/springboot-vpn-backend.git
cd springboot-vpn-backend

# Configure your paths in src/main/resources/application.properties

./mvnw spring-boot:run
```

Requires Java 17+, Maven, and a MySQL database accessible to the application.

## Related Projects

- [Angular Frontend](https://github.com/nathanvargas/angular-vpn-interface) — Real-time dashboard and traffic visualization
- [Python ML Pipeline](https://github.com/nathanvargas/anomaly-scoring) — Anomaly detection with Isolation Forest and K-Means

---

Built with Java, Spring Boot, Spring Data JPA. Part of the VPN Anomaly Detection project.
