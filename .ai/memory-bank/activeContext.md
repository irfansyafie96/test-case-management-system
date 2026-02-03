# Active Context - Test Case Management System

## Current Session: 2026-02-03

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
- **Status**: Sprint 1 completed, Testing phase in progress, Refactoring complete

### Current Blockers
None - All issues resolved!

### Recent Changes (Committed)
1. **MySQL Connection Fix** (Commit: 0af0f5c)
   - Changed connection from `127.0.0.1` to `localhost` in application.properties
   - Resolved: MySQL connection issues on Windows with MariaDB

2. **Permission Fixes for QA/BA Users** (Commit: 0af0f5c)
   - SubmoduleService.java: Added module assignment checks for create/update operations
   - TestCaseService.java: Added module assignment checks for create/update operations
   - ImportExportService.java: Added module assignment check for import operation

3. **Code Quality Fixes** (Commit: 0af0f5c)
   - AuthController.java: Replaced deprecated `acceptsProfiles()` with `matchesProfiles()`

4. **Lazy Loading Fix for Excel Import** (Commit: 096c9bb)
   - UserRepository.java: Added `findByUsernameWithModules` method
   - UserContextService.java: Added `getCurrentUserWithModules` method

5. **Query Derivation Fix** (Commit: bd3fc75)
   - UserRepository.java: Added `@Query` annotation to `findByUsernameWithModules`

6. **SecurityHelper Pattern & Module Visibility** (Commit: 8d9581a)
   - Created: `SecurityHelper.java` service class
   - Refactored: SubmoduleService (5 methods), ModuleService (6 methods)
   - Added: `isEditable` flag to TestModule entity with `@JsonProperty` annotation
   - Fixed: Jackson serialization issue with isEditable field
   - Implemented: READ/WRITE access separation for modules and submodules
   - Fixed: Frontend edit button hiding based on isEditable flag

7. **SecurityHelper Refactoring** (Commit: 2f1a449)
   - Refactored: TestCaseService (7 methods), ExecutionService (7 methods), ImportExportService (1 method)
   - Replaced duplicate permission checks with SecurityHelper methods
   - Code reduction: 156 insertions, 214 deletions (-58 lines net)

8. **Excel Import Transaction Rollback Fix** (Commit: Pending)
   - Issue: QA users couldn't import Excel due to transaction rollback
   - Root Cause: `createTestExecutionForTestCaseAndUser()` had `@Transactional` + `requireAdmin()` check
   - Solution: Created `autoGenerateTestExecution()` method bypassing ADMIN check for auto-generation
   - Updated: ImportExportService, ModuleService to use new method
   - Result: QA users can now import Excel successfully

### Testing Status (30/31 Tests Passed)
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
- ✅ Module visibility: FIXED - QA users can view unassigned modules
- ✅ Submodule READ access: FIXED - All users can view submodules
- ✅ Frontend edit control: FIXED - Edit buttons hidden for unassigned modules
- ✅ Excel import (QA): FIXED - Transaction rollback issue resolved
- ⏸️ Production tests (2): Require deployment

### Completed Work Summary

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
- **Database**: MySQL (Standalone, connection fixed)
- **Frontend**: Angular 21 running on port 4200
- **Backend**: Spring Boot running on port 8080

### Git Status
- Branch: main
- Last commit: 2f1a449 (SecurityHelper refactoring)
- Pending changes to commit:
  - ImportExportService.java (Excel import transaction fix)
  - ApiController.java (Permission check before transaction)
  - TestCaseService.java (Added autoGenerateTestExecution method)
  - ModuleService.java (Uses autoGenerateTestExecution)
   - Test each service update
   - Commit after user approval

3. **Complete Testing Checklist**
   - Test remaining features
   - Verify all refactoring work doesn't break existing functionality

### Environment Notes
- **OS**: Windows 10
- **IDE**: IntelliJ IDEA 2025.2.2
- **Java**: JDK 25
- **Maven**: Local installation at `apache-maven-3.9.8/`
- **Database**: MySQL (Standalone, connection fixed)
- **Frontend**: Angular 21 running on port 4200
- **Backend**: Spring Boot running on port 8080

### Git Status
- Branch: main
- Last commit: bd3fc75 (Query derivation fix)
- Uncommitted changes:
  - `src/main/java/com/yourproject/tcm/service/SecurityHelper.java` (NEW)
  - `src/main/java/com/yourproject/tcm/service/domain/SubmoduleService.java` (MODIFIED)
  - `src/main/java/com/yourproject/tcm/service/domain/ModuleService.java` (MODIFIED)
  - `src/main/java/com/yourproject/tcm/model/TestModule.java` (MODIFIED - added isEditable field)
  - `src/main/java/com/yourproject/tcm/controller/ApiController.java` (MODIFIED - added SecurityHelper, set isEditable flag)
  - `.ai/memory-bank/activeContext.md` (MODIFIED - updated with all changes)

### Known Issues
1. Chocolatey PATH issue - using local Maven instead
2. Excel import - code fixed in previous session, needs testing

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

### Important Note for Future Work
- Always ask user before committing changes
- Test thoroughly before suggesting commits
- Follow existing code patterns in the repository
- When fixing issues, consider if it's a systemic problem that needs a broader fix
- Separate READ access (view) from WRITE access (edit) for better user experience
- Refactor one service at a time, test before proceeding to next
- Use `@Transient` fields for UI-specific flags that shouldn't persist to database
- Frontend should check `isEditable` flag to hide/show buttons appropriately