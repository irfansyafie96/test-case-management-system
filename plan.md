Test Case Management (TCM) System: Full Project Plan

1. Project Overview

Problem: The current testing process relies on scattered, complex, and non-standardized Excel files (e.g., Final Testing.csv, Finance.csv). This makes tracking, execution, and reporting difficult, slow, and prone to errors.

Solution: A centralized, multi-module web application that standardizes the creation, execution, and reporting of test cases. This system will be the "single source of truth" for all testing activities across all project modules (Training Market, Finance, etc.).

Core Goal: Replace the Excel-based workflow with a standardized, database-driven web application.

2. Tech Stack

Backend: Java 17+ & Spring Boot 3+

spring-boot-starter-data-jpa: For database interaction.

spring-boot-starter-web: For creating the REST API.

h2 (for local dev) / postgresql (for production).

Frontend: Simple & Fast (No framework overhead)

HTML5

Tailwind CSS (For a modern, "vibe" UI without writing CSS)

Vanilla JavaScript (for API calls (fetch) and dynamic forms)

Database:

Local: H2 Database (runs in-memory, zero setup).

Production: PostgreSQL (robust, free, and standard for Java apps).

3. Phase 1: Core Foundation & API (The "Standard Format")

Goal: Build the backend "engine" and database structure. By the end of this phase, the "standard format" is enforced by the database, and you can manage it via an API.

3.1. Database Models (JPA Entities)

These 5 classes are the core "standard format."

Project.java (The top-level e.g., "NCS")

package com.yourproject.tcm.model;

import jakarta.persistence.\*;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "NCS"

    private String description;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Module> modules;

    // Getters and Setters

}

Module.java (e.g., "Training Market", "Finance")

package com.yourproject.tcm.model;

import jakarta.persistence.\*;
import java.util.List;

@Entity
@Table(name = "modules")
public class Module {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Training Market"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestSuite> testSuites;

    // Getters and Setters

}

TestSuite.java (e.g., "TP Registration", "TTT Exemption")

package com.yourproject.tcm.model;

import jakarta.persistence.\*;
import java.util.List;

@Entity
@Table(name = "test_suites")
public class TestSuite {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(nullable = false)
    private String name; // e.g., "TP Registration"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases;

    // Getters and Setters

}

TestCase.java (e.g., "TRM-TS-01: Create TP account")

package com.yourproject.tcm.model;

import jakarta.persistence.\*;
import java.util.List;

@Entity
@Table(name = "test_cases")
public class TestCase {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(unique = true, nullable = false)
    private String testCaseId; // e.g., "TRM-TS-01"

    @Column(nullable = false)
    private String title; // "Scenario:" from your Excel

    private String priority; // "High", "Medium", "Low"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC") // Always keep steps in order
    private List<TestStep> testSteps;

    // Getters and Setters

}

TestStep.java (The individual actions)

package com.yourproject.tcm.model;

import jakarta.persistence.\*;

@Entity
@Table(name = "test_steps")
public class TestStep {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(nullable = false)
    private Integer stepNumber; // 1, 2, 3...

    @Column(columnDefinition = "TEXT", nullable = false)
    private String action; // "Steps" from your Excel

    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedResult; // "Expected Result" from your Excel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    // Getters and Setters

}

3.2. Spring Boot Folder Structure

src/
└── main/
└── java/
| └── com/
| └── yourproject/
| └── tcm/
| ├── TcmApplication.java
| ├── model/
| | ├── Project.java
| | ├── Module.java
| | ├── TestSuite.java
| | ├── TestCase.java
| | └── TestStep.java
| ├── repository/
| | ├── ProjectRepository.java
| | ├── ModuleRepository.java
| | ├── TestSuiteRepository.java
| | └── TestCaseRepository.java
| ├── service/
| | └── TcmService.java
| └── controller/
| └── ApiController.java
└── resources/
├── application.properties
└── static/ <-- Frontend files go here

3.3. Spring Data Repositories

Create an interface for each model (except TestStep, which is managed via TestCase).

package com.yourproject.tcm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.Project;
public interface ProjectRepository extends JpaRepository<Project, Long> { }

package com.yourproject.tcm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.Module;
public interface ModuleRepository extends JpaRepository<Module, Long> { }

package com.yourproject.tcm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.TestSuite;
public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> { }

package com.yourproject.tcm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.yourproject.tcm.model.TestCase;
public interface TestCaseRepository extends JpaRepository<TestCase, Long> { }

3.4. Phase 1 API Endpoints (in ApiController.java)

GET /api/projects -> Get all projects.

POST /api/projects -> Create a new project.

GET /api/projects/{projectId} -> Get details for one project (with its modules).

POST /api/projects/{projectId}/modules -> Create a new module for a project.

GET /api/modules/{moduleId} -> Get details for one module (with its test suites).

POST /api/modules/{moduleId}/testsuites -> Create a new test suite for a module.

GET /api/testsuites/{suiteId} -> Get details for one suite (with its test cases).

POST /api/testsuites/{suiteId}/testcases -> Create a new test case. This request body should include the list of TestSteps.

GET /api/testcases/{testCaseId} -> Get full details for one test case (with its steps).

PUT /api/testcases/{testCaseId} -> Update a test case (and its steps).

DELETE /api/testcases/{testCaseId} -> Delete a test case.

4. Phase 2: The Input System (Frontend)

Goal: Build the "Excel-killer" web interface so users can input and manage tests. All files go in src/main/resources/static/.

index.html (Project Dashboard):

Shows a list of all Projects.

Has a form to create a new Project.

Each project name is a link to project.html?id={projectId}.

project.html (Module View):

Shows the details for one Project.

Shows a list of all Modules in that project.

Has a form to create a new Module for this project.

Each module name is a link to module.html?id={moduleId}.

module.html (Test Suite & Case View):

Shows details for one Module.

Lists all TestSuites in this module.

Inside each suite, lists all TestCases (ID, Title, Priority).

Has a "Create New Test Case" button that links to testcase-form.html?suiteId={suiteId}.

Each test case is a link to testcase-view.html?id={testCaseId}.

testcase-form.html (The "Input" Form):

This is the main input system.

A form with fields: Test Case ID (TRM-TS-XX), Title, Priority, Test Suite.

A dynamic "Test Steps" section:

Has an "Add Step" button (JS) that adds a new row with two textareas: Action and Expected Result.

Each row has a "Remove" button.

On "Save", the JavaScript collects all form data and the list of steps into a JSON object and POSTs it to /api/testsuites/{suiteId}/testcases.

5. Phase 3: Test Execution & Reporting (The "Value")

Goal: Run tests and see live reports, completely replacing Summary.csv and UAT_SUMM_PREP.csv.

5.1. New Database Models (JPA Entities)

TestRun.java (A "snapshot" of a test execution, e.g., "UAT Sprint 2")

package com.yourproject.tcm.model;

import jakarta.persistence.\*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_runs")
public class TestRun {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @Column(nullable = false)
    private String name; // e.g., "UAT Sprint 2 - Training Market"

    private LocalDateTime createdAt;

    // A Test Run contains many results
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestResult> testResults;

    // Getters and Setters

}

TestResult.java (The status of a single test in a run)

package com.yourproject.tcm.model;

import jakarta.persistence.\*;

@Entity
@Table(name = "test_results")
public class TestResult {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase; // The test being run

    @Column(nullable = false)
    private String status; // "UNTESTED", "PASSED", "FAILED", "BLOCKED"

    @Column(columnDefinition = "TEXT")
    private String actualResult; // The "Remarks" if it fails

    private String testerName; // Who ran it

    // Getters and Setters

}

5.2. New API Endpoints

POST /api/modules/{moduleId}/testruns -> Create a new TestRun. This service should find all TestCases in the module and create a TestResult (with "UNTESTED" status) for each one.

GET /api/testruns/{runId} -> Get a test run and all its TestResult objects.

PUT /api/testresults/{resultId} -> Update a single result. This is what the tester hits when they click "Pass" or "Fail".

5.3. New Frontend Pages

test-runner.html (The "Execution" Page):

A tester selects a TestRun.

The UI shows them one TestCase and its TestSteps.

Tester follows the steps.

They have buttons: "Pass", "Fail", "Block".

If "Fail", a textbox for actualResult appears.

Clicking a button sends a PUT to /api/testresults/{resultId} and loads the next test.

dashboard.html (The "Summary" Page):

Shows a list of all TestRuns.

When you select a run, it shows a live dashboard (using Chart.js or similar).

A pie chart showing PASSED vs. FAILED vs. UNTESTED.

A list of all FAILED tests for quick review.

6. How to Use This Plan

Give your AI tool Phase 1 first. Ask it to generate the Spring Boot project, including the models, repositories, and controller stubs.

Get the backend running locally.

Move to Phase 2. Ask for one HTML file at a time (e.g., "Create the index.html file with Tailwind and JS to fetch and display /api/projects").

Build and test, then move to Phase 3 to add the execution layer.
