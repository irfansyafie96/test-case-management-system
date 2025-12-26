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

        // Initialize default software tester user
        initializeDefaultTester();

        // Initialize default QA/BA user
        initializeDefaultQaBa();

        // Initialize test users with different organizations for testing
        initializeTestUsersWithDifferentOrganizations();
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
            admin.setOrganization("default");
            userRepository.save(admin);

            System.out.println("Default admin user created: " + defaultAdminUsername);
        }
    }

    private void initializeDefaultTester() {
        String testerUsername = "tester";
        String testerEmail = "tester@test.com";
        String testerPassword = "tester123";

        // Check if default tester user exists
        if (userRepository.findByUsername(testerUsername).isEmpty()) {
            User tester = new User(testerUsername, testerEmail, encoder.encode(testerPassword));

            Set<Role> roles = new HashSet<>();
            Role testerRole = roleRepository.findByName("TESTER")
                    .orElseThrow(() -> new RuntimeException("TESTER role not found"));
            roles.add(testerRole);

            tester.setRoles(roles);
            tester.setOrganization("default");
            userRepository.save(tester);

            System.out.println("Default tester user created: " + testerUsername);
        }
    }

    private void initializeDefaultQaBa() {
        String qabaUsername = "qaba";
        String qabaEmail = "qaba@test.com";
        String qabaPassword = "qaba123";

        // Check if default QA/BA user exists
        if (userRepository.findByUsername(qabaUsername).isEmpty()) {
            User qaba = new User(qabaUsername, qabaEmail, encoder.encode(qabaPassword));

            Set<Role> roles = new HashSet<>();
            Role qaRole = roleRepository.findByName("QA")
                    .orElseThrow(() -> new RuntimeException("QA role not found"));
            Role baRole = roleRepository.findByName("BA")
                    .orElseThrow(() -> new RuntimeException("BA role not found"));
            roles.add(qaRole);
            roles.add(baRole);

            qaba.setRoles(roles);
            qaba.setOrganization("default");
            userRepository.save(qaba);

            System.out.println("Default QA/BA user created: " + qabaUsername);
        }
    }

    private void initializeTestUsersWithDifferentOrganizations() {
        // Test user 1: QA user in organization "org1"
        createTestUserIfNotExists("qa_org1", "qa_org1@test.com", "qa123", "QA", "org1");
        
        // Test user 2: BA user in organization "org1"
        createTestUserIfNotExists("ba_org1", "ba_org1@test.com", "ba123", "BA", "org1");
        
        // Test user 3: TESTER user in organization "org2"
        createTestUserIfNotExists("tester_org2", "tester_org2@test.com", "tester123", "TESTER", "org2");
        
        // Test user 4: QA user in organization "org2"
        createTestUserIfNotExists("qa_org2", "qa_org2@test.com", "qa123", "QA", "org2");
        
        System.out.println("Test users with different organizations created for testing");
    }

    private void createTestUserIfNotExists(String username, String email, String password, String roleName, String organization) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User(username, email, encoder.encode(password));
            
            Set<Role> roles = new HashSet<>();
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException(roleName + " role not found"));
            roles.add(role);
            
            user.setRoles(roles);
            user.setOrganization(organization);
            userRepository.save(user);
            
            System.out.println("Test user created: " + username + " (role: " + roleName + ", org: " + organization + ")");
        }
    }
}
