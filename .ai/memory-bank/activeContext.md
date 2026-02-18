# Project Context

## Current Work: PROJECT_MANAGER Role Implementation

### Committed Changes (2026-02-18)

**Auth Card Updates (COMMITTED - 8085a93):**
- `login.component.css/html` - Added full-width divider line with `var(--ink)` color
- `register-org.component.css/html` - Added full-width divider line
- `join.component.css/html/ts` - Added full-width divider line + `getRoleDisplayName()` method for user-friendly role display

### Uncommitted Changes (Pending)

**Backend:**
- `DataInitializationService.java` - Added PROJECT_MANAGER role to initializeRoles()
- `UserContextService.java` - Added `isProjectManager()`, `currentUserIsProjectManager()`, `canCreateOrEditModule()`
- `SecurityHelper.java` - Added `requireAdminOrProjectManager()`, updated module access checks for PROJECT_MANAGER
- `ModuleService.java` - Updated `createTestModuleForProject()` and `updateTestModule()` to allow PROJECT_MANAGER

**Frontend:**
- `invite-dialog.component.html` - Already has PROJECT_MANAGER role option
- `project-team.component.html` - Added PROJECT_MANAGER role option to inline invite form

### Role Summary
| Role | Can Create Modules | Can Edit Any Module | Can Access Any Module |
|------|-------------------|--------------------|---------------------|
| ADMIN | All projects | All projects | All projects |
| PROJECT_MANAGER | Assigned projects | Assigned projects | Assigned projects |
| QA/BA | - | Assigned modules only | Assigned modules only |
| TESTER | - | - | Assigned modules only |

### Last Session: 2026-02-18
