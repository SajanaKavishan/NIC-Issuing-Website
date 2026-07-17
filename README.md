# NIC Issuing Website

Web-based National Identity Card issuing service for Sri Lanka. This project is a Spring Boot application that helps citizens submit NIC-related requests online and gives administrative users tools to manage applications, payments, deliveries, feedback, and assistance requests.

## Overview

The system supports the main NIC service workflows in one place:

- Citizen registration and login
- New NIC application submission
- NIC renewal requests
- Lost NIC replacement requests with supporting document upload
- Payment record management and finance statistics
- NIC delivery tracking and delivery logs
- Feedback collection and management
- Assistance request handling with assistant replies
- Admin, assistant, recovery, delivery, finance, and public-facing dashboard pages

## Tech Stack

- Java 17
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA / Hibernate
- SQL Server
- Maven Wrapper
- Static HTML, CSS, and JavaScript frontend
- Lombok
- H2 dependency available for runtime/testing support

## Project Structure

```text
NIC-Issuing-Website/
|-- pom.xml
|-- mvnw / mvnw.cmd
|-- src/
|   |-- main/
|   |   |-- java/com/project/nic/
|   |   |   |-- controller/      REST and page controllers
|   |   |   |-- model/           JPA entities
|   |   |   |-- repository/      Spring Data repositories
|   |   |   |-- service/         Business logic
|   |   |   |-- strategy/        Payment strategy implementations
|   |   |   `-- util/            Shared utilities
|   |   `-- resources/
|   |       |-- static/          HTML, CSS, and JavaScript pages
|   |       |-- templates/       Template-based dashboard page
|   |       `-- application.properties
|   `-- test/
|       `-- java/com/project/nic/
`-- README.md
```

## Main Modules

| Module | Purpose |
| --- | --- |
| User Management | Signup, login, user listing, user update, and deletion |
| New NIC | First-time NIC application submission |
| Renew NIC | Existing NIC renewal request submission |
| Lost NIC | Lost NIC replacement workflow, document access, and request status updates |
| Payments | Payment CRUD operations, payment strategy support, and statistics |
| Delivery | NIC delivery tracking, filtering, weekly reports, and logs |
| Feedback | Citizen feedback submission and administrative review |
| Assistance | Citizen support requests and assistant responses |

## Key API Routes

| Area | Routes |
| --- | --- |
| Users | `/api/users`, `/api/users/signup`, `/api/users/login`, `/api/users/by-email` |
| New NIC | `/api/new-nic/submit` |
| Renew NIC | `/api/renew-nic/submit` |
| Lost NIC | `/api/lost-nic/submit`, `/api/lost-nic/all`, `/api/lost-nic/requests`, `/api/lost-nic/{id}/status`, `/api/lost-nic/{id}/file` |
| Payments | `/api/payments`, `/api/payments/{id}`, `/api/payments/stats` |
| Delivery | `/api/delivery/nics`, `/api/delivery/nics/all`, `/api/delivery/nics/{nic}`, `/api/delivery/weekly-report` |
| Delivery Logs | `/api/delivery-logs`, `/api/delivery-logs/{id}` |
| Feedback | `/api/feedback`, `/api/feedback/{id}` |
| Assistance | `/api/assistance/request`, `/api/assistance/all`, `/api/assistance/reply/{id}`, `/api/assistance/requestsByEmail` |
| Assistant Logs | `/api/assistant-logs`, `/api/assistant-logs/{id}` |

## Public Pages and Dashboards

The frontend is served from `src/main/resources/static`.

Common entry pages include:

- `home.html`
- `signup.html`
- `login.html`
- `new-nic.html`
- `renew-nic.html`
- `lost-nic.html`
- `paymentGateway.html`
- `dashboard.html`
- `admin-dashboard.html`
- `assistant-dashboard.html`
- `recovery-dashboard.html`
- `delivery.html`
- `finance.html`
- `feedback.html`
- `assistance.html`

After the application starts, pages are available through:

```text
http://localhost:8080/home.html
```

## Prerequisites

Install the following before running the project:

- JDK 17 or newer
- SQL Server running locally
- A SQL Server database named `nic_db`
- Maven is optional because the project includes the Maven Wrapper

## Database Configuration

The SQL Server connection values are read from environment variables in `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:sqlserver://localhost:1433;databaseName=nic_db;encrypt=true;trustServerCertificate=true}
spring.datasource.username=${DB_USERNAME:sa}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect
```

Before running the application:

1. Start SQL Server.
2. Create a database named `nic_db`.
3. Set the database password as an environment variable. Optionally set the URL and username if your SQL Server settings are different.

PowerShell example:

```powershell
$env:DB_PASSWORD="your-sql-server-password"
$env:DB_USERNAME="sa"
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=nic_db;encrypt=true;trustServerCertificate=true"
$env:AUTH_TOKEN_SECRET="replace-with-a-long-random-secret"
.\mvnw.cmd spring-boot:run
```

For production use, provide database credentials through environment variables or a secure configuration provider instead of committing plain-text passwords.

## Run Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

## Build

On Windows:

```powershell
.\mvnw.cmd clean package
```

On macOS or Linux:

```bash
./mvnw clean package
```

The generated JAR will be created in the `target/` directory.

## Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS or Linux:

```bash
./mvnw test
```

## Suggested Development Workflow

1. Create or update the relevant entity in `model/`.
2. Add repository methods in `repository/` when custom data access is needed.
3. Put business rules in `service/`.
4. Expose HTTP endpoints from `controller/`.
5. Connect the static page JavaScript in `src/main/resources/static/js/`.
6. Add or update tests for the changed workflow.

## Security Notes

This project currently focuses on the application workflow. Before using it in a real public environment, review and improve:

- Password hashing and authentication/session management
- Role-based access control for admin and staff dashboards
- File upload validation and storage security
- Input validation for all public APIs
- CSRF and CORS settings
- Secure payment provider integration
- Secret management for database credentials
- Audit logging for sensitive administrative actions

## License

This project includes a `LICENSE` file. Review it before copying, modifying, or distributing the code.
