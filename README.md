# Test Case Management System

A modern, full-stack web application designed to revolutionize how quality assurance teams organize, execute, and track software testing activities. This system transforms the traditional spreadsheet-based approach to test management into a structured, collaborative, and efficient workflow.

## Current Status

- **Sprint 1**: ✅ Completed
- **Testing**: ✅ 32/32 tests passed
- **Database**: ✅ MariaDB 11.4.9 LTS (migrated from XAMPP)
- **Deployment**: Ready to deploy
- **Version**: 1.0.0

## Technology Stack

### Backend
- **Spring Boot 3.2.0** - Robust Java framework for enterprise applications
- **Java 17** - Modern Java with long-term support
- **Spring Security** - JWT-based authentication and authorization
- **Spring Data JPA** - Database abstraction with Hibernate
- **MariaDB 11.4.9 LTS** - Production-ready relational database (LTS until May 2029)
- **Apache POI** - Excel import/export functionality

### Frontend
- **Angular 21** - Modern framework with standalone components
- **Angular Material** - Consistent, accessible UI components
- **RxJS** - Reactive state management
- **TypeScript** - Type-safe development
- **SCSS** - Custom theming with dark mode support

## Key Features

### 1. Organization & Team Management
- Multi-organization support with isolated workspaces
- Team invitations with role-based access control
- User management with configurable permissions

### 2. Project Hierarchy
- **Projects** - Top-level containers for testing initiatives
- **Modules** - Functional area segmentation within projects
- **Submodules** - Fine-grained organization for test cases
- **Test Cases** - Detailed test specifications with steps and expected results

### 3. Test Execution Workbench
- Interactive execution interface with step-by-step guidance
- Real-time status tracking (Pass/Fail/Blocked)
- Execution assignment and completion tracking
- Completion summary dialog with results overview

### 4. Redmine Integration
- Direct Redmine issue creation from failed test executions
- Pre-filled subject and description from test case data
- Issue URL tracking and display
- Manual link input for existing Redmine tickets

### 5. Excel Import/Export
- Batch test case creation via Excel templates
- Hierarchical data import (Submodule → Test Case → Steps)
- Automatic execution generation for imported test cases
- Transaction rollback on import errors

### 6. Analytics & Reporting
- Real-time dashboard with testing progress metrics
- Pass/Fail/Not executed visualization
- Project-level coverage tracking
- Execution history and trend analysis

### 7. Permission System
- **Organization-based isolation** - Users only see their organization's data
- **Module-level assignments** - QA/BA users can only edit assigned modules
- **Role-based access** - Admin, QA, BA, Tester roles with specific capabilities
- **READ/WRITE separation** - All users can view, only assigned users can edit

## Getting Started

### Prerequisites
- Java Development Kit 17 or later
- Node.js 18 or higher with npm
- Maven 3.9 or newer
- MariaDB 11.4+ or MySQL 8.0+

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/irfansyafie96/test-case-management-system.git
   cd test-case-management-system
   ```

2. **Database Setup**
   - Install MariaDB 11.4.9 LTS or MySQL 8.0+
   - Create database: `CREATE DATABASE testcasedb CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;`
   - Update `src/main/resources/application.properties` with your database credentials

3. **Backend Setup**
   ```bash
   # Build the project
   mvn clean install
   
   # Run the application
   mvn spring-boot:run
   ```
   The backend server starts on `http://localhost:8080`

4. **Frontend Setup**
   ```bash
   cd tcm-frontend
   
   # Install dependencies
   npm install
   
   # Start development server
   npm start
   ```
   The application interface is available at `http://localhost:4200`

5. **Initial Configuration**
   - Register a new organization or use default admin credentials
   - Create your first project
   - Add modules and submodules to organize your test cases
   - Import test cases from Excel or create them manually

## Security Features

- **JWT Authentication** - Stateless authentication with HttpOnly cookies
- **CSRF Protection** - Cross-site request forgery prevention
- **Role-Based Access Control** - Fine-grained permissions per user role
- **Organization Isolation** - Complete data separation between organizations
- **Module-Level Access** - Edit permissions restricted to assigned modules
- **Input Validation** - Server-side validation on all endpoints
- **Secure Password Storage** - BCrypt hashing

## User Roles

### Administrator
- Full system configuration
- User and team management
- Project and module creation
- Access to all analytics and reports

### Quality Assurance Engineer
- Create and manage test cases
- Execute assigned tests
- Import/export test cases
- View execution history

### Business Analyst
- Review and approve test cases
- Requirements traceability
- Stakeholder reporting
- View execution results

### Tester
- Execute assigned test cases
- Record test results
- View personal execution history

## API Endpoints

### Authentication
```
POST   /api/auth/login           # User login
POST   /api/auth/register        # User registration
POST   /api/auth/register-org    # Organization registration
POST   /api/auth/join            # Join organization via invitation
GET    /api/auth/check           # Check authentication status
```

### Projects
```
GET    /api/projects                          # List projects
POST   /api/projects                          # Create project
GET    /api/projects/{id}                     # Get project details
PUT    /api/projects/{id}                     # Update project
DELETE /api/projects/{id}                     # Delete project
POST   /api/projects/{id}/assign/{userId}     # Assign user to project
```

### Modules
```
GET    /api/projects/{id}/modules             # List project modules
POST   /api/projects/{id}/modules             # Create module
GET    /api/modules/{id}                      # Get module details
PUT    /api/modules/{id}                      # Update module
DELETE /api/modules/{id}                      # Delete module
```

### Submodules
```
GET    /api/modules/{id}/submodules           # List module submodules
POST   /api/modules/{id}/submodules           # Create submodule
GET    /api/submodules/{id}                   # Get submodule details
PUT    /api/submodules/{id}                   # Update submodule
DELETE /api/submodules/{id}                   # Delete submodule
```

### Test Cases
```
GET    /api/submodules/{id}/testcases         # List test cases
POST   /api/submodules/{id}/testcases         # Create test case
GET    /api/testcases/{id}                    # Get test case details
PUT    /api/testcases/{id}                    # Update test case
DELETE /api/testcases/{id}                    # Delete test case
```

### Executions
```
GET    /api/executions                        # List executions
GET    /api/executions/{id}                   # Get execution details
PUT    /api/executions/{id}/complete          # Complete execution
PUT    /api/executions/{id}/steps/{stepId}    # Update step result
GET    /api/executions/my-assignments          # My assigned executions
GET    /api/modules/{id}/executions            # Module executions
POST   /api/modules/{id}/regenerate-executions # Regenerate executions
```

### Import/Export
```
POST   /api/import/excel/{submoduleId}        # Import from Excel
GET    /api/export/template                   # Download Excel template
```

### Analytics
```
GET    /api/analytics                         # Get analytics data
GET    /api/analytics/summary                 # Get completion summary
```

## Database Schema

```
User ──┬── Organization
       │
       ├── Project ──┬── TestModule ──┬── TestSubmodule ──┬── TestCase ──┬── TestExecution
       │             │                 │                    │               │
       │             │                 │                    │               └── TestStepResult
       │             │                 │                    │
       │             │                 │                    └── TestStep
       │             │                 │
       │             │                 └── (Additional Submodules)
       │             │
       │             └── (Additional Modules)
       │
       └── Role (ADMIN, QA, BA, TESTER)
```

## Development

### Building for Production

**Backend:**
```bash
mvn clean package -Pprod
```

**Frontend:**
```bash
cd tcm-frontend
npm run build
```

### Environment Variables

Create `.env` file in project root:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/testcasedb
DB_USERNAME=root
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION=86400000

# Admin User (Default)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
ADMIN_EMAIL=admin@example.com

# Frontend URL (for invitations)
TCM_APP_FRONTEND_URL=http://localhost:4200
```

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for detailed deployment instructions to DigitalOcean or other cloud platforms.

### Deployment Checklist
- [ ] Configure production environment variables
- [ ] Set up production database
- [ ] Build backend JAR file
- [ ] Build frontend dist files
- [ ] Configure Nginx reverse proxy
- [ ] Set up SSL certificate
- [ ] Configure systemd service
- [ ] Test all features end-to-end

## Testing

### Test Coverage
- ✅ 32/32 tests passing
- ✅ Redmine integration (17 tests)
- ✅ QA/BA permissions (4 tests)
- ✅ Excel import/export
- ✅ Test execution workflow
- ✅ Analytics and reporting

### Running Tests
```bash
# Backend tests
mvn test

# Frontend tests
cd tcm-frontend
npm test
```

## Known Issues

None - all issues resolved as of Sprint 1 completion.

## Roadmap

### Completed (Sprint 1)
- ✅ Redmine integration
- ✅ Excel import/export
- ✅ JWT authentication
- ✅ Organization management
- ✅ Role-based permissions
- ✅ Test execution workbench
- ✅ Analytics dashboard
- ✅ Security enhancements (CSRF, HttpOnly cookies)

### Future Enhancements
- Test execution scheduling
- Advanced analytics and custom reports
- Mobile application for on-the-go testing
- API test integration
- Performance test management
- AI-powered test generation

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/irfansyafie96/test-case-management-system).

---

**Last Updated**: February 5, 2026  
**Version**: 1.0.0  
**Status**: Production Ready