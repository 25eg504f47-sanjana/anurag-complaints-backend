# Anurag University Complaint Management System: Backend

Spring Boot 3 (Java 17) REST API with JWT auth and MySQL.
Frontend lives in a separate repository: `anurag-complaints-frontend`.

## Roles

| Role | Can do |
|------|--------|
| STUDENT | Register, log in, file complaints, view own complaints, withdraw own complaint while it is still OPEN |
| ADMIN | View all complaints, filter by status, update status and reply, delete, see stats |

Public registration always creates a STUDENT. The first ADMIN is created at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## API

| Method | Path | Access | Notes |
|--------|------|--------|-------|
| POST | `/api/auth/register` | public | name, email, password (min 8), rollNumber, department |
| POST | `/api/auth/login` | public | returns `{ token, user }` |
| GET | `/api/auth/me` | any user | |
| POST | `/api/complaints` | student | title, description, category, priority |
| GET | `/api/complaints` | any user | student: own; admin: all. Admin can add `?status=OPEN` |
| GET | `/api/complaints/{id}` | owner or admin | |
| PATCH | `/api/complaints/{id}/status` | admin | `{ status, adminResponse }` |
| DELETE | `/api/complaints/{id}` | owner (OPEN only) or admin | |
| GET | `/api/complaints/stats` | admin | counts by status |
| GET | `/actuator/health` | public | used by Render health check |

Send the token as `Authorization: Bearer <token>`.

Categories: ACADEMIC, HOSTEL, TRANSPORT, CANTEEN, INFRASTRUCTURE, LIBRARY, FEES, OTHER.
Statuses: OPEN, IN_PROGRESS, RESOLVED, REJECTED. Priorities: LOW, MEDIUM, HIGH.

## Environment variables

| Variable | Required | Example |
|----------|----------|---------|
| `DB_URL` | yes | `jdbc:mysql://host:port/defaultdb?sslMode=REQUIRED` |
| `DB_USERNAME` | yes | `avnadmin` |
| `DB_PASSWORD` | yes | |
| `JWT_SECRET` | yes | 32+ characters; the app refuses to start otherwise |
| `CORS_ORIGINS` | yes in production | `https://your-app.vercel.app` (comma separated, wildcards allowed) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | recommended | creates the first admin |
| `PORT` | no | set by Render |

Tables are created automatically on first start (`ddl-auto=update`).

## Run locally

Needs JDK 17, Maven and a local MySQL.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile (`application-local.properties`) points at `localhost:3306/complaints_db` with user `root`/`root` and seeds `admin@anurag.edu.in` / `Admin@12345`. Edit it to match your machine. Do not use these values in production.

Run the tests (they use an in-memory H2 database, no MySQL needed):

```bash
mvn test
```

## Deploy: Aiven MySQL + Railway (no Docker)

Render cannot build Java without Docker, so this uses Railway, which detects `pom.xml` and builds the jar itself.

1. **Aiven**: create a MySQL service. Copy Host, Port, User, Password. Build `DB_URL` as `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED`.
2. **Railway**: New Project > Deploy from GitHub repo > pick this repo. It builds with Maven automatically.
3. Variables tab: add `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and optionally `JAVA_OPTS=-XX:MaxRAMPercentage=70`.
4. Settings > Networking > Generate Domain. That URL is your `VITE_API_URL` for the frontend. Settings > Deploy: set the health check path to `/actuator/health`.
5. After the frontend is deployed, set `CORS_ORIGINS` here to the frontend URL. Railway redeploys on variable change.

Railway gives a one-time trial credit, then a small monthly charge, so it is not free forever. Aiven free services can be powered off when idle; power them back on from the Aiven console if the API cannot connect.
