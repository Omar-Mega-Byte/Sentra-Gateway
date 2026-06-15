# Sentra User Service

Internal profile service for the Sentra Gateway demonstration. It exposes one
redacted public lookup and trusted current-user read/update operations.

## Runtime

- Java 25
- Spring Boot 4.0.7
- Spring MVC
- Springdoc OpenAPI 3.0.3
- Micrometer/Prometheus
- Deterministic in-memory profile repository

The service does not validate external JWTs. `gateway-service` authenticates the
caller, removes reserved inbound headers, and forwards bounded trusted context.

## Build And Test

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd javadoc:javadoc
```

Generated artifacts:

- Executable JAR: `target/user-service-1.0.0-SNAPSHOT.jar`
- Javadoc JAR: `target/user-service-1.0.0-SNAPSHOT-javadoc.jar`
- Browsable Javadocs: `target/reports/apidocs/index.html`
- JaCoCo report: `target/site/jacoco/index.html`

## Local JVM

PowerShell does not automatically import `.env`. Set at least the active profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

## Podman

The base Compose file does not publish port `8081`. It attaches the service to
the gateway's internal services network:

```powershell
podman network inspect sentra-gateway_services
podman compose --env-file .env config
podman compose --env-file .env up --build -d
podman compose ps
```

If the shared network does not exist yet:

```powershell
podman network create --internal sentra-gateway_services
```

For direct Postman testing, add the loopback-only port override:

```powershell
podman compose --env-file .env `
  -f compose.yaml `
  -f compose.postman.yaml `
  up --build -d
```

Health and documentation:

```text
http://localhost:8081/actuator/health/liveness
http://localhost:8081/actuator/health/readiness
http://localhost:8081/v3/api-docs
http://localhost:8081/swagger-ui.html
```

## Postman

Import:

- `postman/Sentra_User_Service.postman_collection.json`
- `postman/Sentra_User_Service_Local.postman_environment.json`

Select **Sentra User Service Local** and run the complete collection. Trusted
headers in the collection simulate headers created by `gateway-service`.

CLI equivalent:

```powershell
npx --yes newman run `
  postman/Sentra_User_Service.postman_collection.json `
  -e postman/Sentra_User_Service_Local.postman_environment.json
```

## Seed Profile

| Field | Value |
| --- | --- |
| Profile ID | `7aa99db8-a943-4b63-b4b7-79f769ef9f87` |
| Trusted subject | `sentra-user-omar` |
| Initial version | `3` |

Restarting the service resets in-memory state when `PROFILE_SEED_ENABLED=true`.

## API Summary

| Method | Internal path | Required trusted route/scope |
| --- | --- | --- |
| `GET` | `/internal/v1/users/{id}/public` | `user-public-profile` |
| `GET` | `/internal/v1/users/me` | `user-profile-read`, `profile:read` |
| `PATCH` | `/internal/v1/users/me` | `user-profile-update`, `profile:write` |

See `docs/API_CONTRACT.md`, `docs/CONFIGURATION.md`, and
`docs/OPERATIONS.md` for the complete contract and operating model.
