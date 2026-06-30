# YumAI — AI-Powered Restaurant Management SaaS

Full-stack implementation of the SWE Lab project proposal: billing & POS, inventory with
low-stock alerts and waste logs, real-time analytics dashboard, AI insights & chat assistant
(Google Gemini), PDF/CSV reports, and role-based access control.

| Layer    | Technology                                              |
|----------|---------------------------------------------------------|
| Frontend | React 18 + Vite + Tailwind CSS 4 + Recharts             |
| Backend  | Spring Boot 3.4 (Java 21) — Spring Security, JPA        |
| Database | H2 (dev profile) / MySQL (prod profile)                 |
| AI       | Google Gemini API (`gemini-2.0-flash`) with offline fallback |
| Auth     | Stateless JWT + BCrypt, logout blacklist                |
| PDF      | OpenPDF (server-side invoices & reports)                |

Deviations from the proposal (and why) are recorded in [`DEVIATIONS.md`](./DEVIATIONS.md).

## Quick start

Prerequisites: JDK 21+, Maven 3.9+, Node 20+.

### 1. Backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

First start seeds demo data (H2 file DB in `backend/data/`). Demo accounts:

| Role    | Email             | Password    |
|---------|-------------------|-------------|
| Admin   | admin@yumai.com   | Admin@123   |
| Manager | manager@yumai.com | Manager@123 |
| Staff   | staff@yumai.com   | Staff@123   |
| Viewer  | viewer@yumai.com  | Viewer@123  |

Optional environment variables:

- `GEMINI_API_KEY` — enables real Gemini-powered insights/chat (otherwise a built-in statistical engine answers)
- `JWT_SECRET` — override the dev JWT signing key
- Production (MySQL): run with `--spring.profiles.active=prod` and set `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 — the dev server proxies `/api` to the backend.

## Module map (requirement traceability)

| Module (SRS) | Backend | Frontend |
|---|---|---|
| FR-01 Auth & RBAC | `AuthController`, `SecurityConfig`, `JwtService`, `TokenBlacklistService` | `Login.jsx`, `AuthContext.jsx` |
| FR-02 Orders & Billing | `OrderController`, `BillController`, `OrderService`, `PdfService` | `Orders.jsx`, `Bills.jsx` |
| FR-03 Inventory & Waste | `InventoryController`, `InventoryService` | `Inventory.jsx` |
| FR-04 Dashboard & Analytics | `DashboardController`, `DashboardService` | `Dashboard.jsx` (30 s auto-refresh) |
| FR-05 AI Insights & Chat | `AiController`, `AiService`, `GeminiClient` | `Insights.jsx`, `Chat.jsx` |
| FR-06 Reports (PDF/CSV) | `ReportController`, `ReportService` | `Reports.jsx` |
| UC-02 User Management | `UserController`, `UserService` | `Users.jsx` |

## Role access matrix (FR-01.3)

| Capability | Admin | Manager | Staff | Viewer |
|---|---|---|---|---|
| Dashboard (UC-10) | ✓ | ✓ | ✓ | ✓ |
| Orders / POS (UC-04/05) | ✓ | ✓ | ✓ | — |
| Menu view / waste log | ✓ | ✓ | ✓ | — |
| Menu & inventory management | ✓ | ✓ | — | — |
| Billing history (UC-06) | ✓ | ✓ | — | — |
| Reports (FR-06.4) | ✓ | ✓ | — | — |
| AI insights & chat (UC-13/14/15) | ✓ | ✓ | — | — |
| User CRUD (FR-01.4) | ✓ | read-only | — | — |

## Project structure

```
yumai/
├── backend/                 # Spring Boot — layered per NFR-07
│   └── src/main/java/com/yumai/
│       ├── controller/      # REST endpoints
│       ├── service/         # business logic, Gemini client, PDF/CSV
│       ├── repository/      # Spring Data JPA
│       ├── entity/          # class diagram (SRS §5)
│       ├── dto/             # request/response records
│       ├── security/        # JWT filter, token blacklist
│       ├── config/          # security rules, data seeder
│       └── exception/       # uniform API error handling
├── frontend/                # React SPA with persistent sidebar (SRS 3.3.1)
│   └── src/{api,context,components,pages}/
├── DEVIATIONS.md            # recorded deviations from the proposal
└── README.md
```

## Build for production

```bash
cd backend  && mvn -DskipTests package        # target/yumai-backend-1.0.0.jar
cd frontend && npm run build                  # dist/
```
