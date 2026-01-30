# Progress: Test Case Management (TCM) System

## What Works
- **UI Consistency**: Execution Workbench navigation buttons now share a unified visual style (PREV/NEXT match).
- **Automatic Execution Generation**: Executions are now automatically created when a user is assigned to a module OR when a test case is created.
- **Project Access**: QA/BA users with module assignments can now correctly view parent project details.
- **Execution Workbench**: Admin filtering by user now correctly shows tasks assigned to that user.    
- **Project Deletion**: Fixed bug where deleting a project with modules failed. Now correctly cleans up junction tables and cascades deletions.
- **Module Visibility**: Fixed issue where Admins couldn't see their own modules on the Modules page.
- **Refactoring**: Standardized on "Submodule" across the stack (Entity, DB, API, UI).
- **Project Infrastructure**: Backend (Spring Boot) and Frontend (Angular) robust setup.
// ...

## Known Issues
- **None currently identified.**

## Current Status
The application has successfully completed the comprehensive refactoring from "Test Submodule" to "Submodule" across the entire codebase. All UI labels, method names, comments, and error messages have been updated. Both backend and frontend compile successfully and have been validated through testing.

Recent fixes and improvements include:
- **Project Deletion**: Resolved `Test Module not found` error during project deletion by fixing the cleanup logic in `ProjectService`.
- **Critical Fix**: Updated `ModuleService` and `ProjectService` to correctly show all resources to Admin users in "assigned to me" endpoints.
- Final pass completed to rename all remaining 'test submodule' references across frontend and backend
- Updated execution workbench label from "Test Submodule" to "Submodule"
- Renamed all service methods (`createTestSubmodule` → `createSubmodule`, etc.)
- Updated API controller method names and error messages
- Fixed submodule display issue where not all submodules were showing on module detail page
- Resolved JSON circular reference (StackOverflowError) when serializing test modules with submodules
- Fixed CSRF 401 errors when creating test cases for submodules
- Updated UI terminology from "Test Submodules" to "Submodules"
- Changed DDL auto mode from 'create' to 'update' for development

**Build Status**: Both Angular frontend and Spring Boot backend compile without errors. Frontend build shows only CSS budget warnings (non-critical).

**Next Focus**: Continue stabilizing features and move towards Advanced Reporting.
