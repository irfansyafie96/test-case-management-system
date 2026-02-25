# Project Context

## Current Work: UI Fix - Tickets Page Title Icon

### Summary

Fixed the "My Tickets" page title icon to match other page title icons (black/default instead of orange).

---

### Latest Changes (2026-02-25)

**Tickets Page Title Icon Fix:**
- Removed `.page-title mat-icon { color: var(--accent-primary); }` from `tickets.component.css`
- The icon now uses the default color, matching other pages (Executions, Test Analytics, Projects, etc.)

---

## Previous Work: Phase Auto-Assign Feature - COMPLETE

**Phase Auto-Assign Feature:**
- Backend: Modified `ExecutionService.completeTestExecution()` to auto-assign active phase
- When completing execution with no phase, queries active cycles for the project
- Automatically assigns to first active phase if exists
- No phase = still allows execution (no blocking)

**Phase Filter in Executions Page:**
- Added `testCycleId` parameter to backend endpoint `/admin/executions`
- Filter supports: All, No Phase (-1), Specific phase
- Fixed repository to fetch testCycle data
- Frontend passes cycle filter to API

**Phase Display in Workbench:**
- Added "Phase" field in execution workbench info section
- Shows phase name or "No Phase" (gray/italic)
- Files: `execution-workbench.component.html`, `execution-workbench.component.css`

**Data Model Updates:**
- Added `testCycle` field to `TestExecutionDTO`
- Added `testCycle` to `TestExecution` interface in frontend

**Security Fix - Bulk Assign:**
- Fixed `/api/testmodules/bulk-assign` endpoint to allow PROJECT_MANAGER (was ADMIN only)

**Auth Page Fixes:**
- Fixed register page scrollbar: `overflow: hidden` → `overflow-y: auto`
- Fixed join page: Changed divider from `position: absolute` to `relative`
- Fixed login page: Same divider fix

---

## Previous Work: Ticket Edit Dialog UI Improvements - COMPLETE

### Summary

Added edit functionality to Tickets page allowing users to edit ticket details directly.

---

### Latest Changes (2026-02-23)

**Ticket Edit Feature:**
- Created `ticket-edit-dialog.component.ts` with form to edit:
  - Subject
  - Description
  - Redmine URL
  - Status (Open/Closed)
- Added edit button (pencil icon) to tickets table actions
- Added `updateTicket()` method to `tcm.service.ts`
- Added `PUT /api/tickets/{ticketId}` endpoint to backend
- Added `updateTicket()` method to `TicketService.java`
- Creates audit log entries for status changes
- Accessible to all roles (ADMIN, PROJECT_MANAGER, QA, BA, TESTER)

---

## Previous Work: UI Bug Fixes - COMPLETE

### Summary

Fixed multiple UI issues on the Tickets page and Project Detail page (Phases section).

---

### Latest Changes (2026-02-23)

**UI Bug Fixes:**

1. **Icon color on tickets page**:
   - Added `color="accent"` and `color="primary"` to action buttons
   - File: `tickets.component.html`

2. **Delete phase snackbar styling**:
   - Added `panelClass: ['success-snackbar']` to match other notifications
   - File: `project-detail.component.ts`

3. **Date picker not working**:
   - Added `provideNativeDateAdapter()` to `app.config.ts`
   - Fixed missing `#endPicker` template reference in cycle dialog

4. **Redmine URL validation**:
   - Added reactive form with URL pattern validation in phase dialog
   - Form now validates URL format before submission
   - File: `cycle-dialog.component.ts`

5. **Active/Inactive badge visibility**:
   - Fixed CSS using hardcoded colors: Active=#059669 (green), Inactive=#6b7280 (gray)
   - Changed class binding from `[class.active]/[class.inactive]` to `[ngClass]`
   - Files: `project-detail.component.html`, `project-detail.component.css`

6. **Removed phase count badge**:
   - Removed `{{ cycles.length }} Phases` badge from project detail page

7. **CLOSED status not visible**:
   - Fixed CSS using `var(--accent-success)` instead of non-existent `var(--success)`
   - Files: `tickets.component.css`

8. **Phase edit/delete restricted to Admin/PM**:
   - Added `*ngIf="authService.canManageModules()"` to edit and delete buttons
   - QA/BA/TESTER users can only view phases
   - Backend already had `@PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")`
   - Files: `project-detail.component.html`

9. **Ticket status reload**:
   - Fixed toggleTicketStatus to reload data from server after status change
   - Ensures status badge displays correctly after update

10. **Backend DTO fix**:
    - Added `@JsonProperty("isActive")` to TestCycleDTO.java for correct JSON serialization

---

## Previous Work: Test Cycles/Phases Feature - COMPLETE

### Summary

Implemented Test Cycles/Phases feature for organizing test executions by testing phase.

**Test Cycles/Phases Feature:**
- **Description**: Allow PM/Admin to create testing phases linked to Redmine projects
- **Backend Entities**:
  - `TestCycle` - name, description, redmineProjectUrl, startDate, endDate, isActive, sortOrder
  - `TicketAuditLog` - tracks history of ticket changes
  - Updated `TestExecution` - added testCycle ManyToOne relationship
  - Updated `RedmineIssue` - added status (OPEN/CLOSED), auditLogs relationship
- **Backend Services**:
  - `TestCycleService` - CRUD operations for cycles
  - `TicketService` - ticket management and status updates
- **API Endpoints**:
  - `GET/POST /api/projects/{id}/cycles` - list/create cycles
  - `PUT/DELETE /api/cycles/{id}` - update/delete cycles
  - `GET /api/tickets` - list tickets with filters
  - `PUT /api/tickets/{id}/status` - update ticket status
- **Frontend Features**:
  - Tickets page (`/tickets`) with filter card and table
  - Cycle filter dropdown on Executions page
  - Cycles section on Project Detail page
  - Cycle dialog for create/edit
  - PM/Admin read-only mode on execution workbench
- **Security**: Edit/delete restricted to ADMIN and PROJECT_MANAGER only

---

## Previous Work: PDF Design Redesign - COMPLETE
  - Pie chart with clean legend (circle points, no borders)
  - Module breakdown table next to pie chart (side-by-side layout)
  - Light gray table header instead of heavy blue
  - Minimal footer with subtle line
- **Constants**: Added `STYLES` and `LAYOUT` constants for consistent theming (DRY/SOLID)
- **Files Modified**:
  - `pdf-export.service.ts` - Complete redesign with new constants and layout

---

## Previous Work: Pie Chart in PDF Export - COMPLETE

---

## Previous Work: Filter Card Divider Line - COMPLETE

### Summary

Added PDF export functionality to the Test Analytics page. Users can now download analytics reports as colorful PDF documents.

---

### Latest Changes (2026-02-22)

**PDF Export Feature:**
- **Description**: Users can export analytics statistics as PDF
- **Location**: Test Analytics page (`/test-cases`)
- **Access**: All roles (ADMIN, PROJECT_MANAGER, QA, BA, TESTER)
- **Implementation**:
  - Created `PdfExportService` following SOLID/DRY principles
  - Added jsPDF library for PDF generation
  - Colorful PDF with blue header and green project banner
  - Summary section with color-coded KPIs
  - Module breakdown table with consistent borders
  - Filter context included in report
- **Bug Fixed**: AnalyticsService had null pointer (`moduleId` vs `localModuleId` variable conflict)

---

## Previous Work: Test Cases Module Filter Fix - COMPLETE

### Summary

Fixed module filtering in test cases analytics page. KPIs now show correct values when filtering by module.

---

### Latest Changes (2026-02-19)

**Test Cases Module Filter Fix:**
- **Issue**: KPIs showed 0 when filtering by module (User → Project → Module)
- **Root Cause**: Frontend passed Module ID, but backend only filtered by Submodule ID
- **Fix**: 
  - `AnalyticsService.getTestAnalytics()`: Added `moduleId` parameter (4 params total)
  - Added module-level filtering logic (filter by `submodule.testModule.id`)
  - Renamed local variables to avoid conflicts (`moduleId` → `localModuleId`)
  - `ApiController`: Added `moduleId` to `/testcases/analytics` endpoint
  - `tcm.service.ts`: Updated to pass moduleId parameter
- **Result**: Filter by Module now correctly filters test cases and executions

**Test Cases Filter UI Enhancement:**
- Matched filter card design with executions page:
  * Full-width filter container with card styling
  * Filter header with icon and "Filter Analytics" title
  * Three filter dropdowns: User → Project → Module
- Changed filter values to use `"all"` instead of `null` (matches executions page)
- Module dropdown cascades based on selected project
- Added `!important` flags to CSS for consistent Material styling

**Project & Module Filtering (Earlier):**
- Backend: Added `projectId` and `submoduleId` parameters to:
  - `ExecutionService.getAllExecutionsInOrganization()`
  - `AnalyticsService.getTestAnalytics()`
  - `ApiController` endpoints: `/admin/executions`, `/testcases/analytics`
- Frontend: Added project filter dropdown with cascading module filter
  - `executions.component`: Filter by User → Project → Module → Status
  - `test-cases.component`: Filter by User → Project → Module
  - Module dropdown dynamically filters based on selected project
- Service: Updated `tcm.service.ts` methods to accept filter parameters

**Redmine Delete Button:**
- Added delete button for Redmine issue links in execution workbench
- Delete button available in both Redmine card and management dialog
- Confirmation dialog warns that only TCM link is deleted (not Redmine issue)
- Backend endpoint already existed with PROJECT_MANAGER access

**PROJECT_MANAGER Execution Access:**
- Fixed 400 error when PM tried to access execution workbench
- Added PROJECT_MANAGER to @PreAuthorize on 10 execution endpoints
- Service layer already had proper security logic for PM access

**Role Display Pipe:**
- Created `RoleDisplayPipe` for centralized role name formatting (DRY/SOLID)
- Transforms PROJECT_MANAGER → "PROJECT MANAGER" in sidebar
- Replaced duplicate `getRoleDisplayName()` methods with reusable pipe

---

### Previously Committed

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
