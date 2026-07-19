# NIC Issuing Website

Spring Boot based National Identity Card issuing service with a static HTML, CSS, and JavaScript frontend. The system lets citizens submit New NIC, Renew NIC, and Lost NIC requests online, make payments, track their own applications, and contact support. Staff users manage applications, finance records, deliveries, recovery work, feedback, and assistance requests through role-specific dashboards.

## Current Features

- Citizen signup and login with `X-Auth-Token` based sessions
- Staff role bootstrap for `ADMIN`, `FINANCE`, `DELIVERY`, `PRO`, `RECOVERY`, and `ASSISTANT`
- New NIC application submission with document and photo upload
- Renew NIC submission with audit fields: name, gender, address, old NIC number, birthdate, reason, and contact number
- Lost NIC submission with audit fields: full name, address, NIC number, lost date, contact number, birth certificate, and police report
- Duplicate New NIC prevention for active `PENDING` or `PROCESSING` applications
- Payment checkout linked to the exact submitted application by `applicationId`
- Payment records and finance statistics
- Application review and status updates
- Delivery tracking, delivery logs, and weekly report endpoint
- Feedback submission and management
- Assistance requests, assistant replies, edit/delete support, and request history
- Optional email/SMS notifications for application, payment, and assistance events

## Tech Stack

- Java 17
- Spring Boot 3.5.6
- Spring Web, Validation, Data JPA, Mail
- Spring Security Crypto for password hashing
- Hibernate
- SQL Server as the default database
- H2 local profile for lightweight development/testing
- Maven Wrapper
- Static HTML, CSS, and JavaScript frontend
- Lombok dependency and annotation processor configuration

## Project Structure

```text
NIC-Issuing-Website/
|-- pom.xml
|-- mvnw / mvnw.cmd
|-- src/
|   |-- main/
|   |   |-- java/com/project/nic/
|   |   |   |-- config/          API wrapping, CORS, staff bootstrap
|   |   |   |-- controller/      REST and page controllers
|   |   |   |-- dto/             API DTOs and response wrappers
|   |   |   |-- model/           JPA entities
|   |   |   |-- repository/      Spring Data repositories
|   |   |   |-- service/         Business rules and notifications
|   |   |   |-- strategy/        Payment strategy examples
|   |   |   `-- util/            File upload helpers
|   |   `-- resources/
|   |       |-- static/          HTML, CSS, and JavaScript frontend
|   |       |-- templates/       Template-based dashboard page
|   |       |-- application.properties
|   |       `-- application-local.properties
|   `-- test/
|       `-- java/com/project/nic/
`-- README.md
```

## Main Workflows

### Citizen Application Flow

1. Citizen signs up or logs in.
2. Frontend stores `authToken`, `userRole`, and `loggedInEmail` in local storage.
3. Citizen submits `new-nic.html`, `renew-nic.html`, or `lost-nic.html`.
4. The submit endpoint returns:

```json
{
  "status": "SUCCESS",
  "message": "New NIC application submitted successfully.",
  "applicationId": 123
}
```

5. The frontend redirects to:

```text
paymentGateway.html?type=new&appId=123
```

6. The payment page sends `applicationId` to `/api/payments/checkout`.
7. `PaymentService` resolves the exact application by ID and verifies it belongs to the authenticated user before creating the payment record.

### New NIC Duplicate Guard

New NIC submissions are rejected if the authenticated user already has an application with status:

- `PENDING`
- `PROCESSING`

The API returns `400 Bad Request` with:

```text
You already have an active NIC application being processed.
```

### File Uploads

Uploads are validated server-side and stored under:

```properties
app.upload.dir=${UPLOAD_DIR:${user.home}/nic-secure-uploads}
```

Default limits:

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=12MB
```

## Important API Routes

| Area | Routes |
| --- | --- |
| Users | `POST /api/users/signup`, `POST /api/users/login`, `GET /api/users/session`, `GET /api/users`, `GET /api/users/by-email`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` |
| Applications | `GET /api/applications/mine` |
| New NIC | `POST /api/new-nic/submit`, `GET /api/new-nic/all`, `GET /api/new-nic/mine`, `GET /api/new-nic/{id}`, `PUT /api/new-nic/{id}/status` |
| Renew NIC | `POST /api/renew-nic/submit`, `GET /api/renew-nic/all`, `GET /api/renew-nic/mine`, `GET /api/renew-nic/{id}`, `PUT /api/renew-nic/{id}/status` |
| Lost NIC | `POST /api/lost-nic/submit`, `GET /api/lost-nic/all`, `GET /api/lost-nic/mine`, `GET /api/lost-nic/requests`, `GET /api/lost-nic/{id}`, `PUT /api/lost-nic/{id}`, `PUT /api/lost-nic/{id}/status`, `DELETE /api/lost-nic/{id}`, `GET /api/lost-nic/{id}/file` |
| Payments | `GET /api/payments`, `GET /api/payments/mine`, `POST /api/payments`, `POST /api/payments/checkout`, `GET /api/payments/{id}`, `PUT /api/payments/{id}`, `DELETE /api/payments/{id}`, `GET /api/payments/stats` |
| Payment Records | `GET /api/payment-records`, `GET /api/payment-records/mine` |
| Delivery | `GET /api/delivery/nics`, `GET /api/delivery/nics/all`, `GET /api/delivery/nics/{nic}`, `POST /api/delivery/nics`, `PUT /api/delivery/nics/{nic}`, `DELETE /api/delivery/nics/{nic}`, `GET /api/delivery/weekly-report` |
| Delivery Logs | `GET /api/delivery-logs`, `GET /api/delivery-logs/{id}`, `POST /api/delivery-logs`, `PUT /api/delivery-logs/{id}`, `DELETE /api/delivery-logs/{id}` |
| Feedback | `POST /api/feedback`, `GET /api/feedback`, `PUT /api/feedback/{id}`, `DELETE /api/feedback/{id}` |
| Assistance | `POST /api/assistance/request`, `GET /api/assistance/all`, `POST /api/assistance/reply/{id}`, `GET /api/assistance/requestsByEmail`, `PUT /api/assistance/request/{id}`, `DELETE /api/assistance/request/{id}`, `DELETE /api/assistance/{id}` |
| Assistant Logs | `GET /api/assistant-logs`, `GET /api/assistant-logs/{id}`, `POST /api/assistant-logs`, `PUT /api/assistant-logs/{id}`, `DELETE /api/assistant-logs/{id}` |

## Frontend Pages

The frontend is served from `src/main/resources/static`.

Common pages:

- `home.html`
- `signup.html`
- `login.html`
- `new-nic.html`
- `renew-nic.html`
- `lost-nic.html`
- `paymentGateway.html`
- `confirmation.html`
- `dashboard.html`
- `admin-dashboard.html`
- `assistant-dashboard.html`
- `recovery-dashboard.html`
- `delivery.html`
- `finance.html`
- `feedback.html`
- `assistance.html`

After startup:

```text
http://localhost:8080/home.html
```

Frontend API calls use relative `/api/...` paths so the static frontend works behind the same production host.

## Staff Accounts

`StaffAccountInitializer` creates or updates default staff accounts at startup:

| Email | Role | Default Password |
| --- | --- | --- |
| `admin@gmail.com` | `ADMIN` | `1234` |
| `finance@gmail.com` | `FINANCE` | `1234` |
| `delivery@gmail.com` | `DELIVERY` | `1234` |
| `pro@gmail.com` | `PRO` | `1234` |
| `recovery@gmail.com` | `RECOVERY` | `1234` |
| `assistant@gmail.com` | `ASSISTANT` | `1234` |

Change these credentials before any real deployment.

## Prerequisites

- JDK 17 or newer
- Maven is optional because the Maven Wrapper is included
- SQL Server for the default profile, or the local H2 profile for quick development

## Configuration

Default configuration is in:

```text
src/main/resources/application.properties
```

Important environment variables:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | SQL Server JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `AUTH_TOKEN_SECRET` | JWT signing secret for `X-Auth-Token` sessions |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins |
| `UPLOAD_DIR` | Secure upload storage directory |
| `NOTIFICATIONS_EMAIL_ENABLED` | Enable email notifications |
| `NOTIFICATIONS_EMAIL_FROM` | Email sender address |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP settings |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | SMTP flags |
| `NOTIFICATIONS_SMS_ENABLED` | Enable SMS notifications |
| `SMS_GATEWAY_URL`, `SMS_API_TOKEN` | SMS gateway settings |

PowerShell example for SQL Server:

```powershell
$env:DB_PASSWORD="your-sql-server-password"
$env:DB_USERNAME="sa"
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=nic_db;encrypt=true;trustServerCertificate=true"
$env:AUTH_TOKEN_SECRET="replace-with-a-long-random-secret"
$env:CORS_ALLOWED_ORIGINS="http://localhost:8080"
$env:UPLOAD_DIR="C:\nic-secure-uploads"
.\mvnw.cmd spring-boot:run
```

## Run Locally With SQL Server

1. Start SQL Server.
2. Create a database named `nic_db`.
3. Set `DB_PASSWORD` and `AUTH_TOKEN_SECRET`.
4. Run:

```powershell
.\mvnw.cmd spring-boot:run
```

The app starts at:

```text
http://localhost:8080
```

## Run Locally With H2

For a lightweight local database without SQL Server:

```powershell
$env:AUTH_TOKEN_SECRET="replace-with-a-long-random-secret"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

The H2 file database is stored under:

```text
target/nic-local-db
```

## Build

Windows:

```powershell
.\mvnw.cmd clean package
```

macOS/Linux:

```bash
./mvnw clean package
```

The generated JAR is written to `target/`.

## Run Tests

Windows:

```powershell
.\mvnw.cmd test
```

macOS/Linux:

```bash
./mvnw test
```

Focused controller tests:

```powershell
.\mvnw.cmd test "-Dtest=NicApplicationControllerTests,PaymentControllerTests"
```

## Development Notes

- Put business rules in `service/`, not directly in frontend JavaScript.
- Keep frontend API calls relative, for example `/api/users/login`.
- Add repository methods only when custom data access is needed.
- For application submit responses, preserve the `status`, `message`, and `applicationId` contract.
- For citizen checkout payments, always send `applicationId`; payment records should not guess the latest application.
- If production does not use `spring.jpa.hibernate.ddl-auto=update`, create migrations for new columns such as `payments.application_id`, `renewnic.name`, `renewnic.gender`, `renewnic.address`, `lostnic.full_name`, and `lostnic.address`.

## Security Notes

Before real public deployment, review and harden:

- Default staff account passwords
- JWT secret management and token expiry rules
- Role-based access control coverage
- CORS settings
- CSRF strategy if moving beyond same-origin static pages
- File upload storage permissions and malware scanning
- Payment provider integration
- Database migrations and backups
- Email/SMS credential storage
- Audit logging for staff actions

## License

This project includes a `LICENSE` file. Review it before copying, modifying, or distributing the code.
