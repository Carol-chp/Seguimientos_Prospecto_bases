# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

This is a Spring Boot 3.4.1 application using Java 23 and Maven.

- **Run the application**: `./mvnw spring-boot:run` or `mvn spring-boot:run`
- **Build the project**: `./mvnw clean compile` or `mvn clean compile`
- **Package the application**: `./mvnw clean package` or `mvn clean package`
- **Run tests**: `./mvnw test` or `mvn test`

The application runs on port 8081 by default (configured in application.properties).

## Database Setup

The application uses PostgreSQL database:
- **Database**: `seguimiento_prospectos`
- **Host**: localhost:5432
- **Default credentials**: postgres/123456 (configured in application.properties)
- **Schema**: Located in `BD/prospectos.sql`

## Architecture Overview

This is a prospect tracking system (seguimiento de prospectos) with the following key components:

### Core Entities
- **Prospecto**: Main entity representing prospects with personal information, contact details, and campaign associations
- **Campania**: Campaign entity linked to prospects
- **CargaMasiva**: Bulk load tracking for Excel imports
- **Contacto**: Contact tracking
- **Asignacion**: Assignment management
- **Usuario**: User management with roles

### Security
- JWT-based authentication using jjwt library (version 0.11.5)
- Role-based authorization with TELEOPERADOR and ADMINISTRADOR roles
- Spring Security configuration in `Config/SecurityConfig.java`
- Custom JWT filter in `Config/JwtAuthenticationFilter.java`

### Key Features
- **Excel Import**: Bulk prospect import via Base64-encoded Excel files through `/api/prospectos/importar`
- **Prospect Search**: Paginated search with campaign filtering via `/api/prospectos/busqueda`
- **Email Support**: Spring Mail integration configured
- **File Upload**: Supports up to 10MB files for Excel imports

### Package Structure
- `Controller/`: REST API endpoints
- `Service/`: Business logic layer
- `Repository/`: JPA repositories
- `Entity/`: JPA entities
- `Config/`: Security, CORS, Mail, and JWT configuration
- `dto/`: Data transfer objects
- `helper/`: Utility classes (ExcelHelper for file processing)

## Important Dependencies
- Spring Boot 3.4.1 with Web, Data JPA, Security, Mail
- PostgreSQL driver
- Apache POI 5.2.3 for Excel processing
- Lombok for boilerplate code reduction
- JWT libraries for authentication

## API Testing
HTTP request examples are available in the `requests/` directory:
- `auth.http`: Authentication endpoints
- `busqueda.http`: Search functionality

## Development Notes
- Logging is extensively configured for Hibernate SQL debugging
- CORS is configured for cross-origin requests
- Uses Lombok annotations (@Getter, @Setter, @RequiredArgsConstructor)
- JPA entities follow standard naming conventions with table mappings