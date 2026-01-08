package com.yourproject.tcm.util;

import com.yourproject.tcm.model.TestExecution;
import com.yourproject.tcm.model.TestModule;
import com.yourproject.tcm.model.User;
import com.yourproject.tcm.repository.TestExecutionRepository;
import com.yourproject.tcm.repository.TestModuleRepository;
import com.yourproject.tcm.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DebugExecutions implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestModuleRepository testModuleRepository;

    public DebugExecutions(UserRepository userRepository, TestExecutionRepository testExecutionRepository, TestModuleRepository testModuleRepository) {
        this.userRepository = userRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.testModuleRepository = testModuleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("========== DEBUG EXECUTION CHECK ==========");
        
        // Check for users 'tester' and 'qaba'
        checkUserExecutions("tester");
        checkUserExecutions("qaba");
        
        System.out.println("---------- MODULE ASSIGNMENTS ----------");
        List<TestModule> modules = testModuleRepository.findAll();
        for (TestModule module : modules) {
            System.out.println("Module: " + module.getName() + " (ID: " + module.getId() + ")");
            Set<User> assignedUsers = module.getAssignedUsers();
            if (assignedUsers.isEmpty()) {
                System.out.println("  No assigned users.");
            } else {
                assignedUsers.forEach(u -> System.out.println("  - Assigned: " + u.getUsername() + " (ID: " + u.getId() + ")"));
            }
        }

        System.out.println("===========================================");
    }

    private void checkUserExecutions(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("User found: " + username + " (ID: " + user.getId() + ")");
            
            List<TestExecution> executions = testExecutionRepository.findByAssignedToUserWithDetails(user);
            System.out.println("Count of executions for " + username + ": " + executions.size());
            
            if (!executions.isEmpty()) {
                System.out.println("First 3 executions details:");
                executions.stream().limit(3).forEach(e -> {
                    System.out.println(" - Execution ID: " + e.getId());
                    if (e.getTestCase() != null) {
                        System.out.println("   -> TestCase: " + e.getTestCase().getTitle());
                        if (e.getTestCase().getTestSuite() != null) {
                            System.out.println("     -> Suite: " + e.getTestCase().getTestSuite().getName());
                            if (e.getTestCase().getTestSuite().getTestModule() != null) {
                                System.out.println("       -> Module: " + e.getTestCase().getTestSuite().getTestModule().getName());
                                if (e.getTestCase().getTestSuite().getTestModule().getProject() != null) {
                                    System.out.println("         -> Project: " + e.getTestCase().getTestSuite().getTestModule().getProject().getName());
                                } else {
                                    System.out.println("         -> Project: NULL (Frontend will hide this!)");
                                }
                            } else {
                                System.out.println("       -> Module: NULL (Frontend will hide this!)");
                            }
                        } else {
                            System.out.println("     -> Suite: NULL (Frontend will hide this!)");
                        }
                    } else {
                        System.out.println("   -> TestCase: NULL");
                    }
                });
            }
        } else {
            System.out.println("User not found: " + username);
        }
    }
}
