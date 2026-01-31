package com.yourproject.tcm.util;

import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.Submodule;
import com.yourproject.tcm.model.TestCase;
import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.User;

/**
 * Utility class for organization boundary security checks.
 * 
 * This class provides helper methods to verify that resources belong to the same organization
 * as the current user, preventing unauthorized access across organization boundaries.
 * 
 * Implements the DRY principle by centralizing repeated organization validation logic.
 */
public class OrganizationSecurityUtil {

    /**
     * Checks if two organizations are the same.
     * 
     * @param org1 First organization
     * @param org2 Second organization
     * @return true if both organizations have the same ID, false otherwise
     */
    public static boolean isSameOrganization(Organization org1, Organization org2) {
        if (org1 == null || org2 == null) {
            return false;
        }
        return org1.getId().equals(org2.getId());
    }

    /**
     * Checks if a project belongs to the specified organization.
     * 
     * @param project The project to check
     * @param organization The organization to verify against
     * @return true if project belongs to the organization, false otherwise
     */
    public static boolean isProjectInOrganization(Project project, Organization organization) {
        if (project == null || project.getOrganization() == null || organization == null) {
            return false;
        }
        return project.getOrganization().getId().equals(organization.getId());
    }

    /**
     * Checks if a test module belongs to the specified organization.
     * 
     * @param testModule The test module to check
     * @param organization The organization to verify against
     * @return true if test module belongs to the organization, false otherwise
     */
    public static boolean isTestModuleInOrganization(TestModule testModule, Organization organization) {
        if (testModule == null || testModule.getProject() == null || organization == null) {
            return false;
        }
        return isProjectInOrganization(testModule.getProject(), organization);
    }

    /**
     * Checks if a submodule belongs to the specified organization.
     * 
     * @param submodule The submodule to check
     * @param organization The organization to verify against
     * @return true if submodule belongs to the organization, false otherwise
     */
    public static boolean isSubmoduleInOrganization(Submodule submodule, Organization organization) {
        if (submodule == null || submodule.getTestModule() == null || organization == null) {
            return false;
        }
        return isTestModuleInOrganization(submodule.getTestModule(), organization);
    }

    /**
     * Checks if a test case belongs to the specified organization.
     * 
     * @param testCase The test case to check
     * @param organization The organization to verify against
     * @return true if test case belongs to the organization, false otherwise
     */
    public static boolean isTestCaseInOrganization(TestCase testCase, Organization organization) {
        if (testCase == null || testCase.getSubmodule() == null || organization == null) {
            return false;
        }
        return isSubmoduleInOrganization(testCase.getSubmodule(), organization);
    }

    /**
     * Checks if a test execution belongs to the specified organization.
     * 
     * @param execution The test execution to check
     * @param organization The organization to verify against
     * @return true if test execution belongs to the organization, false otherwise
     */
    public static boolean isExecutionInOrganization(TestExecution execution, Organization organization) {
        if (execution == null || execution.getTestCase() == null || organization == null) {
            return false;
        }
        return isTestCaseInOrganization(execution.getTestCase(), organization);
    }

    /**
     * Checks if a user belongs to the specified organization.
     * 
     * @param user The user to check
     * @param organization The organization to verify against
     * @return true if user belongs to the organization, false otherwise
     */
    public static boolean isUserInOrganization(User user, Organization organization) {
        if (user == null || user.getOrganization() == null || organization == null) {
            return false;
        }
        return user.getOrganization().getId().equals(organization.getId());
    }

    /**
     * Checks if a test case belongs to the same organization as the current user.
     * 
     * @param testCase The test case to check
     * @param currentUser The current user
     * @return true if test case belongs to current user's organization, false otherwise
     */
    public static boolean isTestCaseAccessible(TestCase testCase, User currentUser) {
        if (currentUser == null || currentUser.getOrganization() == null) {
            return false;
        }
        return isTestCaseInOrganization(testCase, currentUser.getOrganization());
    }

    /**
     * Checks if a project belongs to the same organization as the current user.
     * 
     * @param project The project to check
     * @param currentUser The current user
     * @return true if project belongs to current user's organization, false otherwise
     */
    public static boolean isProjectAccessible(Project project, User currentUser) {
        if (currentUser == null || currentUser.getOrganization() == null) {
            return false;
        }
        return isProjectInOrganization(project, currentUser.getOrganization());
    }

    /**
     * Checks if a user belongs to the same organization as the current user.
     * 
     * @param targetUser The user to check
     * @param currentUser The current user
     * @return true if target user belongs to current user's organization, false otherwise
     */
    public static boolean isUserAccessible(User targetUser, User currentUser) {
        if (currentUser == null || currentUser.getOrganization() == null) {
            return false;
        }
        return isUserInOrganization(targetUser, currentUser.getOrganization());
    }
}