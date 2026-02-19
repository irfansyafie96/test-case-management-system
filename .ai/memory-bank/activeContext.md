# Project Context

## Current Work: UI Consistency Fixes - COMPLETE

### Summary

Fixed delete module button styling inconsistency between project detail and module detail pages.

---

### Latest Fix (2026-02-19)

**Delete Module Button UI Fix:**
- `project-detail.component.html` - Changed icon from `close` to `delete`, added `color="warn"` attribute
- `project-detail.component.css` - Changed default color from `#999` to `var(--warn-color)`
- Now matches the module detail page: red trash can icon with warn color styling

**Module Assignment Dropdown Fix:**
- `module-detail.component.ts` - Changed `loadAssignmentData()` to use `getUsersAssignedToProject(projectId)` instead of `getUsersByRole()`
- Now filters to show only QA/BA/TESTER roles from the project's team
- Prevents cross-project user assignment

---

### Previously Committed

**Auth Card Updates (2026-02-18 - Commit: 8085a93):**
- `login.component.css/html` - Added full-width divider line
- `register-org.component.css/html` - Added full-width divider line
- `join.component.css/html/ts` - Added full-width divider line + `getRoleDisplayName()` method

---

### Uncommitted Changes - 2026-02-19 (To Commit)

#### Backend Files Modified:

| File | Changes |
|------|---------|
| `SecurityHelper.java` | Added `canViewAllOrganizationExecutions()`, `getViewableProjectIds()`, `requireAdminProjectManagerQaOrBa()` |
| `TestModuleRepository.java` | Added `findModuleIdsByProjectIds()` method |
| `ApiController.java` | Injected SecurityHelper, updated isEditable logic (DRY), added PROJECT_MANAGER to 25+ @PreAuthorize |
| `ExecutionService.java` | Updated to use SecurityHelper for execution visibility |
| `AnalyticsService.java` | Injected SecurityHelper, updated analytics for PROJECT_MANAGER |
| `SubmoduleService.java` | Updated to use new security method |
| `TestCaseService.java` | Updated to use new security method |
| `ImportExportService.java` | Updated to include PM + project check |
| `ModuleService.java` | Updated to use new security method |
| `UserService.java` | Added check to prevent PM from changing ADMIN role |
| `AuthController.java` | Added PROJECT_MANAGER to `/api/auth/users` |

#### Frontend Files Modified:

| File | Changes |
|------|---------|
| `auth.service.ts` | Added `canViewAllExecutions()` helper |
| `executions.component.ts` | Updated to use `canViewAllExecutions()` for admin filters |
| `test-cases.component.ts` | Updated to use `canViewAllExecutions()` for admin filters |

#### DRY Improvements:

1. **Centralized security checks** - `SecurityHelper.canViewAllOrganizationExecutions()` used in:
   - `ExecutionService.getTestExecutionsForCurrentUser()`
   - `ExecutionService.getAllExecutionsInOrganization()`
   - `AnalyticsService.getCompletionSummaryForCurrentUser()`
   - `AnalyticsService.getTestAnalytics()`

2. **Centralized module access** - `SecurityHelper.canAccessModule()` used in:
   - `ApiController.getTestModuleById()` (isEditable flag)
   - `ApiController.getTestCaseById()` (isEditable flag)

3. **New security method** - `requireAdminProjectManagerQaOrBa()` used in:
   - `SubmoduleService` (create/update/delete)
   - `TestCaseService` (create/update/delete)
   - `ImportExportService` (import)
   - `ModuleService` (assign/unassign)

---

### Role Capabilities Matrix

| Capability | ADMIN | PROJECT_MANAGER | QA/BA | TESTER |
|------------|-------|----------------|-------|--------|
| **View All Org Projects** | ✅ | Assigned Only | - | - |
| **Create Modules** | ✅ | Assigned Projects | - | - |
| **Edit Modules** | ✅ | Assigned Projects | Assigned Modules | - |
| **Delete Modules** | ✅ | Assigned Projects | - | - |
| **View Team** | ✅ | Assigned Projects | - | - |
| **Invite Members** | ✅ | Assigned Projects | - | - |
| **Remove Members** | ✅ | Assigned Projects | - | - |
| **Change Roles** | All Roles | QA/BA/TESTER Only | - | - |
| **Assign Modules** | ✅ | ✅ | ✅ | - |
| **Execute Tests** | ✅ | ✅ | ✅ | ✅ |
| **View Executions** | All Org | Assigned Projects | Assigned Modules | Assigned Modules |
| **View Analytics** | All Org | Assigned Projects | Assigned Modules | Assigned Modules |
| **Create Submodules** | ✅ | ✅ | ✅ | - |
| **Manage Assignments** | ✅ | ✅ | ✅ | - |
| **Import Test Cases** | ✅ | ✅ | ✅ | - |

---

### API Endpoints Updated for PROJECT_MANAGER

| Endpoint | Method | Access |
|----------|--------|--------|
| `/api/projects/{id}/testmodules` | POST | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/{id}` | DELETE | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/{id}` | PUT | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/projects/{id}/assigned-users` | GET | ADMIN, PROJECT_MANAGER |
| `/api/projects/assign` | POST/DELETE | ADMIN, PROJECT_MANAGER |
| `/api/users/{id}/role` | PUT | ADMIN, PROJECT_MANAGER |
| `/api/invitations` | POST | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/assigned-to-me` | GET | ADMIN, PROJECT_MANAGER, QA, BA, TESTER |
| `/api/executions/my-assignments` | GET | ADMIN, PROJECT_MANAGER, QA, BA, TESTER |
| `/api/executions/summary` | GET | ADMIN, PROJECT_MANAGER, QA, BA, TESTER |
| `/api/admin/executions` | GET | ADMIN, PROJECT_MANAGER |
| `/api/admin/users` | GET | ADMIN, PROJECT_MANAGER |
| `/api/admin/modules` | GET | ADMIN, PROJECT_MANAGER |
| `/api/auth/users` | GET | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/{id}/submodules` | POST | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/submodules/{id}` | PUT/DELETE | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/submodules/{id}/testcases` | POST | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/modules/{id}/execution-assignees` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/modules/{id}/editors` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/modules/execution-assign` | POST/DELETE | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/testmodules/{id}/assigned-users` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/testmodules/assign` | POST/DELETE | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/projects/assigned-to-me` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/projects/{id}/modules` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/users/by-role/{roleName}` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/executions/{id}/assign` | POST | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/executions/assigned-to/{userId}` | GET | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/testmodules/{id}/regenerate-executions` | POST | ADMIN, PROJECT_MANAGER, QA, BA |
| `/api/testmodules/{id}/import` | POST | ADMIN, PROJECT_MANAGER, QA, BA |

---

### Security Rules Implemented

1. **PROJECT_MANAGER can:**
   - Create/delete modules in projects they're assigned to
   - Invite/remove users in projects they're assigned to
   - Change roles to QA, BA, TESTER (not ADMIN)
   - See team members in their assigned projects
   - View ALL executions in their assigned projects (not just assigned ones)
   - View ALL test cases in their assigned projects
   - View analytics for their assigned projects
   - Filter by users in their assigned projects

2. **PROJECT_MANAGER cannot:**
   - Change ADMIN user's role
   - Access projects they're not assigned to
   - Assign ADMIN role to users

3. **Backend enforces:**
   - Organization boundary checks
   - Project assignment verification for PM
   - Role restrictions (PM cannot assign ADMIN role)

4. **DRY Principle:**
   - SecurityHelper centralizes all role-based checks
   - Single source of truth for permission logic

---

### Testing Status (2026-02-19) - ALL PASSED ✅

| Test Case | Status |
|----------|--------|
| Login as PROJECT_MANAGER | ✅ |
| View assigned projects | ✅ |
| Create module in assigned project | ✅ |
| Delete module in assigned project | ✅ |
| View team members | ✅ |
| Invite members | ✅ |
| Change user role (QA/BA/TESTER) | ✅ |
| View modules page | ✅ |
| Module detail page buttons | ✅ |
| Executions page loads | ✅ |
| Manage Assignments dialog | ✅ |
| View analytics summary | ✅ |
| Create submodule | ✅ |
| Import test cases (Excel) | ✅ |
| View analytics - assigned projects only | ✅ |
| Filter by user in Test Cases page | ✅ |
| Navigate to unassigned project modules | ✅ (correctly denied) |
| Change ADMIN user's role | ✅ (correctly blocked) |
| Edit test case in assigned project | ✅ |
| View all executions in assigned projects | ✅ |

---

### Last Session: 2026-02-19
