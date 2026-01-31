# Active Context - Test Case Management System

## Current Session: 2026-02-01

### User Context
- **User**: irfan
- **Role**: Senior Software Engineer / Developer
- **Communication**: English only
- **Commit Style**: Conventional commits (feat:, fix:, chore:, etc.)

### Project Context
- **Type**: Full-stack web application (Spring Boot + Angular)
- **Purpose**: Test Case Management System with execution tracking
- **Organization**: TMS Asia
- **Status**: Sprint 1 completed, Testing phase in progress

### Current Blockers
1. **Excel Import Issue** (PENDING)
   - Error: "Transaction silently rolled back because it has been marked as rollback-only"
   - Status: Permission checks added, but issue persists
   - Next: Need to investigate root cause - possibly database constraint or validation issue

### Recent Changes (Not Committed Yet)
1. **MySQL Connection Fix**
   - Changed connection from `127.0.0.1` to `localhost` in application.properties
   - Resolved: MySQL connection issues on Windows with MariaDB

2. **Permission Fixes for QA/BA Users**
   - SubmoduleService.java: Added module assignment checks for create/update operations
   - TestCaseService.java: Added module assignment checks for create/update operations
   - ImportExportService.java: Added module assignment check for import operation
   - Status: Submodule and test case creation working, import still failing

3. **Code Quality Fixes**
   - AuthController.java: Replaced deprecated `acceptsProfiles()` with `matchesProfiles()`
   - ImportExportService.java: Added null checks for user, project, organization, submodules

### Files Modified (Not Committed)
- `src/main/resources/application.properties` - MySQL connection string
- `src/main/java/com/yourproject/tcm/service/domain/SubmoduleService.java`
- `src/main/java/com/yourproject/tcm/service/domain/TestCaseService.java`
- `src/main/java/com/yourproject/tcm/service/domain/ImportExportService.java`
- `src/main/java/com/yourproject/tcm/controller/AuthController.java`

### Testing Status
- ✅ Redmine integration: Working
- ✅ Submodule creation (QA): Working
- ✅ Test case creation (QA): Working
- ❌ Excel import (QA): Failing with rollback error
- ⏸️ Other features: Not tested yet

### Next Session Tasks
1. **Fix Excel Import Issue**
   - Investigate root cause of "Transaction silently rolled back"
   - Check for database constraints or validation issues
   - Review ImportExportService.java for potential issues

2. **Complete Testing Checklist**
   - Test remaining features from Sprint 1
   - Verify all permission changes work correctly

3. **Optional: Sprint 2 Tasks**
   - Create DTOMapperService (low priority)
   - Create custom exception classes (low priority)
   - Refactor AnalyticsService (low priority)

### Environment Notes
- **OS**: Windows 10
- **IDE**: IntelliJ IDEA 2025.2.2
- **Java**: JDK 25
- **Maven**: Local installation at `apache-maven-3.9.8/`
- **Database**: MySQL (Standalone, fixed connection issue)
- **Frontend**: Angular 21 running on port 4200
- **Backend**: Spring Boot running on port 8080

### Git Status
- Branch: main
- Uncommitted changes: 5 files modified
- Last commit: 5519032 (Sprint 1 completed)

### Known Issues
1. Chocolatey PATH issue - using local Maven instead
2. Excel import transaction rollback - needs investigation