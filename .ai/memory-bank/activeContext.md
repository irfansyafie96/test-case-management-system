# Active Context: Test Case Management (TCM) System

## Current Work Focus
- **Admin vs QA/BA Differentiation**: Implementing role-based execution views with filtering capabilities.
- **Backend Enhancements**: Implementing automated auditing and ensuring data integrity.
- **UI/UX Refinement**: Fine-tuning visual consistency across modals and page layouts.
- **Deletion Feature**: Fully functional and verified.
- Stabilizing the application for production-readiness.

## Recent Changes
- **Admin Execution Filtering Feature**:
  - **Backend**: Added two new API endpoints for admin filtering:
    - `GET /api/admin/users` - Returns all non-admin users (QA/BA/TESTER) in the organization
    - `GET /api/admin/modules` - Returns all modules in the organization
  - **Service Layer**: Added `getUsersInOrganization()` and `getAllModulesInOrganization()` methods in `TcmService.java`
  - **Frontend**: Updated executions component with filter controls (only visible to admin users):
    - Filter by User dropdown
    - Filter by Module dropdown
    - Filter by Status dropdown (Pending/Passed/Failed/Blocked)
  - **Differentiation**: Admins see all executions in their organization with filtering capabilities, while QA/BA users see only their assigned executions
  - **Executions Page UI**:
    - **Filter Dropdowns**: Fixed dropdown panel height (max 250px) and ensured proper width matching by resetting `min-width` on the overlay pane.
    - **Label Positioning**: Resolved label overlapping issue by removing conflicting manual borders on `.mat-mdc-form-field-flex` and instead styling the native `.mdc-notched-outline` components globally to maintain the Neo-Brutalist look (2px thick borders) while preserving the label notch.
    - **Styling**: Moved dropdown panel customization to global `styles.css` using `panelClass="filter-select-panel"`.
  - **JPA Auditing Implementation**:
    - Enabled automatic population of `created_date`, `updated_date`, and `created_by` fields for `Project` entity.  - Implemented `AuditorAwareImpl` to fetch current username from Spring Security context.
  - Configured via `JpaAuditingConfig`.
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

## Next Steps
- **Advanced Reporting**: Start planning the reporting dashboard (charts, metrics).
- **File Uploads**: Add capability to attach screenshots to test executions.

## Important Decisions & Considerations
- **Admin vs QA/BA Behavior**: Admins should have full visibility into team execution progress with filtering capabilities, while QA/BA users only see their assigned executions.
- **Auditing**: Always use JPA Auditing (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`) for tracking entity metadata instead of manual setting in services.
- **Responsive Dialogs**: Always use `mat-dialog-content` and `maxHeight` for modals.
- **Design Consistency**: Stick to the established simple Neo-Brutalist theme (bold borders, consistent shadows). For side-by-side cards, always use `align-items: stretch` to maintain visual balance. Ensure all modals use the standard `--shadow-lg` for consistency.
