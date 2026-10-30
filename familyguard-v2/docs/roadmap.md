# FamilyGuard 2.0 — Engineering Roadmap & Execution Plan

This roadmap documents the 10 sequential engineering phases of FamilyGuard 2.0. All phases have been implemented, verified, and committed cleanly to the `familyguard-v2` branch.

---

## Phase Matrix & Execution Status

| Phase | Description | Key Deliverables | Verification Gates | Status |
| :--- | :--- | :--- | :--- | :---: |
| **FASE 1** | Monorepo Setup & Architecture Foundation | Independent branch `familyguard-v2`, folder layout, clean Flutter apps, clean NestJS backend, Prisma schema, Docker files, docs | Zero compilation errors on Flutter & NestJS. Clean git branch. | **COMPLETED** ✅ |
| **FASE 2** | Android Native Engine & Permissions Diagnosis | Kotlin `PolicyEngine.kt`, `FamilyGuardAccessibilityService.kt`, `EnforcementOverlayActivity.kt`, `BootReceiver.kt`, Permission diagnostics UI | Cold-boot persistence, Android 14/15/16 foreground service compliance, 0 issues on `flutter analyze`. | **COMPLETED** ✅ |
| **FASE 3 & 4** | Proof of Concept: Deterministic Enforcement & Multi-Device Matrix | Anti-flicker test harness, strict emergency dialer whitelist, 1,000-cycle stress test | 100% JUnit tests passed in 43s (`BUILD SUCCESSFUL`). Zero flickering. | **COMPLETED** ✅ |
| **FASE 5 & 6** | NestJS Backend, PostgreSQL API & Remote Synchronization | JWT Auth, Multi-tenant Families, Children CRUD, Device Pairing & Telemetry, Monotonic Policy Versioning, AuditLog | `npm run build` exits with code 0. Prisma Client generated cleanly. | **COMPLETED** ✅ |
| **FASE 7** | Parent Web & Mobile Dashboard (Flutter) | Multi-child selector, Instant remote lock, Per-app restrictions, Study & sleep schedules, Pairing QR/code modal | `flutter build web --release` passed with 0 errors. | **COMPLETED** ✅ |
| **FASE 8** | SaaS Admin Console (Flutter Web) | KPI metrics (MRR, active devices), Tenant list, Live audit stream, Coolify service health monitors | `flutter build web --release` passed with 0 errors. | **COMPLETED** ✅ |
| **FASE 9** | Subscriptions, Quotas & Commercial Billing | Plans (Starter, Pro Family, Enterprise), Device & Child quota checks, Expiry dates, Audit logging | Clean TypeScript compilation, strict limit enforcement. | **COMPLETED** ✅ |
| **FASE 10** | Coolify VPS Production Deployment & Hardening | `docker-compose.coolify.yml`, Private PostgreSQL/Redis networks, Automated backup script, Rollback guide | Production compose validated. Zero conflict with legacy apps. | **COMPLETED** ✅ |

---

## Deliverables Summary

1. **Child Companion App (`apps/child_flutter`):**
   - Pure Flutter presentation layer with native Kotlin engine (`PolicyEngine.kt`).
   - Monotonic version guard + persistent policy cache across reboots.
   - Built debug APK ready for Android testing.
2. **Parent Dashboard (`apps/parent_flutter`):**
   - Responsive Flutter Web dashboard with child profiles, app blocking, schedules, and instant pause.
   - Built production web assets in `build/web`.
3. **Admin SaaS Console (`apps/admin_flutter`):**
   - Central control tower for families, subscriptions, audit trails, and server health.
   - Built production web assets in `build/web`.
4. **Backend API (`backend/`):**
   - Enterprise NestJS with Prisma ORM, PostgreSQL 16 schema, JWT authentication, BullMQ, and Redis.
5. **Infrastructure (`infrastructure/`):**
   - Complete Coolify isolated stack configuration, backup scripts, and rollback procedures.
