# Email OTP setup

Registration now sends a six-digit email OTP. Accounts stay pending until the customer verifies the code. Codes expire after 10 minutes, can be resent after 60 seconds, and are invalidated after five incorrect attempts.

Configure an SMTP account in the backend environment before starting the application:

- `MAIL_HOST`: SMTP server hostname (defaults to Gmail SMTP)
- `MAIL_PORT`: SMTP submission port (defaults to `587`)
- `MAIL_USERNAME`: sender account email
- `MAIL_PASSWORD`: SMTP password or provider app password

Keep credentials in environment variables or a secrets manager; do not commit them. Without SMTP credentials, registration returns an error and does not create a usable account.

For local development, copy `.env.example` to `.env` and fill in your Gmail address and Google App Password. `.env` is ignored by Git, so the secret stays on your machine. Then start the backend with one command:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
.\scripts\run-backend-current.ps1
```

Spring Boot now reads `MAIL_*` from `.env` automatically, so either the launcher or `.\\mvnw.cmd spring-boot:run` works from the project root. If registration still fails, check that the SMTP provider allows authenticated mail submission over STARTTLS on port 587. The API returns the SMTP configuration error instead of a generic HTTP 500.