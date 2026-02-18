# Project Context

## Current Work: PROJECT_MANAGER Role Implementation

### Summary

The PROJECT_MANAGER role has been fully implemented, allowing users to manage modules and teams within their assigned projects. This role bridges the gap between ADMIN (full access) and QA/BA/TESTER (limited access).

---

### Committed Changes

**Auth Card Updates (2026-02-18 - Commit: 8085a93):**
- `login.component.css/html` - Added full-width divider line
- `register-org.component.css/html` - Added full-width divider line
- `join.component.css/html/ts` - Added full-width divider line + `getRoleDisplayName()` method

---

### Uncommitted Changes - PROJECT_MANAGER Implementation

#### Backend Files Modified:

| File | Changes |
|------|---------|
| `DataInitializationService.java` | Added PROJECT_MANAGER role to `initializeRoles()` |
| `UserContextService.java` | Added `isProjectManager()`, `currentUserIsProjectManager()`, `canCreateOrEditModule()` |
| `SecurityHelper.java` | Added `requireAdminOrProjectManager()` |
| `ModuleService.java` | Updated `createTestModuleForProject()`, `deleteTestModule()`, `getTestModulesAssignedToCurrentUser()` |
| `ProjectService.java` | Updated `assignUserToProject()`, `removeUserFromProject()` |
| `InvitationService.java` | Added PROJECT_MANAGER security checks |
| `UserService.java` | Updated `updateUserRole()` to allow PM to change QA/BA/TESTER roles |
| `ApiController.java` | Updated `@PreAuthorize` annotations for multiple endpoints |
| `UserRepository.java` | Added `assignedProjects` to EntityGraph in `findByUsername()` |

#### Frontend Files Modified:

| File | Changes |
|------|---------|
| `auth.service.ts` | Added helper methods: `canManageModules()`, `canManageTeam()`, `canDeleteModule()`, `canAssignModules()`, `canCreateSubmodule()`, `canManageModuleAssignments()`, `canImportTestCases()` |
| `project-detail.component.html` | Updated buttons to use `authService.canManageModules()`, `canManageTeam()`, `canDeleteModule()` |
| `project-team.component.ts` | Added `isProjectManager` property |
| `project-team.component.html` | Updated to show actions for PM, added PROJECT_MANAGER role option |
| `module-detail.component.html` | Updated 7 buttons to use new helper methods |
| `project-detail.component.ts` | Updated `isAdmin` to include PROJECT_MANAGER |

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

---

### API Endpoints Updated for PROJECT_MANAGER

| Endpoint | Method | Access |
|----------|--------|--------|
| `/api/projects/{id}/testmodules` | POST | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/{id}` | DELETE | ADMIN, PROJECT_MANAGER |
| `/api/projects/{id}/assigned-users` | GET | ADMIN, PROJECT_MANAGER |
| `/api/projects/assign` | POST/DELETE | ADMIN, PROJECT_MANAGER |
| `/api/users/{id}/role` | PUT | ADMIN, PROJECT_MANAGER |
| `/api/invitations` | POST | ADMIN, PROJECT_MANAGER |
| `/api/testmodules/assigned-to-me` | GET | ADMIN, PROJECT_MANAGER, QA, BA, TESTER |

---

### Security Rules Implemented

1. **PROJECT_MANAGER can only:**
   - Create/delete modules in projects they're assigned to
   - Invite/remove users in projects they're assigned to
   - Change roles to QA, BA, TESTER (not ADMIN)
   - See team members in their assigned projects

2. **Backend enforces:**
   - Organization boundary checks
   - Project assignment verification for PM
   - Role restrictions (PM cannot assign ADMIN role)

---

### Testing Status (2026-02-18)

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

---

### Last Session: 2026-02-18
