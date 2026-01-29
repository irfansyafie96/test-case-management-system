package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.Organization;
import com.yourproject.tcm.model.dto.ProjectAssignmentRequest;
import com.yourproject.tcm.repository.ProjectRepository;
import com.yourproject.tcm.repository.UserRepository;
import com.yourproject.tcm.service.UserContextService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Domain service for Project-related operations.
 * Extracted from TcmService for better separation of concerns.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, UserContextService userContextService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
    }

    /**
     * Get all projects in the system
     * For ADMIN users: returns all projects for their organization
     * For QA/BA/TESTER users: returns projects they have ANY access to (Direct or via Module)
     * @return List of projects based on user role and assignments
     */
    public List<Project> getAllProjects() {
        User currentUser = userContextService.getCurrentUser();
        if (userContextService.isAdmin(currentUser)) {
            Organization org = currentUser.getOrganization();
            if (org == null) {
                return List.of(); // Should not happen for valid users, but safety first
            }
            return projectRepository.findAllByOrganization(org);
        } else {
            return projectRepository.findProjectsAssignedToUser(currentUser.getId());
        }
    }

    /**
     * Get projects assigned to the current user.
     */
    public List<Project> getProjectsAssignedToCurrentUser() {
        User currentUser = userContextService.getCurrentUser();
        return projectRepository.findProjectsAssignedToUser(currentUser.getId());
    }

    /**
     * Create a new project.
     */
    @Transactional
    public Project createProject(Project project) {
        User currentUser = userContextService.getCurrentUser();
        if (!userContextService.isAdmin(currentUser)) {
            throw new RuntimeException("Only ADMIN users can create projects");
        }
        
        Organization currentOrg = currentUser.getOrganization();
        if (currentOrg == null) {
            throw new RuntimeException("Current user does not belong to an organization");
        }
        
        // Check if project with same name already exists in the organization
        Optional<Project> existingProject = projectRepository.findByNameAndOrganization(project.getName(), currentOrg);
        if (existingProject.isPresent()) {
            throw new RuntimeException("A project with the name '" + project.getName() + "' already exists in your organization");
        }
        
        project.setOrganization(currentOrg);
        return projectRepository.save(project);
    }

    /**
     * Get a project by ID with security checks.
     * ADMIN users can access any project in their organization.
     * Non-ADMIN users can only access projects they are assigned to.
     */
    public Optional<Project> getProjectById(Long projectId) {
        User currentUser = userContextService.getCurrentUser();
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        
        if (projectOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Project project = projectOpt.get();
        
        // Check organization boundary
        if (!project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
            throw new RuntimeException("Project not found or access denied");
        }
        
        // ADMIN users can access any project in their organization
        if (userContextService.isAdmin(currentUser)) {
            return projectOpt;
        }
        
        // Non-ADMIN users can only access projects they are assigned to
        if (currentUser.getAssignedProjects().contains(project)) {
            return projectOpt;
        }
        
        throw new RuntimeException("Access denied: You are not assigned to this project");
    }

    /**
     * Update a project.
     */
    @Transactional
    public Project updateProject(Long projectId, Project projectDetails) {
        User currentUser = userContextService.getCurrentUser();
        
        return projectRepository.findById(projectId).map(project -> {
            // Security check: Only ADMIN users can update projects
            if (!userContextService.isAdmin(currentUser)) {
                throw new RuntimeException("Only ADMIN users can update projects");
            }
            
            // Organization boundary check
            if (!project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Project not found or access denied");
            }
            
            // Check if project name is being changed and if new name already exists in organization
            if (!project.getName().equals(projectDetails.getName())) {
                Optional<Project> existingProject = projectRepository.findByNameAndOrganization(projectDetails.getName(), project.getOrganization());
                if (existingProject.isPresent()) {
                    throw new RuntimeException("A project with the name '" + projectDetails.getName() + "' already exists in your organization");
                }
            }
            
            project.setName(projectDetails.getName());
            project.setDescription(projectDetails.getDescription());
            return projectRepository.save(project);
        }).orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    /**
     * Delete a project and all its contents (cascading delete)
     * This will delete: Project → Modules → Submodules → Test Cases → Executions → Step Results
     * @param projectId The ID of the project to delete
     * @throws RuntimeException if project doesn't exist
     */
    @Transactional
    public void deleteProject(Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            
            // Security check: Only ADMIN users can delete projects
            User currentUser = userContextService.getCurrentUser();
            if (!userContextService.isAdmin(currentUser)) {
                throw new RuntimeException("Only ADMIN users can delete projects");
            }
            
            // Organization boundary check
            if (!project.getOrganization().getId().equals(currentUser.getOrganization().getId())) {
                throw new RuntimeException("Project not found or access denied");
            }

            // 1. Clear assignments: Remove this project from all users' assigned lists
            // This is critical for ManyToMany cleanup in the junction table 'user_projects'
            if (project.getAssignedUsers() != null) {
                // Create a copy to avoid concurrent modification exception
                java.util.Set<User> users = new java.util.HashSet<>(project.getAssignedUsers());
                for (User user : users) {
                    user.getAssignedProjects().remove(project);
                    userRepository.save(user); // Save user to update junction table
                }
                project.getAssignedUsers().clear();
            }
            entityManager.flush(); // Ensure junction table records are gone

            // TODO: When ModuleService is implemented, replace this with proper module deletion
            // For now, rely on JPA cascading (CascadeType.ALL on project.modules)
            // Note: This may not handle all cleanup logic in deleteTestModule method
            
            // Now delete the project itself (JPA cascading should handle modules)
            projectRepository.deleteById(projectId);
            entityManager.flush(); // Ensure data is written to DB
        } else {
            throw new RuntimeException("Project not found with id: " + projectId);
        }
    }

    /**
     * Assign a user to a project.
     */
    @Transactional
    public User assignUserToProject(ProjectAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());

        if (userOpt.isPresent() && projectOpt.isPresent()) {
            User user = userOpt.get();
            Project project = projectOpt.get();

            // Check if user has QA or BA role
            boolean hasQaOrBaRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("QA") || role.getName().equals("BA"));

            if (!hasQaOrBaRole) {
                throw new RuntimeException("User must have QA or BA role to be assigned to projects");
            }

            // Check if user belongs to same organization as current user
            User currentUser = userContextService.getCurrentUser();
            Organization currentUserOrg = currentUser.getOrganization();
            Organization userOrg = user.getOrganization();

            // If either organization is null (shouldn't happen in valid setup) or they don't match
            if (currentUserOrg == null || userOrg == null || !currentUserOrg.getId().equals(userOrg.getId())) {
                throw new RuntimeException("User must belong to the same organization as the assigner");
            }

            // Add project to user's assigned projects if not already assigned
            if (!user.getAssignedProjects().contains(project)) {
                user.getAssignedProjects().add(project);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Already assigned
            }
        }
        throw new RuntimeException("User or project not found with id: " + request.getUserId() + " or " + request.getProjectId());
    }

    /**
     * Remove a user from a project.
     */
    @Transactional
    public User removeUserFromProject(ProjectAssignmentRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        Optional<Project> projectOpt = projectRepository.findById(request.getProjectId());

        if (userOpt.isPresent() && projectOpt.isPresent()) {
            User user = userOpt.get();
            Project project = projectOpt.get();

            // Remove project from user's assigned projects
            if (user.getAssignedProjects().contains(project)) {
                user.getAssignedProjects().remove(project);
                User savedUser = userRepository.save(user);
                entityManager.flush();
                return savedUser;
            } else {
                return user; // Not assigned
            }
        }
        throw new RuntimeException("User or project not found with id: " + request.getUserId() + " or " + request.getProjectId());
    }
}