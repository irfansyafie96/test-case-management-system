-- ============================================
-- Migration: Separate Module Assignment Systems
-- Description: Split user_test_modules into:
--   1. module_editor_assignments (QA/BA only)
--   2. execution_assignees (QA/BA/TESTER)
-- ============================================

-- ============================================
-- Phase 1: Create New Tables
-- ============================================

-- Table for module editors (QA/BA only)
CREATE TABLE module_editor_assignments (
    user_id BIGINT NOT NULL,
    test_module_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, test_module_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_module_id) REFERENCES test_modules(id) ON DELETE CASCADE
);

-- Table for execution assignees (QA/BA/TESTER)
CREATE TABLE execution_assignees (
    user_id BIGINT NOT NULL,
    test_module_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, test_module_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_module_id) REFERENCES test_modules(id) ON DELETE CASCADE
);

-- ============================================
-- Phase 2: Migrate Existing Data
-- ============================================

-- Migrate QA/BA users to module_editor_assignments
INSERT INTO module_editor_assignments (user_id, test_module_id)
SELECT utm.user_id, utm.test_module_id
FROM user_test_modules utm
JOIN user_roles ur ON utm.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.name IN ('QA', 'BA');

-- Migrate ALL users to execution_assignees
-- (any user assigned to a module can be assigned for execution)
INSERT INTO execution_assignees (user_id, test_module_id)
SELECT user_id, test_module_id FROM user_test_modules;

-- ============================================
-- Phase 3: Verification
-- ============================================

-- Count records in old table
SELECT 'Old table count:' as info, COUNT(*) as count FROM user_test_modules;

-- Count records in new tables
SELECT 'Module editors count:' as info, COUNT(*) as count FROM module_editor_assignments;
SELECT 'Execution assignees count:' as info, COUNT(*) as count FROM execution_assignees;

-- Show which users are module editors
SELECT u.username, m.name as module_name, r.name as role
FROM module_editor_assignments mea
JOIN users u ON mea.user_id = u.id
JOIN test_modules m ON mea.test_module_id = m.id
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
ORDER BY m.name, u.username;

-- ============================================
-- Phase 4: Drop Old Table
-- ============================================
-- UNCOMMENT THE LINES BELOW AFTER VERIFYING DATA MIGRATION
-- DROP TABLE user_test_modules;