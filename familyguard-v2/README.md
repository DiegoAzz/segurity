# FamilyGuard 2.0 🛡️

**FamilyGuard 2.0** is an enterprise-ready, cross-platform parental control platform built from scratch. It provides reliable remote supervision, screen time management, application blocking, and schedule enforcement for families and schools.

> **Important:** This is a clean-slate re-architecture. It does not share dependencies, background loops, or storage layers with legacy implementations.

---

## 🏗️ Repository Layout

```text
familyguard-v2/
├── apps/
│   ├── child_flutter/        # Android Child App (Flutter UI + Kotlin Engine)
│   ├── parent_flutter/       # Parent Dashboard (Flutter Web & Mobile)
│   └── admin_flutter/        # SaaS Super Admin Console (Flutter Web)
├── packages/
│   ├── android_control/      # Flutter-to-Android Platform Channel abstraction
│   ├── shared_models/        # Common Dart data transfer objects & validation
│   └── api_client/           # Shared Dio HTTP client & WebSocket telemetry
├── backend/
│   ├── src/                  # NestJS TypeScript backend
│   ├── prisma/               # PostgreSQL schema & migrations
│   └── test/                 # Unit and E2E test suites
├── infrastructure/
│   ├── docker/               # Production Dockerfiles (multi-stage)
│   └── coolify/              # Coolify VPS configuration templates
├── docs/
│   ├── architecture.md       # High-level architecture & design invariants
│   └── roadmap.md            # 10-phase engineering roadmap & test gates
├── docker-compose.yml        # Local orchestration (NestJS + Postgres + Redis)
├── .env.example              # Sample environment configuration
└── README.md
```

---

## 🛠️ Technology Stack

| Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Mobile Presentation** | Flutter 3.47+ & Dart | Modern reactive UI, fast iteration, unified design |
| **State Management** | Riverpod | Compile-time safety, easy testing, zero global mutable state |
| **Navigation** | GoRouter | Declarative URL-driven routing, ideal for Flutter Web |
| **Native Enforcement** | Kotlin (Android SDK) | Deterministic `PolicyEngine`, Room, WorkManager, Foreground Services |
| **Backend API** | NestJS (TypeScript) | Modular enterprise architecture, strong typing, clean DI |
| **Database** | PostgreSQL 16 + Prisma 6 | ACID compliance, relational multi-tenancy, type-safe queries |
| **Asynchronous Jobs** | BullMQ + Redis 7 | Scheduled policy updates, telemetry batching, push notifications |
| **Hosting & DevOps** | Docker + Coolify | Isolated containers, persistent volumes, automated backups |

---

## 🚀 Getting Started

### Prerequisites
- Node.js `v20+` & npm
- Flutter SDK `3.47+`
- Android Studio / Android SDK (API 34+)
- Docker & Docker Compose

### 1. Backend Setup
```bash
cd backend
npm install
cp ../.env.example .env
npx prisma generate
npm run build
npm run start:dev
```

### 2. Child Android App Setup
```bash
cd apps/child_flutter
flutter pub get
flutter analyze
flutter run
```

### 3. Docker Local Environment
```bash
docker-compose up -d
```
Starts:
- **PostgreSQL 16** on port `5432` with persistent volume `familyguard_v2_pgdata`.
- **Redis 7** on port `6379` with persistent volume `familyguard_v2_redisdata`.
- **NestJS Backend** on port `3000`.

---

## 🧪 Verification & Health Status (Phase 1)

- **Backend NestJS Build:** ✅ Exit Code 0 (`dist/` created cleanly).
- **Prisma Schema Validation:** ✅ Validated against PostgreSQL schema.
- **Child App Dart Analysis:** ✅ 0 issues (`flutter analyze` clean).
- **Kotlin Policy Engine:** ✅ Monotonic version guard & emergency whitelist compiled.
- **Git State:** Isolated on branch `familyguard-v2`. Legacy code untouched.
