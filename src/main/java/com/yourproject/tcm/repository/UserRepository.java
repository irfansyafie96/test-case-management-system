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

    // Find users assigned to a specific project
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedProjects p WHERE p.id = :projectId")
    List<User> findUsersAssignedToProject(@Param("projectId") Long projectId);

    // Find users assigned to a specific test module
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedTestModules tm WHERE tm.id = :moduleId")
    List<User> findUsersAssignedToTestModule(@Param("moduleId") Long moduleId);

    // Find all non-admin users (QA/BA/TESTER) for admin dashboard filter, filtered by organization
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN ('QA', 'BA', 'TESTER') AND u.organization = :organization")
    List<User> findAllNonAdminUsers(@Param("organization") com.yourproject.tcm.model.Organization organization);
}
