# Progress: Test Case Management (TCM) System

## What Works
- **Refactoring**: Standardized on "Submodule" across the stack (Entity, DB, API, UI).
- **Project Infrastructure**: Backend (Spring Boot) and Frontend (Angular) robust setup.
// ...
## Current Status
The application has successfully completed the comprehensive refactoring from "Test Submodule" to "Submodule" across the entire codebase. All UI labels, method names, comments, and error messages have been updated. Both backend and frontend compile successfully and have been validated through testing.

Recent fixes and improvements include:
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

**Next Focus**: Continue development with clean "submodule" terminology established.
