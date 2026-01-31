# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Sprint 1: Critical Security Fixes & Redmine Integration** (Week 1-2)
- **Sprint 2: Code Quality & Simple Deployment Prep** (Week 2)
- Production-ready deployment using JAR + Nginx approach (simplest for learning)

## Sprint 1 Changes (COMPLETED - Backend & Partial Frontend)

### Security Fixes (COMPLETED):
1. **InvitationService Frontend URL Fix**:
   - **Change**: Made frontend URL configurable via environment variable
   - **Files**: `InvitationService.java`
   - **Details**: Added `@Value("${tcm.app.frontendUrl:http://localhost:4200}")` with default for development
   - **Impact**: Invitation links now work in production without code changes

2. **AuthController Cookie Security**:
   - **Change**: Secure cookies for production environments
   - **Files**: `AuthController.java`
   - **Details**: 
     - `setSecure(environment.acceptsProfiles("prod"))` - Only secure in production
     - `setAttribute("SameSite", "Strict")` - CSRF protection
   - **Impact**: Cookies are properly secured for production HTTPS

3. **WebSecurityConfig CSRF Documentation**:
   - **Change**: Added clear documentation explaining CSRF configuration
   - **Files**: `WebSecurityConfig.java`
   - **Details**: Explained that CSRF is disabled because using stateless JWT authentication
   - **Impact**: Clear understanding for future maintenance

### Redmine Integration (COMPLETED - Backend, IN PROGRESS - Frontend):

**Backend Changes (COMPLETED)**:
1. **TestExecution Entity**:
   - **Change**: Added Redmine integration fields
   - **Files**: `TestExecution.java`
   - **New Fields**:
     - `redmineIssueId` (String) - Stores Redmine issue ID
     - `redmineIssueUrl` (String) - Direct link to Redmine issue
     - `bugReportSubject` (String) - Bug report title
     - `bugReportDescription` (String) - Detailed bug description
   - **Impact**: Test executions can now track Redmine issue references

2. **ExecutionService**:
   - **Change**: Updated `completeTestExecution()` to accept Redmine fields
   - **Files**: `ExecutionService.java`
   - **Details**: Added parameters for Redmine data, stores when provided
   - **Impact**: Redmine data is saved when completing failed executions

3. **ApiController**:
   - **Change**: Updated execution completion endpoint to handle Redmine fields
   - **Files**: `ApiController.java`
   - **Details**: Updated `/api/executions/{executionId}/complete` endpoint
   - **Impact**: Frontend can send Redmine data to backend

4. **ExecutionCompleteRequest DTO**:
   - **Change**: Added Redmine fields to request DTO
   - **Files**: `ExecutionCompleteRequest.java`
   - **New Fields**: `bugReportSubject`, `bugReportDescription`, `redmineIssueUrl`
   - **Impact**: Data transfer for Redmine integration

**Frontend Changes (IN PROGRESS)**:
1. **RedmineIssueDialogComponent** (COMPLETED):
   - **Files**: `redmine-issue-dialog.component.ts/html/css`
   - **Features**:
     - Pre-filled subject with test case ID and title
     - Auto-generated description with test steps and results
     - "Open in Redmine" button - opens pre-filled Redmine form
     - "Save Only" button - saves data without opening Redmine
     - Manual link input for pasting Redmine issue URL
   - **Impact**: Users can easily create Redmine issues for failed tests

2. **ExecutionWorkbenchComponent** (COMPLETED):
   - **Change**: Added Redmine button and dialog integration
   - **Files**: `execution-workbench.component.ts/html/css`
   - **Status**: Component created and integrated

3. **TestExecution Model** (COMPLETED):
   - **Change**: Added Redmine fields to frontend interface
   - **Files**: `project.model.ts`
   - **Status**: Updated with all Redmine fields

## Previous Changes (Before Sprint 1):

- **Test Case Detail Navigation (COMPLETED)**: Added Next/Prev navigation across all submodules
- **Test Analytics Display Fix (COMPLETED)**: Separated `status` and `overallResult` fields
- **Execution Workbench Completion Fix (COMPLETED)**: Stay on page after completion
- **QA/BA Deletion Permissions (COMPLETED)**: Allow QA/BA to delete within assigned modules
- **Excel Import Template Updated (COMPLETED)**: Regenerated with correct terminology
- **QA Execution Save Failure (RESOLVED)**: Fixed query to fetch all relationships
- **Execution Workbench UI Polish (COMPLETED)**: Standardized button styles
- **Automatic Execution Generation (COMPLETED)**: Auto-create on assignment
- **Project Access Logic Fix (COMPLETED)**: Module-level users can view parent projects
- **Execution Filter Logic Fix (COMPLETED)**: Admin filtering by user now works

## Sprint 2 Tasks (PENDING):

### Code Quality Improvements:
1. **Create OrganizationSecurityUtil** - Extract 18 repeated organization checks
2. **Create DTOMapperService** - Extract DTO conversions from ApiController
3. **Create Custom Exception Classes** - ResourceNotFound, AccessDenied, Duplicate, Validation
4. **Refactor AnalyticsService** - Extract methods from long `getTestAnalytics()` method

### Deployment Preparation:
1. **Delete Unnecessary Files**:
   - `07 TEST SCENARIOS_CLAIMS_MANAGEMENT_FINAL TESTING HRDC_NCS_.xlsx` (legacy template) - DELETED
   - `replay_pid8848.log` (temporary log) - DELETED
   - `apache-maven-3.9.8/` (Maven installation) - KEEPING (Chocolatey PATH issue)
   - `tcm.iml` (IntelliJ file) - KEEPING (using IntelliJ)

2. **Create Configuration Files**:
   - `application-prod.properties` - Production configuration
   - `.env.example` - Environment variable documentation
   - `DEPLOYMENT.md` - Step-by-step JAR + Nginx deployment guide

### Learning-Focused Deployment:
- **Approach**: JAR file + Nginx (simple, easy to understand)
- **Target**: DigitalOcean Droplet with Ubuntu
- **Database**: Managed MySQL or self-hosted
- **SSL**: Let's Encrypt with certbot

## Development Setup Issues & Solutions

### Chocolatey Installation Issue (Documented):
- **Problem**: Chocolatey was previously installed but `choco` command not recognized
- **Error**: `choco : The term 'choco' is not recognized as the name of a cmdlet, function, script file, or operable program`
- **Root Cause**: Chocolatey installed but not added to system PATH
- **Location**: `C:\ProgramData\chocolatey\bin\choco.exe` exists but not in PATH
- **Attempted Solutions**:
  1. Closed and reopened PowerShell - didn't work
  2. Checked `C:\ProgramData\chocolatey` - exists
  3. Winget search for Apache Maven - not available
- **Decision**: Keep using local Maven (`apache-maven-3.9.8/`) for now
- **Future Solutions**:
  1. Add `C:\ProgramData\chocolatey\bin` to PATH manually
  2. Use full path: `C:\ProgramData\chocolatey\bin\choco.exe install maven`
  3. Reinstall Chocolatey in new session

### Maven Setup:
- **Current**: Using local Maven installation at `apache-maven-3.9.8/`
- **Status**: Working fine for development
- **Notes**: Not tracked by git, can be used until PATH issue resolved

## Known Issues
- **None currently**

## Important Decisions & Considerations

### Architecture:
- **Admin Visibility**: Admins see all resources in their organization
- **Deletion Strategy**: Use domain services for complex deletions
- **Entity Equality**: Use ID-based equals/hashCode for collections
- **DTOs**: Flattened DTOs preferred for frontend
- **Frontend Models**: `project.model.ts` is source of truth
- **JPA Optimization**: Use explicit LEFT JOIN FETCH for permission checks
- **Status vs Result**: Always separate fields (PASSED/FAILED vs PENDING/COMPLETED)

### Security:
- **JWT Authentication**: Stateless, no session management
- **CSRF**: Disabled for stateless JWT APIs (documented)
- **Cookie Security**: Secure + SameSite in production
- **Environment Variables**: All secrets configurable via env vars

### Learning Principles:
- **DRY**: Extract repeated patterns (organization checks, DTO conversions)
- **Simplicity**: Use JAR + Nginx (easier to learn than Docker initially)
- **Documentation**: Clear comments for codebase learning
- **Modularity**: Keep components focused and single-purpose

### Redmine Integration:
- **Approach**: URL pre-filling (simple, no API key needed)
- **Trigger**: Manual button (user chooses when to create issue)
- **Flow**: Fill form → Open Redmine → Paste link back
- **Future**: Could upgrade to API integration when needed

## Next Steps (When Continuing):
1. Complete Sprint 1 remaining tasks (Tasks 11-16)
2. Create code quality utilities (Tasks 11-13)
3. Create deployment configuration (Tasks 14-16)
4. Update memory bank with all changes (Task 17)
5. Test Redmine integration end-to-end
6. Deploy to DigitalOcean (after Sprint 2 completion)