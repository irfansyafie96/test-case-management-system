# Technical Context: Test Case Management System

## Technology Stack

### Core Framework
- **Spring Boot 3.2.0**: Main application framework
- **Java 17**: Programming language specification
- **Maven**: Build and dependency management tool

### Web Layer
- **Spring Web MVC**: REST API implementation
- **Jackson**: JSON serialization/deserialization
- **Static HTML/CSS/JS**: Frontend implementation

### Data Layer
- **Spring Data JPA**: Data access abstraction
- **Hibernate 6.3.1**: JPA implementation and ORM
- **H2 Database 2.2.224**: In-memory database for development/runtime

### Additional Dependencies
- **HikariCP 5.0.1**: Database connection pooling
- **Jakarta Persistence 3.1.0**: JPA API specification
- **Tomcat 10.1.16**: Embedded web server

## Development Environment

### IDE Support
- **IntelliJ IDEA Ultimate**: Primary development environment
- **Maven integration**: Built-in project management and building

### Runtime Configuration
- **H2 Console**: Available at `/h2-console` for database inspection
- **Port 8080**: Default web server port
- **In-memory database**: Fresh database on each application restart

## Key Configuration Files

### application.properties (src/main/resources/)
```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server Configuration
server.port=8080
```

## Development Workflow

### Local Development
- Build: `mvn clean compile`
- Run: `mvn spring-boot:run` or IDE run/debug
- Database Access: Navigate to `http://localhost:8080/h2-console`
- API Access: `http://localhost:8080/api/*` endpoints
- Static Content: `http://localhost:8080/*.html`

### Testing
- **Unit Tests**: JUnit 5 through Spring Boot Test starter
- **Integration Tests**: Full application context testing
- **Database Tests**: In-memory H2 for isolated testing

## Build and Deployment

### Maven Build Process
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```

### Packaging
- **JAR packaging**: Spring Boot executable JAR
- **Embedded Tomcat**: No external web server required
- **Single executable**: Includes all dependencies

## Known Technical Constraints

### Database Limitations
- **In-memory storage**: Data persists only during application runtime
- **No file-based persistence**: Database resets on application restart
- **Single connection mode**: Not suitable for concurrent access patterns

### Architecture Considerations
- **Static frontend**: No server-side rendering or dynamic routing
- **Direct entity exposure**: No DTO layer for API responses
- **Lazy loading**: Potential N+1 query issues in complex relationships

### Performance Considerations
- **H2 in-memory**: Fast for development but not production-ready
- **Single-threaded design**: No concurrency handling in current implementation
- **No caching layer**: All data fetched from database on each request
