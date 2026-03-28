# Java 25/26 — Scoped Values + Structured Concurrency Demo

> **Real-world Spring Boot application** demonstrating 4 production use cases using  
> **ScopedValue (JEP 506 — Final)** + **StructuredTaskScope (JEP 505/525 — Preview)**

## Prerequisites

- **Java 25 or 26** (ScopedValue is final; StructuredTaskScope requires `--enable-preview`)
- **Maven 3.9+** (or use the wrapper)

## Run

```bash
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

---

## 4 Use Cases — API Quick Reference

### 1. Distributed Tracing — `TracingService`
> Bind traceId once at HTTP edge → read deeper in the call stack. No parameter drilling.

```bash
curl http://localhost:8080/api/tracing/order/ORD-123
curl -H "X-Trace-ID: my-custom-trace" http://localhost:8080/api/tracing/order/ORD-456
```

**Pattern:** `ScopedValue.where().call()`  
**Why not method params:** traceId is cross-cutting infrastructure, not domain data.

---

### 2. Auth Context Propagation — `AuthContextService`
> Replaces ThreadLocal-backed SecurityContextHolder. Auth flows to backend components for permission checks, credit checks, and audit logging.

```bash
curl -X POST http://localhost:8080/api/auth/create-order \
  -H "Content-Type: application/json" \
  -H "X-User-ID: user-42" \
  -H "X-Tenant-ID: acme-corp" \
  -d '{"item": "LAPTOP-PRO", "quantity": 1}'
```

**Pattern:** Immutable `AuthContext` record bound to a ScopedValue  
**Key:** No `DelegatingSecurityContextRunnable` boilerplate needed. ScopedValue avoids all data races.

---

### 3. Multi-Tenant Isolation — `MultiTenantService`
> Zero-leak tenant routing. ThreadLocal can leak tenant A's data to tenant B's request on a reused thread. ScopedValue makes this structurally impossible.

```bash
curl -H "X-Tenant-ID: acme" "http://localhost:8080/api/tenant/dashboard?from=2026-01-01&to=2026-03-28"
curl -H "X-Tenant-ID: globex" "http://localhost:8080/api/tenant/dashboard"
```

**Pattern:** Tenant bound to scope → deeper queries auto-route to correct schema  
**Critical:** Binding is tied to `.call()` block — unreachable after scope ends.

---

### 4. Feature Flags / A/B Testing (Nested Rebinding) — `FeatureFlagService`
> Shadowing pattern: inner scope shadows outer ScopedValue binding. Auto-restores on exit.

```bash
# Normal rendering with experiment cohort
curl "http://localhost:8080/api/experiment/product/PROD-101?cohort=treatment_a"

# Admin override — shows both user view AND control group preview
curl "http://localhost:8080/api/experiment/product/PROD-101/admin-override?cohort=treatment_b"
```

**Pattern:** Nested `ScopedValue.where()` shadows outer binding  
**Impossible with ThreadLocal:** Requires fragile save/restore boilerplate.

---

## Project Structure

```
src/main/java/com/demo/structured/
├── StructuredConcurrencyDemoApplication.java
├── context/                          # Shared ScopedValue holders + records
│   ├── TraceContext.java
│   ├── AuthContext.java
│   ├── TenantContext.java
│   └── ExperimentContext.java
├── tracing/                          # Use Case 1
│   ├── TracingService.java
│   └── TracingController.java
├── auth/                             # Use Case 2
│   ├── AuthContextService.java
│   └── AuthContextController.java
├── tenant/                           # Use Case 3
│   ├── MultiTenantService.java
│   └── MultiTenantController.java
└── experiment/                       # Use Case 4
    ├── FeatureFlagService.java
    └── FeatureFlagController.java
```

## Key APIs Used

| API | JEP | Status in Java |
|-----|-----|--------------------|
| `ScopedValue.newInstance()` | 506 | **Final** ✅ |
| `ScopedValue.where(key, value).call()` | 506 | **Final** ✅ |
| `StructuredTaskScope.open()` | 505 | **Preview** 🔶 |

| Use ScopedValue | Use Method Parameter |
|-----------------|----------------------|
| `traceId`, `tenantId`, `requestId` — cross-cutting | `orderId`, `customerId` — domain data |
| Auth `Principal` / JWT claims — security context | Entity being transformed — computational input |
| Feature flags / experiment config — ambient config | Data that varies per call within same scope |
| Needed across many layers but belongs to none | Needed by 1-2 callees only |
