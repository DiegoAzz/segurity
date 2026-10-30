# FamilyGuard 2.0 — Engineering Roadmap & Execution Plan

This roadmap outlines the 10 sequential phases for building and launching FamilyGuard 2.0. In accordance with architectural safety rules, no phase begins until the preceding phase meets all verification criteria.

---

## Phase Matrix

| Phase | Description | Deliverables | Verification Gates | Status |
| :--- | :--- | :--- | :--- | :---: |
| **FASE 1** | Monorepo Setup & Architecture Foundation | Independent branch, folder layout, clean Flutter apps, clean NestJS backend, Prisma schema, Docker files, docs | Zero compilation errors on Flutter & NestJS. Clean git branch. | **COMPLETED** |
| **FASE 2** | Android Native Engine & Permissions Diagnosis | Kotlin `PolicyEngine.kt`, Android Services (`AccessibilityMonitor`, `BootReceiver`), Permission diagnostics channel | Passes cold-boot persistence test and unit test suite. | Next Up |
| **FASE 3** | Proof of Concept: Deterministic App Enforcement | Lock overlay UI, anti-flicker test harness, synthetic stress test (1,000 rapid cycles) | Zero lock/unlock flickering. Emergency dialer always accessible. | Pending |
| **FASE 4** | Multi-Device Android Compatibility Matrix | Validation across Android 12, 13, 14, 15, and OEM skins (OPPO ColorOS, Xiaomi HyperOS, Samsung OneUI) | No crash on reboot, foreground service types validated. | Pending |
| **FASE 5** | NestJS Backend & PostgreSQL API Core | Auth (JWT), Family Multi-Tenancy Guards, Children CRUD, Device Pairing, Policy API | E2E integration test suite with isolated PostgreSQL test container. | Pending |
| **FASE 6** | Remote Synchronization & Telemetry Pipeline | BullMQ workers, WebSocket / FCM push, Monotonic Policy Version Acknowledgment | Two-way handshake verified: Server Push -> Android ACK -> Server DB update. | Pending |
| **FASE 7** | Parent Web & Mobile Dashboard (Flutter) | Riverpod state management, GoRouter, Live device cards, Instant lock switch, Schedule planner | Flutter Web build passes, responsive layout tested. | Pending |
| **FASE 8** | SaaS Admin Console (Flutter Web) | Tenant explorer, Device health monitors, Global analytics, Audit log viewer | Super admin role guard passes security audit. | Pending |
| **FASE 9** | Subscriptions, Quotas & Commercial Billing | Tier enforcement (Free, Pro, Enterprise), Child/Device limiters, Payment webhooks | Quota enforcement tests pass; subscription renewal / expiry tests pass. | Pending |
| **FASE 10** | Coolify VPS Production Deployment & Hardening | Production Docker compose, SSL certificates, automated backup cron jobs, disaster recovery drill | Isolated stack runs on VPS without interfering with legacy or adjacent apps. | Pending |

---

## Phase 1 Acceptance Criteria (Checklist)

- [x] Branch `familyguard-v2` created from `main` preserving all historical commits.
- [x] Directory `familyguard-v2/` created in complete isolation.
- [x] Clean Flutter projects initialized (`apps/child_flutter`, `apps/parent_flutter`, `apps/admin_flutter`).
- [x] Shared package modules scaffolded (`packages/android_control`, `packages/shared_models`, `packages/api_client`).
- [x] NestJS backend scaffolded with TypeScript, Prisma 6, PostgreSQL models, and BullMQ queue support.
- [x] Kotlin Native `PolicyEngine.kt` implemented with monotonic versioning, emergency dialer whitelist, and offline caching.
- [x] MethodChannel `com.familyguard.child/enforcement` wired in `MainActivity.kt`.
- [x] Multi-tenant relational schema drafted with Family, User, Child, Device, Policy, PolicyAck, TimeRequest, UsageLog, Subscription, and AuditLog.
- [x] Docker multi-stage build files and docker-compose orchestration defined with health checks and persistent volumes.
- [x] Local compilation verified: `npm run build` (NestJS: Exit code 0) & `flutter analyze` (Child app: 0 issues).
