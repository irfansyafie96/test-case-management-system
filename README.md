# Test Case Management System (TCM)

A comprehensive, full-stack web application for managing software testing life cycles. The system enables QA teams to organize test cases into hierarchical structures (Projects → Modules → Submodules → Test Cases), execute tests with real-time tracking, integrate with Redmine for bug tracking, and generate analytics reports.

---

## 🚀 Current Status

| Metric | Status |
|--------|--------|
| **Sprint 1** | ✅ Complete |
| **Sprint 2 (Refactoring)** | 20% Complete |
| **Testing** | ✅ 32/32 Tests Passed |
| **Database** | ✅ MariaDB 11.4.9 LTS |
| **Deployment** | Ready for Production |
| **Version** | 1.0.0 |

---

## 🛠 Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Spring Boot** | 3.2.0 | Enterprise Java framework |
| **Java** | 17 (JDK 25 for dev) | Core programming language |
| **Spring Security** | 6.x | Authentication & authorization |
| **Spring Data JPA** | 3.2.0 | Database abstraction with Hibernate |
| **MariaDB** | 11.4.9 LTS | Relational database (LTS until May 2029) |
| **Apache POI** | 5.x | Excel import functionality |
| **JWT** | jjwt | Stateless authentication |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| **Angular** | 21 | Frontend framework with standalone components |
| **Angular Material** | 21 | UI component library |
| **TypeScript** | 5.x | Type-safe development |
| **RxJS** | 7.x | Reactive state management |
| **SCSS** | - | Custom theming |
| **jsPDF** | - | PDF generation for reports |

### Development Tools
| Tool | Purpose |
|------|---------|
| **IntelliJ IDEA 2025.2.2** | IDE |
| **Maven 3.9.8** | Build tool |
| **Node.js 21** | Frontend development |
| **HeidiSQL** | Database GUI (bundled with MariaDB) |

---

## 📋 User Roles & Permissions

The system implements a role-based access control (RBAC) with organization-level isolation:

| Role | Capabilities |
|------|-------------|
| **ADMIN** | Full system access, all projects, user management, role changes |
| **PROJECT_MANAGER** | Manage assigned projects, create/delete modules, invite team members |
| **QA** | Create/edit test cases in assigned modules, execute tests, import Excel |
| **BA** | Review test cases, execute tests in assigned modules |
| **TESTER** | Execute assigned tests only, read-only access to test cases |

### Permission Matrix

| Capability | ADMIN | PM | QA/BA | TESTER |
|------------|-------|-----|-------|--------|
| View All Org Projects | ✅ | Assigned Only | - | - |
| Create Modules | ✅ | Assigned Projects | - | - |
| Edit Modules | ✅ | Assigned Projects | Assigned Only | - |
| Delete Modules | ✅ | Assigned Projects | - | - |
| View Team | ✅ | Assigned Projects | - | - |
| Invite Members | ✅ | Assigned Projects | - | - |
| Assign Modules | ✅ | ✅ | ✅ | - |
| Execute Tests | ✅ | ✅ | ✅ | ✅ |
| View Executions | All Org | Assigned Projects | Assigned Modules | Assigned Modules |
| View Analytics | All Org | Assigned Projects | Assigned Modules | Assigned Modules |
| Import Test Cases | ✅ | ✅ | ✅ | - |

---

## ✨ Key Features

### 1. Organization & Team Management
- Multi-organization support with isolated workspaces
- Team invitations with role-based access control
- Project-specific team assignments
- User management with configurable permissions

### 2. Project Hierarchy
```
Organization
    └── Project
        └── Test Module (functional area)
            └── Test Submodule (feature grouping)
                └── Test Case
                    └── Test Steps
                        └── Test Execution Results
```

### 3. Test Execution Workbench
- Interactive step-by-step execution interface
- Real-time status tracking (Pass/Fail/Blocked)
- Execution assignment to team members
- Completion summary with results overview

### 4. Test Cycles/Phases
- Create testing phases (e.g., "Phase 1 - UAT", "Phase 2 - Regression")
- Link phases to Redmine projects for ticket creation
- Auto-assign active phase when completing executions
- Filter executions by phase

### 5. Redmine Integration
- Direct Redmine issue creation from failed test executions
- Pre-filled subject and description from test case data
- Multiple Redmine issues per failed execution
- Manual link input for existing tickets
- Ticket status tracking (Open/Closed)
- Audit trail for ticket changes

### 6. Ticket Management Page
- View all tickets across projects
- Filter by Project, Phase, Status
- Toggle ticket status (Open ↔ Closed)
- View in Redmine (opens in new tab)
- View Audit Trail (history of changes)
- Edit ticket details directly

### 7. Excel Import/Export
- Batch test case creation via Excel templates
- Hierarchical data import (Submodule → Test Case → Steps)
- Automatic execution generation
- Transaction rollback on import errors

### 8. Analytics & Reporting
- Real-time dashboard with testing metrics
- Pass/Fail/Not Executed visualization
- Project and module-level coverage tracking
- Filter by User, Project, Module, Phase
- **PDF Export** - Generate colorful PDF reports

### 9. Mobile Responsiveness
- Hamburger menu for mobile navigation
- Slide-out drawer sidebar
- Card-based layouts for tables on mobile
- Responsive dialogs

---

## 🏗 Architecture

### Design Patterns

1. **Domain-Driven Design (DDD)**
   - Monolithic `TcmService` (2003 lines) refactored into 8 domain services:
     - `ProjectService` - Project lifecycle
     - `ModuleService` - Module operations
     - `SubmoduleService` - Submodule management
     - `TestCaseService` - Test case lifecycle
     - `ExecutionService` - Test execution workflow
     - `AnalyticsService` - Reporting
     - `ImportExportService` - Excel operations
     - `UserService` - User management

2. **DTO Pattern**
   - Data Transfer Objects for API responses
   - Prevents JSON serialization issues with circular references
   - Decouples internal models from external API

3. **SecurityHelper**
   - Centralized permission checks (eliminates 53+ duplicate checks)
   - Single source of truth for authorization logic

4. **Blueprint Modal Pattern**
   - Multi-step dialogs with clear visual hierarchy
   - Input → Loading → Result states

5. **Specification Grid Pattern**
   - Two-column layout for detailed views
   - Fixed-width label column, flexible value column

### Security Features

- **JWT Authentication** - Stateless with HttpOnly cookies
- **CSRF Protection** - Configurable per environment
- **Role-Based Access Control** - Fine-grained permissions
- **Organization Isolation** - Complete data separation
- **Module-Level Access** - Edit permissions restricted to assignments
- **Server-Side Validation** - All endpoints validated
- **BCrypt Password Hashing** - Secure storage

---

## 📊 Project Complexity

### Code Quality Metrics
| Metric | Value |
|--------|-------|
| SecurityHelper | 255 lines (eliminates 53+ permission checks) |
| Refactored Services | 8 domain services from 1 monolithic service |
| Code Reduction (Sprint 2) | 20-30% expected from refactoring |
| Test Coverage | 32/32 tests passing |

### Backend Architecture
- **Layers**: Controller → Service → Repository → Entity
- **Transaction Management**: @Transactional annotations
- **Query Strategy**: @EntityGraph for controlling fetch strategy
- **Exception Handling**: @ControllerAdvice for global error handling
- **JPA Auditing**: Automatic createdDate, updatedDate, createdBy

---

## 📖 API Endpoints

### Authentication
```
POST   /api/auth/login           # User login
POST   /api/auth/register        # User registration
POST   /api/auth/register-org    # Organization registration
POST   /api/auth/join            # Join via invitation
GET    /api/auth/check           # Check auth status
GET    /api/auth/users            # List org users (Admin/PM)
GET    /api/auth/team-members     # Get team members
```

### Projects
```
GET    /api/projects                              # List projects
POST   /api/projects                              # Create project
GET    /api/projects/{id}                        # Get project
PUT    /api/projects/{id}                        # Update project
DELETE /api/projects/{id}                        # Delete project
GET    /api/projects/assigned-to-me              # My assigned projects
POST   /api/projects/{id}/assign/{userId}        # Assign user
POST   /api/projects/{id}/cycles                 # Create cycle
GET    /api/projects/{id}/cycles                 # List cycles
```

### Modules
```
GET    /api/projects/{id}/modules             # List modules
POST   /api/projects/{id}/modules             # Create module
GET    /api/modules/{id}                       # Get module
PUT    /api/modules/{id}                      # Update module
DELETE /api/modules/{id}                      # Delete module
GET    /api/modules/{id}/submodules           # List submodules
POST   /api/modules/{id}/submodules           # Create submodule
POST   /api/modules/{id}/regenerate-executions # Regenerate executions
```

### Submodules
```
GET    /api/submodules/{id}                   # Get submodule
PUT    /api/submodules/{id}                   # Update submodule
DELETE /api/submodules/{id}                   # Delete submodule
GET    /api/submodules/{id}/testcases         # List test cases
POST   /api/submodules/{id}/testcases         # Create test case
```

### Test Cases
```
GET    /api/testcases/{id}                    # Get test case
PUT    /api/testcases/{id}                    # Update test case
DELETE /api/testcases/{id}                    # Delete test case
GET    /api/testcases/{id}/executions         # Get executions
POST   /api/testcases/{id}/executions         # Create execution
```

### Executions
```
GET    /api/executions                        # List executions
GET    /api/executions/{id}                   # Get execution
PUT    /api/executions/{id}/complete          # Complete execution
PUT    /api/executions/{id}/steps/{stepId}    # Update step result
PUT    /api/executions/{id}/save              # Save work in progress
GET    /api/executions/my-assignments          # My assignments
POST   /api/executions/{id}/assign            # Assign execution
PUT    /api/executions/{id}/redmine           # Update Redmine link
```

### Admin
```
GET    /api/admin/executions                  # All org executions (with filters)
GET    /api/admin/users                        # All org users
GET    /api/admin/modules                      # All modules
```

### Cycles
```
GET    /api/cycles/{id}                       # Get cycle
PUT    /api/cycles/{id}                       # Update cycle
DELETE /api/cycles/{id}                       # Delete cycle
```

### Tickets
```
GET    /api/tickets                           # List tickets (with filters)
PUT    /api/tickets/{id}                     # Update ticket
PUT    /api/tickets/{id}/status               # Toggle status
GET    /api/tickets/{id}/audit                # Get audit trail
```

### Import/Export
```
POST   /api/import/excel/{submoduleId}        # Import from Excel
GET    /api/export/template                   # Download template
```

### Analytics
```
GET    /api/analytics                         # Get analytics data
GET    /api/analytics/summary                 # Get completion summary
```

---

## 🗄 Database Schema

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │       │  Organization │    │    Role     │
└──────┬──────┘       └───────────────┘    └─────────────┘
       │
       │ ┌─────────────┐       ┌─────────────┐
       └─┤   Project   │◄──────│ TestModule  │
         └──────┬──────┘       └──────┬──────┘
                │                      │
                │              ┌───────┴───────┐
                │              │               │
                │        ┌─────▼─────┐   ┌──────▼──────┐
                │        │ Submodule │   │TestCycle    │
                │        └─────┬─────┘   └─────────────┘
                │              │
                │        ┌─────▼─────┐
                └────────│ TestCase  │
                        └─────┬─────┘
                              │
                    ┌─────────┴─────────┐
                    │                 │
              ┌─────▼─────┐     ┌──────▼──────┐
              │ TestStep  │     │TestExecution│
              └───────────┘     └──────┬──────┘
                                         │
                                  ┌──────▼──────┐
                                  │RedmineIssue │
                                  └─────────────┘
```

### Key Tables
- `users` - User accounts with roles
- `organizations` - Multi-tenant workspaces
- `projects` - Top-level containers
- `test_modules` - Functional areas
- `test_submodules` - Feature groupings
- `test_cases` - Test specifications
- `test_steps` - Individual test steps
- `test_executions` - Execution records
- `test_cycles` - Testing phases
- `redmine_issues` - Linked Redmine tickets
- `module_editor_assignments` - QA/BA editing access
- `execution_assignees` - Tester execution access

---

## 🚦 Getting Started

### Prerequisites
- Java Development Kit 17+
- Node.js 18+
- Maven 3.9+
- MariaDB 11.4+ or MySQL 8.0+

### Installation

1. **Clone and Setup**
   ```bash
   git clone https://github.com/irfansyafie96/test-case-management-system.git
   cd test-case-management-system
   ```

2. **Database**
   ```sql
   CREATE DATABASE testcasedb CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```

3. **Backend**
   ```bash
   mvn clean install
   mvn spring-boot:run
   # Server starts on http://localhost:8080
   ```

4. **Frontend**
   ```bash
   cd tcm-frontend
   npm install
   npm start
   # App available at http://localhost:4200
   ```

---

## 📈 Testing

### Test Coverage
| Category | Tests | Status |
|----------|-------|--------|
| Redmine Integration | 17 | ✅ Pass |
| QA/BA Permissions | 4 | ✅ Pass |
| Excel Import | 1 | ✅ Pass |
| Test Execution | 6 | ✅ Pass |
| Analytics | 4 | ✅ Pass |
| **Total** | **32** | **✅ Pass** |

---

## 📝 Recent Changes

### Sprint 2 (Refactoring - In Progress)
- ✅ SecurityHelper for centralized permissions
- ✅ Module assignment dropdown fix
- ✅ Team management list fix
- ✅ Module deletion cascade fix
- ✅ Test Cases module filter fix
- ✅ PROJECT_MANAGER role implementation
- ✅ Role Display Pipe (DRY)

### Features Added
- ✅ Test Cycles/Phases with Redmine linkage
- ✅ Ticket Management Page
- ✅ PM/Admin Read-Only Mode
- ✅ Phase Auto-Assign on execution completion
- ✅ Cycle filter on Executions/Test Cases
- ✅ PDF Export for Analytics
- ✅ Mobile responsiveness improvements
- ✅ Ticket Edit functionality

---

## 🎯 Roadmap

### Completed (Sprint 1)
- ✅ Redmine integration (multi-issue support)
- ✅ Excel import/export
- ✅ JWT authentication
- ✅ Organization management
- ✅ Role-based permissions
- ✅ Test execution workbench
- ✅ Analytics dashboard
- ✅ Security enhancements

### Future Enhancements
- ⏳ Advanced analytics with custom reports
- ⏳ Test execution scheduling
- ⏳ Mobile application
- ⏳ API test integration
- ⏳ Performance test management

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

MIT License

---

## 📞 Contact

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/irfansyafie96/test-case-management-system).

---

**Last Updated**: March 10, 2026  
**Version**: 1.0.0  
**Status**: Production Ready
