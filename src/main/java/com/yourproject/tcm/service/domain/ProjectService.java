package com.yourproject.tcm.service.domain;

import com.yourproject.tcm.model.Project;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.model.dto.ProjectAssignmentRequest;
import com.yourproject.tcm.repository.ProjectRepository;
import com.yourproject.tcm.repository.UserRepository;
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

    @Autowired
    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all projects for the current user's organization.
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Get projects assigned to the current user.
     */
    public List<Project> getProjectsAssignedToCurrentUser() {
        // This will need to be implemented with current user context
        return projectRepository.findAll();
    }

    /**
     * Create a new project.
     */
    @Transactional
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    /**
     * Get a project by ID.
     */
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * Update a project.
     */
    @Transactional
    public Project updateProject(Long projectId, Project projectDetails) {
        return projectRepository.findById(projectId).map(project -> {
            project.setName(projectDetails.getName());
            project.setDescription(projectDetails.getDescription());
            return projectRepository.save(project);
        }).orElse(null);
    }

    /**
     * Delete a project.
     */
    @Transactional
    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }

    /**
     * Assign a user to a project.
     */
    @Transactional
    public User assignUserToProject(ProjectAssignmentRequest request) {
        return projectRepository.findById(request.getProjectId()).map(project -> {
            return userRepository.findById(request.getUserId()).map(user -> {
                if (!user.getAssignedProjects().contains(project)) {
                    user.getAssignedProjects().add(project);
                    return userRepository.save(user);
                }
                return user;
            }).orElse(null);
        }).orElse(null);
    }

    /**
     * Remove a user from a project.
     */
    @Transactional
    public User removeUserFromProject(ProjectAssignmentRequest request) {
        return projectRepository.findById(request.getProjectId()).map(project -> {
            return userRepository.findById(request.getUserId()).map(user -> {
                user.getAssignedProjects().remove(project);
                return userRepository.save(user);
            }).orElse(null);
        }).orElse(null);
    }
}