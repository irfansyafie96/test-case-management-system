# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Debugging**: Investigating persistent 500 Error when QA users save/navigate executions.
- **Stabilizing the application for production-readiness**.
- Addressing user-reported visibility issues.

## Recent Changes
- **Backend Permission Robustness (ATTEMPTED)**:
  - **Change**: Updated `ExecutionService` to use direct DB queries (`testModuleRepository.findTestModulesAssignedToUser`) instead of entity collections for checking module-level permissions.
  - **Goal**: To fix the 500 error where QA users couldn't save work on executions they accessed via module assignment.
  - **Status**: **Unresolved**. User reports the error persists.
- **Execution Workbench UI Polish (COMPLETED)**:
  - **Change**: Updated the "PREV" button in the execution workbench to match the "NEXT" button style (Raised, Accent Color).
  - **Reason**: To ensure visual consistency and perfect vertical alignment across all navigation controls in the workbench.
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
  - **Fix**: Updated `ExecutionService` to correctly filter by `execution.assignedToUser.id` when a user filter is applied.
  - **Impact**: Admin users can now correctly filter execution lists to see tasks assigned to specific users.

## Known Issues (FOR LATER FIX)
- **QA Execution Save Failure (PERSISTENT)**:
  - **Symptom**: QA users receive a 500 Internal Server Error (Access Denied) when clicking "Next" or trying to save work in the Execution Workbench, even for executions within modules they are assigned to.
  - **Current State**: Backend permission logic was updated to check module assignment via direct DB query, but the error persists.
  - **Suspects**: 
    - Transaction boundaries or Entity Manager state?
    - Potential mismatch between the execution's module and the user's assigned modules (data integrity)?
    - Is `currentUser` id correct in the context?
  - **Next Step**: Deep debug of `ExecutionService.saveExecutionWork`. Verify IDs being compared.

## Next Steps

## Important Decisions & Considerations
- **Admin Visibility**: Admins should implicitly see all resources in their organization. API endpoints returning "assigned" resources must explicitly check for Admin role and return the full set.
- **Deletion Strategy**: Complex deletions (like Projects) must leverage the domain services of their children (Modules) to ensure side effects (like clearing user assignments) are handled. Relying solely on JPA Cascade is insufficient for complex relationships.
- **Entity Equality**: Always implement `equals()` and `hashCode()` based on ID for entities involved in collections (Sets) to ensure correct removal and persistence behavior.
- **DTOs**: Ensure all DTOs (like `TestExecutionDTO`) match their constructors exactly when used in services. Flattened DTOs are preferred for the frontend to avoid complex object graph traversals.
- **Frontend Models**: `project.model.ts` is the source of truth for frontend interfaces. Keep it synchronized with backend DTOs.
