package com.yourproject.tcm.repository;

import com.yourproject.tcm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "assignedModulesForEditing", "assignedModulesForExecution"})
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameWithModules(@Param("username") String username);

    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // Find users by specific role
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    // Find users by specific role and organization
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.organization = :organization")
    List<User> findByRoleNameAndOrganization(@Param("roleName") String roleName, @Param("organization") com.yourproject.tcm.model.Organization organization);

    // Find QA users (users with QA role)
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'QA'")
    List<User> findQaUsers();

    // Find BA users (users with BA role)  
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'BA'")
    List<User> findBaUsers();

    // Find TESTER users (users with TESTER role)
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = 'TESTER'")
    List<User> findTesterUsers();

    // Find QA/BA users (users with either QA or BA role)
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN ('QA', 'BA')")
    List<User> findQaBaUsers();

    // Find users assigned to a specific project (directly or via a module in that project) + all Organization Admins
    @EntityGraph(attributePaths = {"roles", "assignedModulesForEditing", "assignedModulesForExecution"})
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.organization o " +
           "LEFT JOIN u.assignedProjects p " +
           "LEFT JOIN u.assignedModulesForEditing tmEdit " +
           "LEFT JOIN u.assignedModulesForExecution tmExec " +
           "LEFT JOIN u.roles r " +
           "WHERE (p.id = :projectId OR tmEdit.project.id = :projectId OR tmExec.project.id = :projectId OR r.name = 'ADMIN') " +
           "AND o.id = (SELECT p2.organization.id FROM Project p2 WHERE p2.id = :projectId) " +
           "AND u.enabled = true")
    List<User> findUsersAssignedToProject(@Param("projectId") Long projectId);

    // Find users assigned to a specific test module (for editing - QA/BA only)
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedModulesForEditing tm WHERE tm.id = :moduleId")
    List<User> findModuleEditors(@Param("moduleId") Long moduleId);

    // Find users assigned to a specific test module (for execution - QA/BA/TESTER)
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedModulesForExecution tm WHERE tm.id = :moduleId")
    List<User> findExecutionAssignees(@Param("moduleId") Long moduleId);

    // Find all non-admin users (QA/BA/TESTER) for admin dashboard filter, filtered by organization, only active users
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN ('QA', 'BA', 'TESTER') AND u.organization = :organization AND u.enabled = true")
    List<User> findAllNonAdminUsers(@Param("organization") com.yourproject.tcm.model.Organization organization);

    // Find all users in organization (including admins), only active users
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u WHERE u.organization = :organization AND u.enabled = true")
    List<User> findAllUsersByOrganization(@Param("organization") com.yourproject.tcm.model.Organization organization);

    // Find users who share at least one project with the given user
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT other FROM User u JOIN u.assignedProjects p JOIN p.assignedUsers other WHERE u.id = :userId AND other.enabled = true")
    List<User> findCollaborators(@Param("userId") Long userId);
}
