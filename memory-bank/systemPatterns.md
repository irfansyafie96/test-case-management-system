# System Patterns: Test Case Management System

## System Architecture

### Layered Architecture
- **Presentation Layer**: Static HTML/CSS/JS frontend served from resources/static
- **API Layer**: Spring Web REST controllers with JSON endpoints
- **Service Layer**: Business logic coordination in TcmService
- **Data Layer**: Spring Data JPA repositories with H2 database

### Hierarchical Domain Model
```
Project (1) ──── (N) TestModule (1) ──── (N) TestSuite (1) ──── (N) TestCase
                                                             └──── (M) TestStep
                                                             └──── (M) TestExecution
                                                                    └──── (M) TestStepResult
```

## Key Technical Decisions

### Entity Relationships
- **@OneToMany/@ManyToOne**: Used throughout for hierarchical navigation
- **CascadeType.ALL**: Applied to ownership relationships (TestCase→TestStep, TestExecution→TestStepResult)
- **orphanRemoval = true**: Ensures child entities are deleted when parent is removed
- **FetchType.LAZY**: Default fetch strategy to avoid N+1 query problems

### ORM Patterns
- **Field-based access**: @Entity annotations applied to fields
- **Custom @OrderBy**: Uses "testStep.stepNumber ASC" for consistent step ordering
- **JSON serialization control**: @JsonBackReference/@JsonManagedReference to prevent circular references

### REST API Design
- **Resource-based URLs**: /api/projects/{id}/testmodules, /api/executions/{id}/steps/{stepId}
- **HTTP method semantics**: GET (read), POST (create), PUT (update), DELETE (delete)
- **Consistent error responses**: RuntimeException mapped to 404, other exceptions to 500

## Component Relationships

### Controller-Service-Repository Pattern
```
ApiController → TcmService → [Repository Interfaces] → H2 Database
     ↓              ↓
   REST API     Business Logic
```

### Key Components
- **ApiController**: 23 REST endpoints covering full CRUD operations
- **TcmService**: Orchestrates business operations across repositories
- **Repository Layer**: Spring Data JPA interfaces with query method DSL
- **Model Layer**: JPA entities with proper relationships and validations

## Critical Implementation Paths

### Test Execution Flow
1. Create TestExecution instance (sets execution timestamp)
2. Initialize TestStepResult records for each TestStep in sequence
3. Update step results during execution (status: "Pass", "Fail", "Skipped")
4. Complete execution with overall result and notes

### Hierarchical Navigation
- **Top-down**: Project → TestModule → TestSuite → TestCase → TestSteps
- **Execution focus**: TestCase → TestExecution (history) → TestStepResults (detailed)

## Design Patterns Applied

### Repository Pattern
- Interface-based repositories extending JpaRepository
- Query method naming conventions for dynamic queries
- Separation of data access from business logic

### Service Layer Pattern
- Single TcmService coordinates all business operations
- Transaction boundaries managed implicitly by Spring
- Exception handling and validation logic centralized

### Factory Pattern (Implicit)
- Repository interfaces provide entity creation/query capabilities
- Service methods create complex entity relationships (test cases with steps)

## Data Flow Patterns

### Write Operations
- **Validation**: Service layer validates relationships and constraints
- **Persistence**: JPA cascades handle related entity persistence
- **Response**: JSON serialization with managed references

### Read Operations
- **Lazy Loading**: Related entities loaded on-demand
- **Dto Pattern**: Controllers return full entities (no DTOs currently)
- **Serialization**: Jackson handles complex nested relationships
