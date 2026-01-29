# Progress: Test Case Management (TCM) System

## What Works
- **Refactoring**: Standardized on "Submodule" across the stack (Entity, DB, API, UI).
- **Project Infrastructure**: Backend (Spring Boot) and Frontend (Angular) robust setup.
// ...
## Current Status
The application has successfully completed the refactoring from "Test Submodule" to "Submodule" and resolved all associated bugs. Both backend and frontend compile successfully. Recent fixes include:
- Fixed submodule display issue where not all submodules were showing on module detail page
- Resolved JSON circular reference (StackOverflowError) when serializing test modules with submodules
- Fixed CSRF 401 errors when creating test cases for submodules
- Updated UI terminology from "Test Submodules" to "Submodules"
- Changed DDL auto mode from 'create' to 'update' for development
