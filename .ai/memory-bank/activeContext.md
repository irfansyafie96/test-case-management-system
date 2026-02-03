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
- **Status**: Sprint 1 completed, Testing phase in progress, Refactoring in progress

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
   - Status: Submodule and test case creation working

3. **Code Quality Fixes** (Commit: 0af0f5c)
   - AuthController.java: Replaced deprecated `acceptsProfiles()` with `matchesProfiles()`
   - ImportExportService.java: Added null checks for user, project, organization, submodules

4. **Lazy Loading Fix for Excel Import** (Commit: 096c9bb)
   - UserRepository.java: Added `findByUsernameWithModules` method
   - UserContextService.java: Added `getCurrentUserWithModules` method
   - ImportExportService.java: Updated to use user with modules loaded

5. **Query Derivation Fix** (Commit: bd3fc75)
   - UserRepository.java: Added `@Query` annotation to `findByUsernameWithModules`
   - Fixed: Spring Data JPA query derivation error

### Current Work (Not Committed)
1. **SecurityHelper Refactoring** (COMPLETED)
   - Created: `SecurityHelper.java` service class
   - Added methods: `requireAdmin()`, `requireAdminQaOrBa()`, `requireSameOrganization()`, `requireModuleAccess()`, `canAccessModule()`
   - Updated: `SubmoduleService.java` (all 5 methods refactored)
   - Updated: `ModuleService.java` (all 6 methods refactored)
   - Code reduction: ~80% fewer permission check lines
   - Status: Tested and working

2. **Module Visibility Bug Fix** (COMPLETED)
   - Issue: QA users couldn't view unassigned modules (showed "Module Not Found")
   - Solution: Separated READ access (all modules in org) from WRITE access (assigned modules only)
   - Updated: `ModuleService.getTestModuleById()` - allows read access for all org modules
   - Updated: `ModuleService.updateTestModule()` - still requires assignment for edit
   - Status: Tested and working

3. **Submodule READ Access Fix** (COMPLETED)
   - Issue: Submodules in unassigned modules also blocked access
   - Solution: Applied same READ/WRITE separation to submodules
   - Updated: `SubmoduleService.getSubmoduleById()` - allows read access
   - Updated: `SubmoduleService.getSubmodulesByModuleId()` - allows read access
   - Status: Tested and working

4. **Frontend Edit Control** (COMPLETED)
   - Added: `isEditable` transient field to `TestModule` entity with `@JsonProperty("isEditable")` on getter
   - Updated: `TestModuleRepository.java` - Added `isUserAssignedToModule()` direct database query
   - Updated: `ApiController.java` - Sets isEditable flag using direct database query
   - Updated: Frontend `module-detail.component.html` - Hides edit buttons when `!isEditable`
   - Root Cause: Jackson was serializing `isEditable()` getter as `editable` (lowercase)
   - Fix: Added `@JsonProperty("isEditable")` to getter method to force correct JSON property name
   - Status: Tested and working

### Testing Status (29/31 Tests Passed)
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
- ✅ Module visibility bug: FIXED - QA users can now view unassigned modules
- ✅ Submodule READ access: FIXED - All users can view submodules in their organization
- ✅ Frontend edit control: FIXED - Edit buttons hidden for unassigned modules
- ⏸️ Excel import (QA): Fixed in previous session, needs testing
- ⏸️ Production tests (2): Require deployment

### Next Session Tasks
1. **Test Excel Import**
   - User needs to test import as QA user
   - Verify the lazy loading fix works correctly

2. **Continue SecurityHelper Refactoring**
   - Update remaining services: TestCaseService, ExecutionService, ImportExportService
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