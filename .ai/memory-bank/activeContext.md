# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Stabilizing the application for production-readiness**.
- Ensuring QA users can navigate and save execution work without errors.

## Recent Changes
- **QA Execution Save Failure (RESOLVED)**:
  - **Issue**: QA users received a 500 Internal Server Error when clicking "Next" or "Prev" buttons in the Execution Workbench, even when not filling in execution notes or actual results.
  - **Root Cause**: The `findByIdWithStepResults` query in `TestExecutionRepository` was not fetching all necessary relationships (submodule, testModule, project, organization) required for permission checks in `ExecutionService.saveExecutionWork()`. These relationships were lazy-loaded and not properly initialized.
  - **Fix**: Updated the `findByIdWithStepResults` query to include explicit `LEFT JOIN FETCH` for all necessary relationships:
    - `tc.submodule ts`
    - `ts.testModule tm`
    - `tm.project p`
    - `p.organization`
  - **Impact**: QA users can now successfully navigate between executions and save work (even with empty notes) without encountering 500 errors.
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

## Known Issues
- **None currently**

## Next Steps
- Continue monitoring for any user-reported issues.
- Consider adding additional logging for debugging future permission-related issues.

## Important Decisions & Considerations
- **Admin Visibility**: Admins should implicitly see all resources in their organization. API endpoints returning "assigned" resources must explicitly check for Admin role and return the full set.
- **Deletion Strategy**: Complex deletions (like Projects) must leverage the domain services of their children (Modules) to ensure side effects (like clearing user assignments) are handled. Relying solely on JPA Cascade is insufficient for complex relationships.
- **Entity Equality**: Always implement `equals()` and `hashCode()` based on ID for entities involved in collections (Sets) to ensure correct removal and persistence behavior.
- **DTOs**: Ensure all DTOs (like `TestExecutionDTO`) match their constructors exactly when used in services. Flattened DTOs are preferred for the frontend to avoid complex object graph traversals.
- **Frontend Models**: `project.model.ts` is the source of truth for frontend interfaces. Keep it synchronized with backend DTOs.
- **JPA Query Optimization**: When fetching entities that require access to nested relationships for permission checks or business logic, always include explicit `LEFT JOIN FETCH` clauses in the query to avoid lazy loading issues and potential N+1 query problems.
