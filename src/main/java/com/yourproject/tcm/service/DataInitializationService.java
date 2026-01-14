package com.yourproject.tcm.service;

import com.yourproject.tcm.model.Role;
import com.yourproject.tcm.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

/**
 * DataInitializationService - Handles system-level configuration on startup.
 * 
 * In this multi-tenant version, we only initialize global Roles.
 * All organizations and users must be created through the registration flow.
 */
@Service
public class DataInitializationService {

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultData() {
        // Initialize global system roles
        initializeRoles();
    }

    private void initializeRoles() {
        // Check and create roles if they don't exist
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("QA");
        createRoleIfNotExists("BA");
        createRoleIfNotExists("TESTER");
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role(roleName);
            roleRepository.save(role);
        }
    }
}