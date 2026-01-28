# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Stabilizing the application for production-readiness**.
- All Phase 1 and Phase 2 tasks completed.

## Current Problem - Database & Code Synchronization (RESOLVED)
- **Issue**: Startup failures due to renaming "Test Suites" to "Test Submodules" and removing "Scenario" field. The application faced `UnknownPathException` in JPA repositories and SQL errors when `ddl-auto=create` tried to recreate an existing schema in a bad state.
- **Resolution**:
  1.  **Repository Fixes**: Updated `TestCaseRepository` and `TestExecutionRepository` to use the new field name `testSubmodule` instead of `testSuite` in JPQL queries.
  2.  **Schema Reset**: Confirmed `spring.jpa.hibernate.ddl-auto=create` is active. A clean restart after fixing the queries successfully rebuilt the database schema with the correct `test_submodules` table and removed the `scenario` column.
  3.  **Frontend Alignment**: Updated Angular models and components (`executions.component`, `modules.component`, `project-detail.component`) to use `TestSubmodule` terminology and remove references to the deleted `scenario` field.
- **Current Status**: Backend started successfully. Frontend compilation errors resolved.

## Recent Changes
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
- **Verify Frontend Functionality**: Manually test the "Executions" page grouping, "Modules" page counts, and "Project Detail" page stats to ensure the terminology refactor didn't break display logic.
- **Test Excel Import**: Verify that importing an Excel file works correctly with the new `TestSubmodule` logic and without the `Scenario` field.
- **Update Excel Template**: The downloadable template needs to be updated to match the new schema (remove "Scenario" column).
- **Advanced Reporting**: Start planning the reporting dashboard (charts, metrics).
- **File Uploads**: Add capability to attach screenshots to test executions.

## Important Decisions & Considerations
- **Database Strategy**: `ddl-auto=create` is currently used to handle the massive schema changes (renaming tables, removing columns). This wipes data on restart. Once stable, switch back to `update` to persist data.
- **Terminology**: "Submodule" is the definitive term. "Suite" should no longer appear in the UI or code (except maybe as deprecated aliases if absolutely necessary for transition, but we aimed for a clean break).
- **Domain Services**: Continue using the domain service pattern for new features to keep `TcmService` manageable.
- **DTOs**: Ensure all DTOs (like `TestExecutionDTO`) match their constructors exactly when used in services. Flattened DTOs are preferred for the frontend to avoid complex object graph traversals.
- **Frontend Models**: `project.model.ts` is the source of truth for frontend interfaces. Keep it synchronized with backend DTOs.
