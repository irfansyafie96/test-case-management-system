package com.yourproject.tcm.service;

import com.yourproject.tcm.model.Role;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.repository.RoleRepository;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

@Service
public class DataInitializationService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${tcm.app.defaultAdminUsername:admin}")
    private String defaultAdminUsername;

    @Value("${tcm.app.defaultAdminPassword:admin123}")
    private String defaultAdminPassword;

    @Value("${tcm.app.defaultAdminEmail:admin@test.com}")
    private String defaultAdminEmail;

    @PostConstruct
    @Transactional
    public void initializeDefaultData() {
        // Initialize roles
        initializeRoles();

        // Initialize default admin user
        initializeDefaultAdmin();
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

    private void initializeDefaultAdmin() {
        // Check if default admin user exists
        if (userRepository.findByUsername(defaultAdminUsername).isEmpty()) {
            User admin = new User(defaultAdminUsername, defaultAdminEmail, encoder.encode(defaultAdminPassword));

            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            roles.add(adminRole);

            admin.setRoles(roles);
            userRepository.save(admin);

            System.out.println("Default admin user created: " + defaultAdminUsername);
        }
    }
}
