# Sentra Gateway Enterprise API Security Gateway

Sentra Gateway Enterprise API Security Gateway is a multi-service demonstration platform built around a reactive API gateway, trusted internal microservices, and a Next.js operations console.

The project shows how a gateway can centralize authentication, authorization, request signing, replay protection, policy enforcement, audit logging, and route management while keeping backend services private on an internal network.

## What Is Included

- `gateway-service`: reactive API security gateway and administration service
- `user-service`: internal profile service
- `order-service`: internal order service with idempotency support
- `payment-service`: internal partner payment service with gateway-validated signing
- `notification-service`: internal notification service for resilience and failure-mode demonstrations
- `frontend`: Next.js console for exploring and exercising the platform
- `scripts`: PowerShell automation for build, startup, health checks, logs, and E2E smoke tests
- `docs`: architecture, technical, and requirements documentation

## Key Capabilities

- Centralized route administration
- Gateway-validated JWT and local development authentication
- API key issuance and verification
- HMAC request signing for partner traffic
- Replay protection using Redis-backed nonce tracking
- IP, risk, and rate-limit policy enforcement
- Audit event persistence
- OpenAPI and Swagger UI for each service
- Health checks, metrics, and operational scripts
- Containerized local development with Podman or Docker Compose

## Repository Layout

```text
.
├─ gateway-service/
├─ user-service/
├─ order-service/
├─ payment-service/
├─ notification-service/
├─ frontend/
├─ scripts/
└─ docs/
```

## Prerequisites

- Java 25
- Maven Wrapper, included in each Java service
- Node.js 18+ for the frontend
- PowerShell 7+ on Windows, or compatible shell support for the scripts
- Podman 5+ or Docker Compose for containerized local runs
- PostgreSQL and Redis for the gateway and supported service stacks

## Quick Start

Each service has its own README with the most accurate runtime details. For a fast start, use the gateway service documentation first:

- [Gateway Service README](gateway-service/README.md)
- [User Service README](user-service/README.md)
- [Order Service README](order-service/README.md)
- [Payment Service README](payment-service/README.md)
- [Notification Service README](notification-service/README.md)
- [Frontend README](frontend/README.md)
- [Scripts README](scripts/README.md)

Typical local workflow:

1. Start the required infrastructure and services.
1. Open the gateway, service, or frontend endpoints.
1. Use the Postman collections to validate the API contracts.

## Running The Project

### Gateway Service

See the service README for the exact commands and environment variables:

- [gateway-service/README.md](gateway-service/README.md)

### Microservices

The internal services are designed to be run behind the gateway and each provides its own container, Maven, and Postman instructions.

- [user-service/README.md](user-service/README.md)
- [order-service/README.md](order-service/README.md)
- [payment-service/README.md](payment-service/README.md)
- [notification-service/README.md](notification-service/README.md)

### Frontend

The console is a separate Next.js app:

- [frontend/README.md](frontend/README.md)

### Automation Scripts

The `scripts` folder contains the workspace-level orchestration helpers:

- [scripts/README.md](scripts/README.md)

## Documentation

The repository includes both high-level and service-specific documentation.

- [Technical implementation overview](docs/Technical%20Implementation/Sentra_Gateway_Technical_Documentation.md)
- [Microservices documentation](docs/Technical%20Implementation/Sentra_Gateway_Microservices_Documentation.md)
- [System requirements specification](docs/Technical%20Implementation/Sentra_Gateway_SRS.md)
- [Java concept explanations](docs/JAVA_CONCEPT_EXPLANATIONS.md)

## Testing

Most components include automated tests and local verification assets.

- Java services: Maven test suites and Javadoc generation
- Frontend: lint, typecheck, and Vitest coverage
- Services: Postman collections and Newman-friendly environments
- Workspace: PowerShell smoke tests and startup scripts

## Notes For GitHub

- This repository is structured as a multi-service workspace rather than a single deployable application.
- Each service has its own contract, configuration, and runtime documentation.
- The internal services are intended to be reached through the gateway in normal operation.

## License

Licensed under the terms of the included [LICENSE](LICENSE) file.
