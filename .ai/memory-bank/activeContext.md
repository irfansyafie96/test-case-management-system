# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Stabilizing the application for production-readiness**.
- Addressing user-reported visibility issues.

## Recent Changes
- **Module Visibility Fix (COMPLETED)**:
  - **Issue**: Admin users could not see modules they created on the `/modules` page because `getTestModulesAssignedToCurrentUser` only returned explicitly assigned modules.
  - **Fix**: Updated `ModuleService.getTestModulesAssignedToCurrentUser` to check for ADMIN role. If Admin, it now returns all modules in the user's organization (mirroring `getAllModulesInOrganization` logic).
  - **Impact**: Admins now see all modules in the "My Modules" view without needing manual assignment.
- **Project Visibility Fix (COMPLETED)**:
  - **Issue**: Identified consistent behavior in `ProjectService` where Admins might not see projects in "My Projects" view.
  - **Fix**: Updated `ProjectService.getProjectsAssignedToCurrentUser` to return all organization projects for Admins.
  - **Impact**: Consistent visibility logic across Projects and Modules for Admin users.
- **Submodule Refactoring (COMPLETED)**:
  - **Terminology**: Standardized on "Submodule" instead of "Test Submodule" or "Suite".
  - **Final Completion**: Renamed all remaining 'test submodule' references in UI labels, method names, comments, and error messages across both frontend and backend.
  - **Backend**:
    - Renamed all classes (`Submodule`, `SubmoduleRepository`, `SubmoduleDTO`).
    - Updated all entity relationships and database table name to `submodules`.
    - Updated all JPQL queries to use `submodules` instead of `testSubmodules` in:
      - TestModuleRepository: findByIdWithSubmodules, findAll, findTestModulesAssignedToUser
      - TestExecutionRepository: findByAssignedToUserWithDetails, findAllWithDetails, findAllWithDetailsByOrganizationId
      - TestCaseRepository: findAllWithDetails, findAllWithDetailsByOrganizationId, findByModuleIdWithSteps
    - Updated API endpoints to use `/submodules` instead of `/testsubmodules`.
  - **Frontend**:
    - Updated `project.model.ts` and `tcm.service.ts` (imports and API endpoints).
    - Renamed `TestSubmoduleDialogComponent` to `SubmoduleDialogComponent` (all files).
    - Fixed property references across all components:
      - `testSubmoduleId` → `submoduleId`
      - `testSubmoduleName` → `submoduleName`
      - `testSubmodules` → `submodules`
    - Updated core components (`module-detail`, `executions`, `workbench`, `project-detail`, `test-case-detail`).
  - **Impact**: Simplified API surface and code readability. Cleaned up legacy "test" prefixes from internal hierarchy members.

- **Bug Fixes (COMPLETED)**:
  - **Submodule Display Issue**: Fixed `getTestModuleById()` in TcmService.java to use `submodulesWithTestCases` as source of truth, preventing incomplete submodule lists on module detail page
  - **JSON Circular Reference**: Added `@JsonIgnore` to `TestCase.getTestModule()` to prevent StackOverflowError during JSON serialization
  - **CSRF 401 Errors**: Added `/api/submodules/**` to CSRF ignore list in WebSecurityConfig
  - **UI Text Updates**: Changed "Test Submodules" to "Submodules" in module-detail component
- **Scenario Field Removal (COMPLETED)**:
  - **Backend**: Removed `scenario` field from `TestCase` entity and `TestCaseDTO`. Removed getters/setters.
  - **Logic**: Updated `TcmService` import logic to ignore scenario column and `TestCaseService` update logic.
  - **Frontend**: Removed `scenario` from `TestCase` interface, edit dialog, and detail view.
  - **Impact**: Simplified test case data model. Note: Excel import templates need to be updated to remove the "Scenario" column (or just ignore it).
- **Terminology Refactor: TestSuite -> TestSubmodule (COMPLETED)**:
  - **Backend**:
    - Renamed entity: `TestSuite` -> `TestSubmodule`.
    - Renamed repository: `TestSuiteRepository` -> `TestSubmoduleRepository`.
    - Renamed DTO: `TestSuiteDTO` -> `TestSubmoduleDTO`.
    - Updated relationships in `TestModule`, `TestCase`, `TestExecution`.
    - Updated all service layer logic and API endpoints (`/api/testsubmodules`).
  - **Frontend**:
    - Updated `project.model.ts` interfaces.
    - Updated `executions.component` to group by `testSubmodule`.
    - Updated `modules.component` to display `submodulesCount`.
    - Updated `project-detail.component` to count test cases via `testSubmodules`.
  - **Data**: Database tables renamed to `test_submodules` and foreign keys updated.
- **Service Refactoring (COMPLETED)**:
  - **Domain Services**: Extracted logic into `ProjectService`, `ModuleService`, `SubmoduleService`, `TestCaseService`, `ExecutionService`.
  - **TcmService**: Now acts as a facade/coordinator, delegating core CRUD operations to domain services while handling authorization and cross-cutting concerns.
- **Execution Workbench Navigation Fix**:
  - **Issue**: "Next Test Case" button failed or showed premature completion.
  - **Fix**: Re-trigger component load on URL parameter change, include legacy status values in "pending" filter, and improve navigation logic to respect hierarchy.
- **Console Logging Cleanup**:
  - Removed excessive `console.log` and `console.error` statements from frontend.
  - Removed `System.err.println` from backend.
  - **Security**: Removed password logging.

## Next Steps
- **Test Excel Import**: Verify that importing an Excel file works correctly with the new `TestSubmodule` logic and without the `Scenario` field.
- **Update Excel Template**: The downloadable template needs to be updated to match the new schema (remove "Scenario" column).
- **Advanced Reporting**: Start planning the reporting dashboard (charts, metrics).
- **File Uploads**: Add capability to attach screenshots to test executions.

## Important Decisions & Considerations
- **Admin Visibility**: Admins should implicitly see all resources in their organization. API endpoints returning "assigned" resources must explicitly check for Admin role and return the full set.
- **Database Strategy**: `ddl-auto=create` is currently used to handle the massive schema changes (renaming tables, removing columns). This wipes data on restart. Once stable, switch back to `update` to persist data.
- **Terminology**: "Submodule" is the definitive term. "Suite" should no longer appear in the UI or code (except maybe as deprecated aliases if absolutely necessary for transition, but we aimed for a clean break).
- **Domain Services**: Continue using the domain service pattern for new features to keep `TcmService` manageable.
- **DTOs**: Ensure all DTOs (like `TestExecutionDTO`) match their constructors exactly when used in services. Flattened DTOs are preferred for the frontend to avoid complex object graph traversals.
- **Frontend Models**: `project.model.ts` is the source of truth for frontend interfaces. Keep it synchronized with backend DTOs.