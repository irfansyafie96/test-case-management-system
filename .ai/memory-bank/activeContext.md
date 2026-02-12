# Active Context - Test Case Management System

## Current Session: 2026-02-11

### User Context
- **User**: irfan
- **Role**: Senior Software Engineer / Developer
- **Communication**: English only
- **Commit Style**: Conventional commits (feat:, fix:, chore:, etc.)
- **IMPORTANT**: DO NOT commit changes directly. Only commit after user explicitly says so.

### Project Context
- **Type**: Full-stack web application (Spring Boot + Angular)
- **Purpose**: Test Case Management System with execution tracking
- **Organization**: TMS Asia
- **Status**: Sprint 1 completed, Database migrated to MariaDB 11.4

### Current Blocker: NONE ✅

### Recent Changes

24. **Module Assignment Checkbox Fix** (2026-02-11)
   - **Issue**: When assigning modules in the project team page, assigned modules were not being checked in the dialog despite being in the database (user_test_modules table)
   - **Root Cause**: Backend `UserDTO` didn't include `assignedTestModules` field, so the frontend couldn't determine which modules were assigned
   - **Changes**:
     - Backend: Added `assignedTestModules` field to `UserDTO` with getter/setter and updated constructors
     - Backend: Updated `UserRepository.findUsersAssignedToProject()` to include `assignedTestModules` in `@EntityGraph` attributePaths
     - Backend: Updated `ApiController.getUsersAssignedToProject()` to convert `assignedTestModules` entities to `TestModuleDTOs` and filter to only include modules belonging to the specified project
   - **Files**: `UserDTO.java`, `UserRepository.java`, `ApiController.java`
   - **SOLID/DRY**: ✅ Reuses existing `convertToDTO()` method, follows Single Responsibility
   - **Status**: COMPLETED ✅

23. **Module Creation Permission & Assignment Fixes** (2026-02-11)
   - **Goal**: Restrict module creation to ADMIN and fix bugs in module assignment dialog
   - **Changes**:
     - Backend:
       - Restricted `createTestModuleForProject` endpoint to `hasRole('ADMIN')`
       - Restricted `bulkAssignModules` endpoint to `hasRole('ADMIN')`
       - Updated `bulkAssignModules` to return the updated `UserDTO` (fixes "nothing happens" issue)
       - Refactored `ModuleService` to allow QA/BA to assign/remove users if they have edit access to the module
       - **Refined `isEditable` logic**: Restrict edit access to ADMIN, QA, or BA roles only; TESTER role now only has execution access even if assigned to a module.
       - **Fixed Compilation Error**: Replaced non-existent `hasAnyRole()` with `isQaOrBa()` in `ApiController.java`.
     - Frontend:
       - Updated `project-detail.component.html` to hide "NEW MODULE" from QA/BA
       - Refactored `ProjectTeamComponent` using **Refresh Trigger pattern** (Subject + switchMap) to solve `NG0100` errors and added success snackbar after module assignment.
       - Refactored `AssignModulesDialogComponent`:
         - **Fixed Selection State**: Replaced `[(ngModel)]` with explicit `[selected]` binding and `(selectionChange)` handler. This guarantees correct checkbox state even with async data loading, converting all IDs to strings for reliable comparison.
         - Implemented `setTimeout` in initial load and `cdr.detectChanges()` in save to ensure stable state.
         - Standardized module selection and improved `onSave` logic to properly filter assignments/removals.
   - **Status**: COMPLETED ✅
   - **Goal**: Simplify access model by moving module assignment to three-dot menu
   - **Changes**:
     - Backend:
       - Modified `ProjectService.removeUserFromProject()` to remove from project + all modules
       - Added `ModuleService.getModulesByProjectId()` method
       - Added `TestModuleRepository.findByProjectId()` query
       - Added `ApiController.getModulesByProject()` endpoint
       - Added `ApiController.bulkAssignModules()` endpoint
       - Created `BulkAssignmentRequest` DTO
     - Frontend:
       - Added `AssignModulesDialogComponent` with checkbox list
       - Added `getModulesAbbreviated()` and `getModulesTooltip()` helpers
       - Updated team list to show module names (abbreviated + tooltip)
       - Added "Assign Modules" option in three-dot menu
       - Updated "Remove" to completely revoke access
       - Added `bulkAssignModules()` service method
       - Added `getModulesByProject()` service method
       - Updated `removeUserFromProject()` to use new signature
     - Updated `/modules/{id}` page to use new backend methods
   - **Files**:
     - Backend: `ProjectService.java`, `ModuleService.java`, `TestModuleRepository.java`, `ApiController.java`, `BulkAssignmentRequest.java`
     - Frontend: `project-team.component.ts/html/css`, `assign-modules-dialog.component.ts/html/css`, `tcm.service.ts`, `project-detail.component.ts`
   - **SOLID/DRY**: ✅ Reuses existing service methods, follows Single Responsibility
   - **Status**: COMPLETED ✅

22. **Module Assignment Bug Fixes** (2026-02-11)
   - **Fixes**:
     - Removed "Manage Assignments" section from project-detail page
     - Changed button to "MANAGE TEAM" linking to `/projects/{id}/team`
     - Fixed `ExpressionChangedAfterItHasBeenCheckedError` in dialog (added ChangeDetectorRef)
     - Fixed `removeUserFromProject()` to handle plain text response
     - Fixed `bulkAssignModules()` field name from `moduleId` to `testModuleId` to match backend DTO
   - **Files**:
     - Frontend: `project-detail.component.html`, `assign-modules-dialog.component.ts`, `tcm.service.ts`
   - **Status**: COMPLETED ✅

20. **Project Team Members Loading Fix** (2026-02-11)
   - **Issue**: Team members didn't load initially - required user interaction to appear
   - **Root Cause**: Manual Observable subscription without change detection
   - **Solution**: Refactored to use async pipe pattern
   - **Changes**:
     - Removed manual `teamMembers[]` array subscription
     - Updated template to use `*ngIf="(teamMembers$ | async) as members"`
     - Added loading state template with spinner
   - **Files**: `project-team.component.ts`, `.html`, `.css`
   - **Status**: COMPLETED ✅

19. **Test Case Prev/Next Navigation Fix** (2026-02-11)
   - **Issue**: Prev/Next buttons on test-case-detail page worked inconsistently
   - **Root Cause**: `loadAllTestCases()` didn't trigger change detection after loading
   - **Fix**: Added `this.cdr.detectChanges()` in subscription callbacks (next and error)
   - **File**: `test-case-detail.component.ts`
   - **SOLID Analysis**: ✅ Follows Single Responsibility, Open/Closed, Dependency Inversion
   - **DRY Analysis**: ⚠️ Slight repetition (detectChanges called twice). Could use `.add()` for better DRY
    - **Status**: COMPLETED ✅

25. **Team Management Page Consistency Fix** (2026-02-12)
    - **Goal**: Align Team Management page layout with Project Detail page for consistency
    - **Changes**:
      - HTML (`project-team.component.html`):
        - Replaced simple `page-header` with `project-header-section` wrapper matching project-detail structure
        - Added breadcrumb with "Projects" link and icon at top-left
        - Added `dossier-header` and `dossier-title` with "Team Management" title and description
      - CSS (`project-team.component.css`):
        - Added `.breadcrumb` class with `align-items: center`, fonts, and spacing
        - Added `.project-header-section`, `.dossier-header`, `.dossier-title` styles
        - Removed local `.page-container` override to use global styles (`padding: 48px 64px`)
    - **Result**: Team page now has same top padding and header layout as Project Detail page
    - **Status**: COMPLETED ✅

### Recent Changes (Committed)

18. **Team Visibility & Relationship Fixes** (2026-02-11)
   - **Issue**: Admins weren't visible to team; new users weren't visible to Admins.
   - **Fixes**:
     - `UserRepository`: Query now includes all Organization Admins.
     - `InvitationService`: Explicitly updates both sides of `User-Project` relationship.
     - `ProjectService`: Auto-assigns project creator (Admin) to the new project.
   - **Status**: COMPLETED ✅

17. **Consolidated Project-Centric Team Management** (2026-02-11)
   - **Goal**: Move all team management to project pages and restrict visibility to project members.
   - **Changes**:
     - Removed Team tab from Profile page.
     - Enabled non-admin visibility for project team members.
     - Enhanced backend query to include module-assigned users in project team list.
     - Refactored invitation logic to support direct project assignment for existing users.
     - Removed all hover effects from project team list UI.
   - **Status**: COMPLETED ✅

15. **Guest User Team Visibility Fix** (2026-02-10)
   - **Issue**: Guest users only saw themselves in Team Members section even when assigned to projects
   - **Root Cause**: `assignUserToProject()` only updated one side of bidirectional relationship (user.assignedProjects) but not (project.assignedUsers)
   - **Impact**: The `findCollaborators()` query uses `p.assignedUsers` which was empty, so no collaborators were found
   - **Fix**: Added `project.getAssignedUsers().add(user)` in ProjectService.java line ~296
   - **Status**: COMPLETED ✅

14. **Team Members Visibility Fix** (2026-02-10)
   - **Issue**: Testers couldn't see team members on profile page - Team Members section was empty
   - **Root Cause**: Frontend only loaded team members when `this.isAdmin` was true, blocking non-admin users
   - **Solution**: 
     - Created new backend endpoint `GET /api/auth/team-members` (no ADMIN restriction)
     - Added `findAllUsersByOrganization()` query to fetch all org members including admins
     - Updated frontend to call new endpoint for all users
   - **Changes Made**:
     - Backend: `UserRepository.java` - Added `findAllUsersByOrganization()` query
     - Backend: `AuthController.java` - Added `/api/auth/team-members` endpoint
     - Frontend: `tcm.service.ts` - Added `getAllTeamMembers()` method
     - Frontend: `profile.component.ts` - Removed `if (this.isAdmin)` check in `loadTeamMembers()`
   - **Result**: All users (testers, QA, BA, admins) can now see all team members on their profile
   - **Status**: COMPLETED ✅

1. **TestCase isEditable Flag Implementation** (2026-02-04)
   - **Issue**: Edit button shown for all QA/BA users regardless of module assignment
   - **Solution**: Added `isEditable` flag to TestCaseDTO to control edit button visibility
   - **Changes Made**:
     - Added `isEditable` field to `TestCaseDTO` with `@JsonProperty("isEditable")` annotations
     - Modified `ApiController.getTestCaseById()` to set `isEditable` based on module assignment
     - Updated frontend `TestCase` interface to include `isEditable?: boolean`
     - Updated `test-case-detail.component.html` to use `testCase.isEditable` condition
   - **Permission Logic**: Edit button shows if user is admin OR assigned to the test case's parent module
   - **Pattern**: Follows the same pattern as TestModule for consistent permission handling
   - **Code Reduction**: Cleaner implementation with module-based edit control
   - **Status**: COMPLETED ✅

2. **QA User Test Case Viewing Permission Fix** (2026-02-04)
   - **Issue**: QA users couldn't navigate to test cases in unassigned modules, getting "Failed to load test case details" error
   - **Root Cause**: `getTestCaseById()` required project/module assignment for VIEWING (too restrictive)
   - **Solution**: Removed assignment checks for VIEWING, allowing all org members to view test cases
   - **Changes Made**:
     - Modified `TestCaseService.getTestCaseById()` to only enforce organization boundary
     - Removed project/module assignment checks for READ operations
     - Kept existing `requireModuleAccess()` checks in update/delete methods
   - **Result**: QA users can now view test cases from any module in their organization
   - **Pattern**: Aligns with module viewing where READ access is org-wide, WRITE access is assignment-based
   - **Date Tested**: 2026-02-05
   - **Status**: COMPLETED ✅

2. **Database Migration: XAMPP to MariaDB 11.4** (2026-02-04)
   - **Issue**: XAMPP MySQL kept failing with "MySQL shutdown unexpectedly" due to corrupted Aria logs and data files
   - **Solution**: Migrated to standalone MariaDB 11.4.9 LTS installed on C drive
   - **Changes Made**:
     - Installed MariaDB 11.4.9 LTS (Long Term Support, supported until May 2029)
     - Created `testcasedb` database with utf8mb4_general_ci collation
     - Updated application.properties to connect to MariaDB
     - Uses HeidiSQL (bundled with MariaDB) as GUI tool
   - **Configuration Details**:
     - JDBC URL: `jdbc:mysql://localhost:3306/testcasedb` (MySQL protocol works with MariaDB)
     - Driver: `com.mysql.cj.jdbc.Driver` (MySQL driver is compatible with MariaDB)
     - Database: MariaDB 11.4.9 LTS on C:\Program Files\MariaDB 11.4
     - GUI: HeidiSQL at C:\Program Files\Common Files\MariaDBShared\HeidiSQL\heidisql.exe
   - **Benefits**:
     - No more XAMPP startup issues
     - More stable and reliable database
     - LTS support until May 2029
     - No permission issues on C drive
   - **Status**: COMPLETED ✅

2. **MySQL Connection Fix** (Commit: 0af0f5c)
   - Changed connection from `127.0.0.1` to `localhost` in application.properties
   - Resolved: MySQL connection issues on Windows with MariaDB

3. **Permission Fixes for QA/BA Users** (Commit: 0af0f5c)
   - SubmoduleService.java: Added module assignment checks for create/update operations
   - TestCaseService.java: Added module assignment checks for create/update operations
   - ImportExportService.java: Added module assignment check for import operation

4. **Code Quality Fixes** (Commit: 0af0f5c)
   - AuthController.java: Replaced deprecated `acceptsProfiles()` with `matchesProfiles()`

5. **Lazy Loading Fix for Excel Import** (Commit: 096c9bb)
   - UserRepository.java: Added `findByUsernameWithModules` method
   - UserContextService.java: Added `getCurrentUserWithModules` method

6. **Query Derivation Fix** (Commit: bd3fc75)
   - UserRepository.java: Added `@Query` annotation to `findByUsernameWithModules`

7. **SecurityHelper Pattern & Module Visibility** (Commit: 8d9581a)
   - Created: `SecurityHelper.java` service class
   - Refactored: SubmoduleService (5 methods), ModuleService (6 methods)
   - Added: `isEditable` flag to TestModule entity with `@JsonProperty` annotation
   - Fixed: Jackson serialization issue with isEditable field
   - Implemented: READ/WRITE access separation for modules and submodules
   - Fixed: Frontend edit button hiding based on isEditable flag

8. **SecurityHelper Refactoring** (Commit: 2f1a449)
   - Refactored: TestCaseService (7 methods), ExecutionService (7 methods), ImportExportService (1 method)
   - Replaced duplicate permission checks with SecurityHelper methods
   - Code reduction: 156 insertions, 214 deletions (-58 lines net)

9. **Excel Import Transaction Rollback Fix** (2026-02-04)
   - Issue: QA users couldn't import Excel due to transaction rollback
   - Root Cause: `createTestExecutionForTestCaseAndUser()` had `@Transactional` + `requireAdmin()` check
   - Solution: Created `autoGenerateTestExecution()` method bypassing ADMIN check for auto-generation
   - Updated: ImportExportService, ModuleService to use new method
   - Result: QA users can now import Excel successfully
   - Date Tested: 2026-02-05
   - Status: COMPLETED ✅

10. **Documentation Update: README.md** (2026-02-05)
   - **Issue**: README.md was outdated and didn't reflect current project state
   - **Changes Made**:
     - Updated technology stack versions (Spring Boot 3.2.0, Angular 21, MariaDB 11.4.9 LTS)
     - Updated architecture to Project → Module → Submodule → TestCase hierarchy (removed TestSuite)
     - Added current status section (Sprint 1 completed, testing 32/32 passed)
     - Added key features section (Redmine integration, Excel import, Execution workbench, Analytics)
     - Updated API endpoints to match actual implementation
     - Updated database schema diagram
     - Updated installation instructions with database setup
     - Added environment variables reference
     - Updated security features and user roles documentation
     - Added deployment checklist and DEPLOYMENT.md reference
     - Updated testing status and project roadmap
   - **Commit**: c17ac38 - "docs: update README.md to reflect Sprint 1 completion and current project state"
   - **Status**: COMPLETED ✅

11. **Remove Unused expected_result Column** (2026-02-09)
   - **Issue**: Unused `expected_result` column in `test_cases` table (all NULL values, no UI to set it)
   - **Changes Made**:
     - Backend: Removed `expectedResult` field, getter, and setter from `TestCase.java`
     - Backend: Removed `setExpectedResult()` call from `TestCaseService.java:208`
     - Frontend: Removed `expectedResult` from TestCase interface in `project.model.ts:59`
     - Frontend: Removed "Final Result" display section from `test-case-detail.component.html:132-135`
     - Database: Dropped `expected_result` column from `test_cases` table via SQL
   - **Important Distinction**: `TestStep.expectedResult` (for individual steps) remains intact and working
   - **Impact**: Cleaner codebase, removed dead code with no functionality loss
   - **Testing**: Backend and frontend both compiled successfully after changes
   - **Status**: COMPLETED ✅

12. **Text Truncation for Module Detail Page** (2026-02-09)
   - **Issue**: Long submodule names and test case titles broke the layout.
   - **Solution**: Added ellipsis truncation and flex-box constraints.
   - **Status**: COMPLETED ✅

13. **User Role Management & Team Removal (Deactivation)** (2026-02-10)
   - **Feature**: Admins can now change user roles and remove users from the team.
   - **Mechanism**: "Removal" is implemented as **Deactivation** (`enabled = false`).
   - **Preservation**: This ensures that all test execution history linked to that user is preserved in the audit trail.
   - **Frontend**: Integrated a "Three Dots" menu (`mat-menu`) in the Team Members list on the Profile page.
   - **Backend**: Added `updateUserRole` and `deactivateUser` to `UserService` and `ApiController`.
   - **Status**: COMPLETED ✅

14. **Guest/External Collaborator Model** (2026-02-10)
   - **Feature**: Support for outsourced teams and freelancers with restricted visibility.
   - **Mechanism**: Added `isExternal` flag to `User` and `Invitation` entities.
   - **Project Integration**: Added `projectId` to `Invitation` for auto-assignment upon registration.
   - **Tunnel Vision**: External guests only see users they share projects with in the Team Directory.
   - **Frontend**: 
     - Added "External Guest" checkbox to the invitation forms (Global and Dialog).
     - Added "INVITE" button to the Project Detail page for project-specific external invites.
     - Added "External" badge in team lists to distinguish guests.
   - **Backend**: 
     - Updated `InvitationService` to handle external flags and auto-assignments.
     - Updated `AuthController`'s `/team-members` endpoint with shared-project visibility logic.
   - **Status**: COMPLETED ✅

### Testing Status (32/32 Tests Passed) ✅
- ✅ Redmine integration (17 tests): Working
- ✅ QA/BA permissions (4 tests): Working
- ✅ Test Case Detail Navigation: Working
- ✅ Analytics Display: Working
- ✅ Execution Workbench completion: Working
- ✅ QA/BA deletion permissions: Working
- ✅ Execution save/navigation: Working
- ✅ Project access: Working
- ✅ Execution filtering: Working
- ✅ Submodule operations (QA): Working
- ✅ Module visibility: Working - QA users can view unassigned modules
- ✅ Submodule READ access: Working - All users can view submodules
- ✅ Frontend edit control: Working - Edit buttons hidden for unassigned modules
- ✅ Excel import (QA): Working - Transaction rollback issue resolved
- ✅ Database connection: Working - Migrated to MariaDB 11.4 LTS
- ✅ QA test case viewing (unassigned modules): Working - Tested and confirmed 2026-02-05
- ⏸️ Production tests (2): Require deployment

### Completed Work Summary

**Database Migration (2026-02-04):**
- Migrated from XAMPP MySQL to standalone MariaDB 11.4.9 LTS
- Resolved persistent XAMPP startup failures due to corrupted Aria logs
- Created `testcasedb` database with proper utf8mb4_general_ci collation
- Updated application.properties for MariaDB connection
- Uses HeidiSQL (bundled with MariaDB) as database GUI tool

**QA User Test Case Viewing Fix (2026-02-04):**
- Fixed issue where QA users couldn't view test cases in unassigned modules
- Removed assignment checks from `getTestCaseById()` method
- Now allows all organization members to VIEW test cases (READ access)
- Maintains WRITE restrictions via existing `requireModuleAccess()` checks in update/delete methods
- Aligns with module viewing pattern: org-wide READ, assignment-based WRITE
- Code reduction: -13 lines (cleaner implementation)

**TestCase isEditable Flag Implementation (202D-02-04):**
- Added `isEditable` flag to TestCaseDTO to control edit button visibility
- Backend sets flag based on user permissions (admin OR module assignment)
- Frontend uses flag to conditionally show/hide edit button
- Fixed JSON serialization by adding `@JsonProperty("isEditable")` annotations
- Follows same pattern as TestModule for consistent permission handling
- Result: Edit button only shows for admins and users assigned to parent module

**SecurityHelper Pattern Implementation:**
- Created centralized permission checking service
- Refactored all service classes to use SecurityHelper
- Eliminated ~150 lines of duplicate permission check code
- Improved maintainability and consistency

**Module Visibility & Edit Control:**
- Separated READ access (all org modules) from WRITE access (assigned modules)
- Added isEditable flag to communicate permissions to frontend
- Fixed Jackson serialization issue with @JsonProperty annotation
- Frontend now conditionally hides edit buttons

**Excel Import Fix:**
- Fixed transaction rollback issue for QA users
- Created autoGenerateTestExecution() for auto-generation scenarios
- Maintains ADMIN check for explicit API calls
- QA users can now import Excel files successfully

### Code Quality Standards (Memory Bank)

#### Permission & Security Patterns
- **READ vs WRITE Access**: Always separate viewing (read) from editing (write) permissions
- **Use SecurityHelper**: All permission checks must use SecurityHelper methods
- **Organization Boundary**: Always verify organization match before checking other permissions
- **Role-Based Access**: Use helper methods: `requireAdmin()`, `requireAdminQaOrBa()`, `requireModuleAccess()`
- **Non-throwing Checks**: Use `canAccessModule()` for read access, `requireModuleAccess()` for write access
- **UI Control Flags**: Use `@Transient` fields with `@JsonProperty` for frontend communication

#### Transaction Management
- **Permission Checks Outside Transaction**: Check permissions BEFORE starting @Transactional methods
- **Avoid Nested @Transactional with Security Checks**: Can cause transaction rollback issues
- **Auto-Generation Methods**: Create separate methods bypassing role checks for internal auto-generation
- **Try-Catch Doesn't Save Transactions**: Once a RuntimeException is thrown in @Transactional, transaction is marked rollback-only

#### DRY Principle (Don't Repeat Yourself)
- **Extract common logic**: If code appears 2+ times, create a helper method
- **SecurityHelper**: For all permission checks (admin checks, organization checks, role checks)
- **Code Duplication Threshold**: If you see the same pattern 3+ times, extract it

#### Modularization Standards
- **Single Responsibility**: Each method should do ONE thing
- **Method Length**: Keep methods under 50 lines when possible, maximum 100 lines
- **Service Layer**: Business logic belongs in services, not controllers
- **Helper Classes**: Create utility classes for reusable logic (SecurityHelper, Mappers, etc.)

#### Repository Patterns
- **@EntityGraph**: Use for controlling fetch strategy (roles, assignedTestModules)
- **@Query**: Use when method name derivation is ambiguous
- **Optional Pattern**: Always use Optional for findById() operations
- **Direct Database Queries**: Use for bypassing lazy loading issues when needed

#### Exception Handling
- **Custom Exceptions**: Create typed exceptions for better type safety
- **Meaningful Messages**: Error messages should be specific and actionable
- **Global Handler**: Rely on GlobalExceptionHandler
- **Transaction Rollback**: RuntimeException in @Transactional marks transaction rollback-only

### Known Issues
None - All issues resolved!

### Environment Notes
- **OS**: Windows 10
- **IDE**: IntelliJ IDEA 2025.2.2
- **Java**: JDK 25
- **Maven**: Local installation at `apache-maven-3.9.8/`
- **Database**: MariaDB 11.4.9 LTS (standalone, installed on C drive)
- **Database GUI**: HeidiSQL (bundled with MariaDB at C:\Program Files\Common Files\MariaDBShared\HeidiSQL\heidisql.exe)
- **Frontend**: Angular 21 running on port 4200
- **Backend**: Spring Boot running on port 8080

### Git Status
- Branch: main
- Last commit: 2f1a449 (SecurityHelper refactoring)
- Uncommitted changes:
  - .ai/memory-bank/activeContext.md (Updated with Module Assignment Checkbox Fix)
  - .ai/memory-bank/progress.md (Updated with Module Assignment Checkbox Fix)
  - src/main/java/com/yourproject/tcm/controller/ApiController.java (Updated getUsersAssignedToProject to populate assignedTestModules)
  - src/main/java/com/yourproject/tcm/model/dto/UserDTO.java (Added assignedTestModules field)
  - src/main/java/com/yourproject/tcm/repository/UserRepository.java (Updated findUsersAssignedToProject EntityGraph)
  - application.properties (Database connection updated for MariaDB)
  - ImportExportService.java (Excel import transaction fix)
  - TestCaseService.java (Added autoGenerateTestExecution method)
  - ModuleService.java (Uses autoGenerateTestExecution)

### Code Quality Standards (Memory Bank)

#### Permission & Security Patterns
- **READ vs WRITE Access**: Always separate viewing (read) from editing (write) permissions
- **Use SecurityHelper**: All permission checks must use SecurityHelper methods, never duplicate logic
- **Organization Boundary**: Always verify organization match before checking other permissions
- **Role-Based Access**: Use helper methods: `requireAdmin()`, `requireAdminQaOrBa()`, `requireProjectAccess()`, `requireModuleAccess()`
- **Non-throwing Checks**: Use `canAccessModule()` for read access, `requireModuleAccess()` for write access
- **UI Control Flags**: Use `@Transient` fields (e.g., `isEditable`) to communicate edit permissions to frontend
- **@JsonProperty for Transient Fields**: When using `@Transient` fields that need to be serialized to JSON, add `@JsonProperty("fieldName")` to the GETTER method (not the field) to ensure correct JSON property name

#### DRY Principle (Don't Repeat Yourself)
- **Extract common logic**: If code appears 2+ times, create a helper method
- **SecurityHelper**: For all permission checks (admin checks, organization checks, role checks)
- **DTO Mappers**: For data transformation (create mapper classes instead of inline mapping)
- **Custom Exceptions**: For error handling (AccessDeniedException, ResourceNotFoundException, etc.)
- **Code Duplication Threshold**: If you see the same pattern 3+ times, extract it

#### SOLID Principles
Always apply SOLID principles when writing code:

**S - Single Responsibility**
- Each class should have ONE reason to change
- Each method should do ONE thing
- Example: Don't mix data access logic with business logic in the same class

**O - Open/Closed**
- Software entities should be open for extension, closed for modification
- Use interfaces and inheritance to extend behavior
- Example: Create new classes instead of modifying existing working code

**L - Liskov Substitution**
- Subtypes must be substitutable for their base types
- Don't break existing contracts when extending
- Example: If a method accepts `Service`, any `Service` implementation should work

**I - Interface Segregation**
- Clients should not be forced to depend on methods they don't use
- Prefer many specific interfaces over one general interface
- Example: Split `UserService` into `UserReadService` and `UserWriteService` if needed

**D - Dependency Inversion**
- Depend on abstractions, not concretions
- Use dependency injection
- Example: Inject `Repository<User>` instead of `UserRepositoryImpl`

**Application in This Project**:
- Use dependency injection for all services and repositories
- Create helper classes for reusable logic (SecurityHelper, Mappers)
- Keep controllers thin, services thick with business logic
- Prefer composition over inheritance
- Extract duplicate code into utility methods

#### Modularization Standards
- **Single Responsibility**: Each method should do ONE thing
- **Method Length**: Keep methods under 50 lines when possible, maximum 100 lines
- **Service Layer**: Business logic belongs in services, not controllers
- **Helper Classes**: Create utility classes for reusable logic (SecurityHelper, Mappers, etc.)
- **Focused Updates**: Refactor one service at a time, test before moving to next

#### Repository Patterns
- **@EntityGraph**: Use for controlling fetch strategy (roles, assignedTestModules, etc.)
- **@Query**: Use when method name derivation is ambiguous
- **Optional Pattern**: Always use Optional for findById() operations
- **Avoid flush()**: Only use flush() when you need the generated ID immediately in the same transaction

#### Exception Handling
- **Custom Exceptions**: Create typed exceptions (AccessDeniedException, ResourceNotFoundException)
- **Meaningful Messages**: Error messages should be specific and actionable
- **Global Handler**: Rely on GlobalExceptionHandler, don't catch exceptions in controllers
- **Avoid RuntimeException**: Use custom exceptions for better type safety and error handling

#### Code Review Checklist
Before committing, verify:
- [ ] No duplicate permission checks (use SecurityHelper)
- [ ] No hardcoded strings/numbers (use constants)
- [ ] Methods are focused and under 50 lines
- [ ] Custom exceptions used instead of RuntimeException
- [ ] DTO mappers used for transformations
- [ ] Null checks added where necessary
- [ ] READ access separated from WRITE access
- [ ] Organization boundary verified first
- [ ] UI flags added for frontend control (isEditable, etc.)
- [ ] SOLID principles followed (Single Responsibility, Open/Closed, etc.)
- [ ] DRY principle followed (no code duplication > 2 occurrences)

### Important Note for Future Work
- Always ask user before committing changes
- Test thoroughly before suggesting commits
- Follow existing code patterns in the repository
- When fixing issues, consider if it's a systemic problem that needs a broader fix
- Separate READ access (view) from WRITE access (edit) for better user experience
- Refactor one service at a time, test before proceeding to next
- Use `@Transient` fields for UI-specific flags that shouldn't persist to database
- Frontend should check `isEditable` flag to hide/show buttons appropriately