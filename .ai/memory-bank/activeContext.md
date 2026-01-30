# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Stabilizing the application for production-readiness**.
- Addressing user-reported visibility issues.
- **Verification**: Verify that project deletion now works correctly with the recent fix.

## Recent Changes
- **Automatic Execution Generation (COMPLETED)**:
  - **Issue**: QA users saw empty execution lists even when assigned to modules because execution records were not automatically created.
  - **Fix**: Implemented bidirectional auto-generation triggers:
    1.  **On Test Case Creation**: Automatically creates executions for all users already assigned to the parent module.
    2.  **On Module Assignment**: Automatically creates executions for the newly assigned user for all existing test cases in that module.
  - **Impact**: Ensures "Tasks" appear in the execution workbench immediately upon assignment or creation, without needing manual regeneration.
- **Project Access Logic Fix (COMPLETED)**:
  - **Issue**: QA users assigned to a module (but not the project) received a 500 Error when accessing the project detail page.
  - **Root Cause**: `ProjectService` enforced strict project-level assignment checks, throwing a RuntimeException if the user wasn't directly assigned to the project.
  - **Fix**: Updated access checks in `getProjectById` and `getProjectWithModulesById` to grant access if the user is assigned to the project OR to any module within that project.
  - **Impact**: Users with module-level access can now correctly view the parent project details.
- **Execution Filter Logic Fix (COMPLETED)**:
  - **Issue**: Admin users reported seeing no tasks on the Executions page when filtering by user (or even default view).
  - **Root Cause**: `ExecutionService.getAllExecutionsInOrganization` had incorrect filtering logic. When a `userId` was provided, it filtered executions based on the user's *assigned modules* (showing all executions in those modules) instead of the user's *assigned executions*.
  - **Fix**: Updated `ExecutionService` to correctly filter by `execution.assignedToUser.id` when a user filter is applied.
  - **Impact**: Admin users can now correctly filter execution lists to see tasks assigned to specific users.
- **Project Deletion Fix (COMPLETED)**:
  - **Issue**: Deleting a project with modules failed with `RuntimeException: Test Module not found`.
  - **Root Cause**: `ProjectService.deleteProject` was clearing the project's module list and flushing (triggering implicit DB deletion via `orphanRemoval`) *before* attempting to manually delete modules service-side. This caused the service to look for already-deleted modules.
  - **Fix**: Removed the redundant manual module deletion logic (Step 2) in `ProjectService`. Retained the native SQL cleanup of `user_test_modules` (Step 1) to handle the foreign key constraints, then allowed standard JPA cascading (Step 3) to handle the deletion of modules, submodules, and test cases.
  - **Impact**: Projects containing modules can now be deleted successfully without 404/500 errors.
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

## Known Issues (FOR LATER FIX)
- **None currently identified.**

## Next Steps
- **Test Excel Import**: Verify that importing an Excel file works correctly with the new `Submodule` logic and without the `Scenario` field.
- **Update Excel Template**: The downloadable template needs to be updated to match the new schema (remove "Scenario" column).
- **Advanced Reporting**: Start planning the reporting dashboard (charts, metrics).
- **File Uploads**: Add capability to attach screenshots to test executions.

## Important Decisions & Considerations
- **Admin Visibility**: Admins should implicitly see all resources in their organization. API endpoints returning "assigned" resources must explicitly check for Admin role and return the full set.
- **Deletion Strategy**: Complex deletions (like Projects) must leverage the domain services of their children (Modules) to ensure side effects (like clearing user assignments) are handled. Relying solely on JPA Cascade is insufficient for complex relationships.
- **Entity Equality**: Always implement `equals()` and `hashCode()` based on ID for entities involved in collections (Sets) to ensure correct removal and persistence behavior.
- **DTOs**: Ensure all DTOs (like `TestExecutionDTO`) match their constructors exactly when used in services. Flattened DTOs are preferred for the frontend to avoid complex object graph traversals.
- **Frontend Models**: `project.model.ts` is the source of truth for frontend interfaces. Keep it synchronized with backend DTOs.
