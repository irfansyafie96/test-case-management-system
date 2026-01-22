# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Admin vs QA/BA Differentiation**: Implementing role-based execution views with filtering capabilities.
- **Backend Enhancements**: Implementing automated auditing and ensuring data integrity.
- **UI/UX Refinement**: Fine-tuning visual consistency across modals and page layouts.
- **Deletion Feature**: Fully functional and verified.
- Stabilizing the application for production-readiness.

## Recent Changes
- **Test Case Detail Page Improvements**:
  - **Fixed Metadata Border**: Added `overflow: hidden` to `.spec-card` to prevent the header background from clipping the border radius.
  - **Enhanced Information Display**: Added Project, Module, and Suite names to the metadata section using flattened DTO fields (`projectName`, `moduleName`, `testSuiteName`) for better context.
  - **Metadata Refinement**: Removed 'Status' and 'Date' fields. Added structural dividers to the metadata card for better organization.
  - **Specification Grid Layout**: Reimagined the content card as a structured grid with a dedicated "Label" column on the left and "Value" column on the right, separated by the system's thin borders.
  - **Typography Refinement**: Darkened the step numbers (`#888`) to ensure they are clearly legible while remaining secondary to the step content.
  - **Visual Cleanup**: Removed orange side-borders and blue backgrounds to stick to a more professional, neutral palette that relies on typography and structural lines.
  - **Static Cards**: Removed hover effects (transform and shadow change) from the specification cards to maintain a more stable, documentation-like feel.
  - **Tags Support**: Added display for test case tags if available.
- **Admin Execution Filtering Feature**:
  - **Backend**: Added three new API endpoints for admin filtering:
    - `GET /api/admin/users` - Returns all non-admin users (QA/BA/TESTER) in the organization
    - `GET /api/admin/modules` - Returns all modules in the organization
    - `GET /api/admin/executions?userId={id}` - Returns executions filtered by user's current module assignments
  - **Service Layer**: Added `getUsersInOrganization()`, `getAllModulesInOrganization()`, and `getAllExecutionsInOrganization(userId)` methods in `TcmService.java`
  - **Frontend**: Updated executions component with filter controls (only visible to admin users):
    - Filter by User dropdown
    - Filter by Module dropdown
    - Filter by Status dropdown (Pending/Passed/Failed/Blocked)
  - **Differentiation**: Admins see all executions in their organization with filtering capabilities, while QA/BA users see only their assigned executions
  - **Bug Fix 1**: Fixed filtering by assigned user - the backend now returns ALL executions (not just latest per test case) and frontend correctly filters using `assignedToUserId` field
  - **Bug Fix 2**: Fixed module assignment filtering - when a user's module assignment is removed, executions from that module are no longer shown when filtering by that user. Backend now checks user's current module assignments.
  - **Executions Page UI**:
    - **Filter Dropdowns**: Fixed dropdown panel height (max 250px) and ensured proper width matching by resetting `min-width` on the overlay pane.
    - **Label Positioning**: Resolved label overlapping issue by removing conflicting manual borders on `.mat-mdc-form-field-flex` and instead styling the native `.mdc-notched-outline` components globally to maintain the Neo-Brutalist look (2px thick borders) while preserving the label notch.
    - **Styling**: Moved dropdown panel customization to global `styles.css` using `panelClass="filter-select-panel"`.
  - **JPA Auditing Implementation**:
    - Enabled automatic population of `created_date`, `updated_date`, and `created_by` fields for `Project` entity.  - Implemented `AuditorAwareImpl` to fetch current username from Spring Security context.
  - Configured via `JpaAuditingConfig`.
- **Test Case Edit Fix**:
  - **Backend**: Added `testSteps` field to `TestCaseDTO` with inner `TestStepDTO` class to include step data in API responses
  - **Backend**: Updated `convertToDTO()` method in `ApiController` to convert test steps to DTO format
  - **Backend**: Added `entityManager.flush()` after saving test case during import to ensure steps are persisted to database
  - **Frontend**: Modified `editTestCase()` in `ModuleDetailComponent` to fetch test case with steps before opening edit dialog
  - **Bug Fixed**: Test case edit dialog now correctly displays test steps (actions and expected results) for imported test cases
- **Import Dialog Shadow Fix**:
  - Reverted the custom shadow override for the import dialog.
  - It now uses the standard global `--shadow-lg` (12px) deep shadow, ensuring visual consistency with all other application modals.
- **Profile Page Layout Update**:
  - Refactored "Change Password" and "Download Templates" cards to sit **side-by-side** in a responsive row layout.
  - Implemented `flex: 1` and `align-items: stretch` to ensure equal card heights and perfectly aligned shadows.
  - Added horizontal padding to the tab content to prevent shadow clipping on the edges.
  - Increased `max-width` of the profile container to `1200px` to accommodate the wider layout.
- **Import Dialog Design Overhaul**:
  - **Structure**: Redesigned as a structured multi-step process (Prepare Data → Upload File).
  - **Visuals**: Added a "Template Card" for better visibility of the download action and a modern dashed-border upload zone with icon circles.
  - **Feedback**: Implemented a results grid for success (stats for suites/test cases) and a structured error list for failed imports.
  - **Sizing**: Maintained the optimized 500px width and 90vh maxHeight for laptop compatibility.
- **Import Dialog CSS Consistency**:
  - **CSS Property Reordering**: Moved `margin: 12px auto` property before `align-items` in `.icon-circle` selector and removed redundant `margin-bottom: 12px` for better CSS property organization and consistency.
- **Test Case Detail Page Button Functionality**:
  - **Edit Button**: Now opens the edit modal directly (using `TestCaseDialogImprovedComponent`) with full test case data including steps. After saving, the detail page refreshes to show updated data.
  - **Execute Button**: Now navigates to the executions page (`/executions`) where users can execute test cases.
  - **Implementation**: Added `MatDialog`, `MatSnackBar`, and `Router` imports. Added `editTestCase()` and `navigateToExecutions()` methods. Includes CSRF token synchronization and error handling with success/error snackbars.

## Next Steps
- **Advanced Reporting**: Start planning the reporting dashboard (charts, metrics).
- **File Uploads**: Add capability to attach screenshots to test executions.

## Important Decisions & Considerations
- **Admin vs QA/BA Behavior**: Admins should have full visibility into team execution progress with filtering capabilities, while QA/BA users only see their assigned executions.
- **Module Assignment Filtering**: When filtering by user, backend must check user's CURRENT module assignments (not just the execution's assigned_to_user field) to ensure removed assignments don't show stale executions.
- **DTO Pattern**: Backend uses DTOs with flattened fields to prevent serialization issues. `TestCaseDTO` now includes `testSteps` to ensure edit dialog displays step data.
- **Eager Fetching**: When editing entities, always fetch related data (like test steps) from backend rather than using cached incomplete data from lists.
- **Auditing**: Always use JPA Auditing (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`) for tracking entity metadata instead of manual setting in services.
- **Responsive Dialogs**: Always use `mat-dialog-content` and `maxHeight` for modals.
- **Design Consistency**: Stick to the established simple Neo-Brutalist theme (bold borders, consistent shadows). For side-by-side cards, always use `align-items: stretch` to maintain visual balance. Ensure all modals use the standard `--shadow-lg` for consistency.
