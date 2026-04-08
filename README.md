# Wound Detection — Auth API

A production-ready **Spring Boot 4** REST API providing:

- **Email + Password** registration and login with **6-digit OTP** verification
- **Google OAuth2** login (register + login in one step)
- **JWT** issued on successful verification, persisted in `auth_sessions`
- **Swagger UI** for interactive API exploration
- **PostgreSQL** (Supabase-compatible) database backend

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.5 / Spring Framework 7 |
| Security | Spring Security 7 + OAuth2 Client |
| Database | PostgreSQL (Supabase) |
| ORM | Hibernate 7 / Spring Data JPA |
| Auth | JWT (JJWT 0.12.6) |
| OAuth2 Provider | Google |
| Email | Spring Mail (Gmail SMTP) |
| Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Build | Maven 3.9 |
| Runtime | Java 21 |

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/register` | Public | Register → OTP sent to email |
| `POST` | `/api/auth/login` | Public | Login → OTP sent to email |
| `POST` | `/api/auth/verify-otp` | Public | Submit OTP → returns **JWT** |
| `POST` | `/api/auth/logout` | 🔒 Bearer | Invalidate all sessions |
| `GET` | `/api/auth/google` | Public | Redirect to Google consent screen |

### Documentation

| URL | Description |
|-----|-------------|
| `/swagger-ui/index.html` | Interactive Swagger UI |
| `/v3/api-docs` | OpenAPI 3 JSON spec |

---

## Auth Flow

### Email / OTP Flow
```
POST /api/auth/register   →  { requiresOtp: true }
POST /api/auth/login      →  { requiresOtp: true }
POST /api/auth/verify-otp →  { token: "eyJ..." }

# Use token on all protected endpoints:
Authorization: Bearer eyJ...
```

### Google OAuth2 Flow
```
Open in browser → GET /api/auth/google
                → Google consent screen
                → JSON response: { token: "eyJ...", email, fullName }
```

---

## Database Schema

Tables created automatically by Hibernate (`ddl-auto=update`):

| Table | Purpose |
|---|---|
| `users` | Core account: email, name, phone, password hash, verified flag |
| `user_profiles` | Extended info: blood type, weight, height, location |
| `oauth_accounts` | Linked OAuth providers: google + provider user ID |
| `otp_verifications` | OTP codes with expiry |
| `auth_sessions` | Active JWT sessions with expiry |

---

## Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL (or use Supabase free tier)

### 1. Clone
```bash
git clone <your-repo-url>
cd detection
```

### 2. Set Environment Variables

Create a local override file (never committed):
```bash
# src/main/resources/application-local.properties
DATABASE_URL=jdbc:postgresql://localhost:5432/wound
DATABASE_PASSWORD=your_local_password
JWT_SECRET=YourSecretKeyAtLeast32CharsLong!!
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

Or export them as environment variables:
```powershell
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/wound"
$env:DATABASE_PASSWORD="your_password"
$env:JWT_SECRET="YourSecretKeyAtLeast32CharsLong!!"
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:MAIL_USERNAME="your@gmail.com"
$env:MAIL_PASSWORD="your-app-password"
```

### 3. Run
```bash
mvn spring-boot:run
```

The API is available at: `http://localhost:8081`  
Swagger UI: `http://localhost:8081/swagger-ui/index.html`

---

## Docker

### Build
```bash
docker build -t wound-detection-api .
```

### Run
```bash
docker run -p 8081:8081 \
  -e DATABASE_URL="jdbc:postgresql://your-db-host:5432/postgres" \
  -e DATABASE_USERNAME="postgres" \
  -e DATABASE_PASSWORD="your-password" \
  -e JWT_SECRET="YourSecretKeyAtLeast32CharsLong!!" \
  -e GOOGLE_CLIENT_ID="your-client-id" \
  -e GOOGLE_CLIENT_SECRET="your-client-secret" \
  -e MAIL_USERNAME="your@gmail.com" \
  -e MAIL_PASSWORD="your-app-password" \
  wound-detection-api
```

---

## Deploy on Render

### Steps

1. **Push to GitHub** — make sure no secrets are committed (all values use `${ENV_VAR}`)

2. **Create a new Web Service** on [render.com](https://render.com)
   - Connect your GitHub repository
   - **Runtime**: Docker
   - **Dockerfile path**: `./Dockerfile`
   - **Port**: `8081`

3. **Set Environment Variables** in Render Dashboard → Environment:

| Key | Value |
|-----|-------|
| `DATABASE_URL` | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | Your Supabase DB password |
| `JWT_SECRET` | A random 32+ character string |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours) |
| `GOOGLE_CLIENT_ID` | From Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | From Google Cloud Console |
| `GOOGLE_REDIRECT_URI` | `https://your-app.onrender.com/login/oauth2/code/google` |
| `MAIL_USERNAME` | Gmail address |
| `MAIL_PASSWORD` | Gmail App Password |

4. **Update Google OAuth redirect URI**  
   In [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials:
   - Add `https://your-app.onrender.com/login/oauth2/code/google` to **Authorized redirect URIs**

5. **Deploy** — Render builds the Docker image and starts the container automatically.

> **Supabase SSL note:** Always append `?sslmode=require` to the DATABASE_URL when connecting to Supabase from a remote host.

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `PORT` | No | `8081` | Server port (auto-set by Render) |
| `DATABASE_URL` | **Yes** | — | Full JDBC URL |
| `DATABASE_USERNAME` | No | `postgres` | DB username |
| `DATABASE_PASSWORD` | **Yes** | — | DB password |
| `JWT_SECRET` | **Yes** | — | Min 32-char secret for HMAC-SHA256 |
| `JWT_EXPIRATION_MS` | No | `86400000` | Token TTL in ms (default 24h) |
| `GOOGLE_CLIENT_ID` | **Yes** | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | **Yes** | — | Google OAuth2 client secret |
| `GOOGLE_REDIRECT_URI` | No | `{baseUrl}/login/oauth2/code/{registrationId}` | Override for production |
| `MAIL_USERNAME` | **Yes** | — | Gmail address for OTP emails |
| `MAIL_PASSWORD` | **Yes** | — | Gmail App Password (not account password) |

---

## Security Notes

- All secrets are read from **environment variables** — never hardcoded
- JWT tokens are validated on every request via `JwtAuthenticationFilter`
- OAuth2 users are stored in `oauth_accounts` linked to `users`
- OTPs expire in **5 minutes** and are single-use
- Sessions are tracked in `auth_sessions` and cleared on logout
- Docker container runs as a **non-root user**
