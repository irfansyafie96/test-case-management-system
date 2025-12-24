
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, of, map } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { Project, TestModule, TestSuite, TestCase, TestExecution, TestStepResult, User, ProjectAssignmentRequest, ModuleAssignmentRequest } from '../models/project.model';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

/**
 * TCM Service - Main API Service for the Test Case Management System
 *
 * This service handles all communication with the backend API, providing
 * methods to interact with the test case hierarchy: Projects → Modules → Suites → Test Cases → Test Steps
 * and their executions/results.
 *
 * Key Features:
 * - HTTP API communication with proper authentication headers (via Interceptor)
 * - Shared state management for projects and modules
 * - Server-Side Rendering (SSR) compatibility
 */
@Injectable({
  providedIn: 'root'  // Singleton service, available app-wide
})
export class TcmService {
  private apiUrl = environment.apiUrl || 'http://localhost:8080/api';  // Use environment configuration
  private isBrowser: boolean;  // Flag to check if running in browser environment

  // Shared state for components - RxJS Subjects for reactive state management
  private projectsSubject = new BehaviorSubject<Project[]>([]);  // Stores projects list
  public projects$ = this.projectsSubject.asObservable();  // Observable for components to subscribe

  private modulesSubject = new BehaviorSubject<TestModule[]>([]);  // Stores modules list
  public modules$ = this.modulesSubject.asObservable();  // Observable for components to subscribe

  constructor(
    private http: HttpClient,      // Angular's HTTP client for API calls
    private authService: AuthService,  // Service for authentication management
    @Inject(PLATFORM_ID) private platformId: Object  // SSR: distinguish between browser and server
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);  // Check if client-side or server-side

    // Only load initial data in browser environment (not in Server-Side Rendering)
    if (this.isBrowser) {
      // Load initial projects list when service is created
      this.loadProjects().subscribe();
    }
  }

  // ==================== PROJECT METHODS ====================

  /**
   * Get all projects from the backend
   * @returns Observable<Project[]> - Stream of projects array
   */
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`)
      .pipe(
        tap(projects => this.projectsSubject.next(projects)),  // Update shared state
        catchError(this.handleError<Project[]>('getProjects', []))  // Handle errors
      );
  }

  /**
   * Get a single project by ID
   * @param id - Project ID
   * @returns Observable<Project> - Stream of single project
   */
  getProject(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/projects/${id}`)
      .pipe(
        catchError(this.handleError<Project>('getProject'))
      );
  }

  /**
   * Create a new project
   * @param project - Project data {name, description?}
   * @returns Observable<Project> - Stream of created project
   */
  createProject(project: { name: string; description?: string }): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/projects`, project)
      .pipe(
        tap(() => this.loadProjects().subscribe()), // Refresh projects list after creation
        catchError(this.handleError<Project>('createProject'))
      );
  }

  /**
   * Delete a project by ID
   * @param id - Project ID to delete
   * @returns Observable<void> - Stream indicating completion
   */
  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${id}`)
      .pipe(
        tap(() => this.loadProjects().subscribe()), // Refresh projects list after deletion
        catchError(this.handleError<void>('deleteProject'))
      );
  }

  // ==================== MODULE METHODS ====================

  /**
   * Get a single module by ID with full details
   * @param id - Module ID
   * @returns Observable<TestModule> - Stream of single module
   */
  getModule(id: string): Observable<TestModule> {
    return this.http.get<TestModule>(`${this.apiUrl}/testmodules/${id}`)
      .pipe(
        catchError(this.handleError<TestModule>('getModule'))
      );
  }

  /**
   * Create a new module within a project
   * @param projectId - Parent project ID
   * @param module - Module data {name, description?}
   * @returns Observable<TestModule> - Stream of created module
   */
  createModule(projectId: string, module: { name: string; description?: string }): Observable<TestModule> {
    return this.http.post<TestModule>(`${this.apiUrl}/projects/${projectId}/testmodules`, module)
      .pipe(
        catchError(this.handleError<TestModule>('createModule'))
      );
  }

  /**
   * Update an existing module
   * @param id - Module ID to update
   * @param updates - Updated data {name?, description?}
   * @returns Observable<TestModule> - Stream of updated module
   */
  updateModule(id: string, updates: { name?: string; description?: string }): Observable<TestModule> {
    return this.http.put<TestModule>(`${this.apiUrl}/testmodules/${id}`, updates)
      .pipe(
        catchError(this.handleError<TestModule>('updateModule'))
      );
  }

  /**
   * Delete a module by ID
   * @param id - Module ID to delete
   * @returns Observable<void> - Stream indicating completion
   */
  deleteModule(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testmodules/${id}`)
      .pipe(
        catchError(this.handleError<void>('deleteModule'))
      );
  }

  // ==================== TEST SUITE METHODS ====================

  /**
   * Create a new test suite within a module
   * @param moduleId - Parent module ID
   * @param suite - Suite data {name}
   * @returns Observable<TestSuite> - Stream of created suite
   */
  createTestSuite(moduleId: string, suite: { name: string }): Observable<TestSuite> {
    return this.http.post<TestSuite>(`${this.apiUrl}/testmodules/${moduleId}/testsuites`, suite)
      .pipe(
        catchError(this.handleError<TestSuite>('createTestSuite'))
      );
  }

  /**
   * Update an existing test suite
   * @param id - Suite ID to update
   * @param updates - Updated data {name?}
   * @returns Observable<TestSuite> - Stream of updated suite
   */
  updateTestSuite(id: string, updates: { name?: string }): Observable<TestSuite> {
    return this.http.put<TestSuite>(`${this.apiUrl}/testsuites/${id}`, updates)
      .pipe(
        catchError(this.handleError<TestSuite>('updateTestSuite'))
      );
  }

  /**
   * Delete a test suite by ID
   * @param id - Suite ID to delete
   * @returns Observable<void> - Stream indicating completion
   */
  deleteTestSuite(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testsuites/${id}`)
      .pipe(
        catchError(this.handleError<void>('deleteTestSuite'))
      );
  }

  // ==================== TEST CASE METHODS ====================

  /**
   * Get all test cases in the system
   * @returns Observable<TestCase[]> - Stream of all test cases
   */
  getAllTestCases(): Observable<TestCase[]> {
    return this.http.get<TestCase[]>(`${this.apiUrl}/testcases`)
      .pipe(
        catchError(this.handleError<TestCase[]>('getAllTestCases', []))
      );
  }

  /**
   * Get a single test case by ID
   * @param id - Test case ID
   * @returns Observable<TestCase> - Stream of single test case
   */
  getTestCase(id: string): Observable<TestCase> {
    return this.http.get<TestCase>(`${this.apiUrl}/testcases/${id}`)
      .pipe(
        catchError(this.handleError<TestCase>('getTestCase'))
      );
  }

  /**
   * Create a new test case within a suite
   * @param suiteId - Parent suite ID
   * @param testCase - Test case data (including steps)
   * @returns Observable<TestCase> - Stream of created test case
   */
  createTestCase(suiteId: string, testCase: any): Observable<TestCase> {
    return this.http.post<TestCase>(`${this.apiUrl}/testsuites/${suiteId}/testcases`, testCase)
      .pipe(
        catchError(this.handleError<TestCase>('createTestCase'))
      );
  }

  /**
   * Update an existing test case
   * @param id - Test case ID to update
   * @param testCase - Updated test case data (including steps)
   * @returns Observable<TestCase> - Stream of updated test case
   */
  updateTestCase(id: string, testCase: any): Observable<TestCase> {
    return this.http.put<TestCase>(`${this.apiUrl}/testcases/${id}`, testCase)
      .pipe(
        catchError(this.handleError<TestCase>('updateTestCase'))
      );
  }

  /**
   * Delete a test case by ID
   * @param id - Test case ID to delete
   * @returns Observable<void> - Stream indicating completion
   */
  deleteTestCase(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testcases/${id}`)
      .pipe(
        catchError(this.handleError<void>('deleteTestCase'))
      );
  }

  // ==================== TEST EXECUTION METHODS ====================

  /**
   * Execute a test case (create a new test execution record)
   * @param testCaseId - ID of the test case to execute
   * @returns Observable<TestExecution> - Stream of created execution
   */
  executeTestCase(testCaseId: string): Observable<TestExecution> {
    return this.http.post<TestExecution>(`${this.apiUrl}/testcases/${testCaseId}/executions`, {})
      .pipe(
        catchError(this.handleError<TestExecution>('executeTestCase'))
      );
  }

  /**
   * Get all executions for a specific test case
   * @param testCaseId - Test case ID
   * @returns Observable<TestExecution[]> - Stream of execution records
   */
  getTestCaseExecutions(testCaseId: string): Observable<TestExecution[]> {
    return this.http.get<TestExecution[]>(`${this.apiUrl}/testcases/${testCaseId}/executions`)
      .pipe(
        catchError(this.handleError<TestExecution[]>('getTestCaseExecutions', []))
      );
  }

  /**
   * Get a single test execution by ID
   * @param id - Execution ID
   * @returns Observable<TestExecution> - Stream of single execution
   */
  getExecution(id: string): Observable<TestExecution> {
    return this.http.get<TestExecution>(`${this.apiUrl}/executions/${id}`)
      .pipe(
        catchError(this.handleError<TestExecution>('getExecution'))
      );
  }

  /**
   * Get all test executions assigned to the current user
   * @returns Observable<TestExecution[]> - Stream of assigned executions
   */
  getMyAssignedExecutions(): Observable<TestExecution[]> {
    return this.http.get<TestExecution[]>(`${this.apiUrl}/executions/my-assignments`)
      .pipe(
        catchError(this.handleError<TestExecution[]>('getMyAssignedExecutions', []))
      );
  }

  /**
   * Complete a test execution
   * @param executionId - ID of the execution to complete
   * @param overallResult - Final result (PASS, FAIL, BLOCKED)
   * @param notes - Optional notes about the execution
   * @returns Observable<TestExecution> - Stream of completed execution
   */
  completeExecution(executionId: string, overallResult: string, notes: string): Observable<TestExecution> {
    return this.http.put<TestExecution>(`${this.apiUrl}/executions/${executionId}/complete`, {
      overallResult,
      notes
    })
      .pipe(
        catchError(this.handleError<TestExecution>('completeExecution'))
      );
  }

  /**
   * Update a step result in a test execution
   * @param executionId - ID of the execution
   * @param stepId - ID of the step to update
   * @param status - New status (PASS, FAIL, BLOCKED, NOT_EXECUTED)
   * @param actualResult - Actual result description
   * @returns Observable<any> - Stream of updated step result
   */
  updateStepResult(executionId: string, stepId: string, status: string, actualResult: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/executions/${executionId}/steps/${stepId}`, {
      status,
      actualResult
    })
      .pipe(
        catchError(this.handleError('updateStepResult', {}))
      );
  }

  // ==================== ASSIGNMENT METHODS ====================

  /**
   * Assign a QA/BA user to a project (ADMIN only)
   * @param request Project assignment request
   * @returns Observable<User> Updated user with assignments
   */
  assignUserToProject(request: ProjectAssignmentRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/projects/assign`, request)
      .pipe(
        catchError(this.handleError<User>('assignUserToProject'))
      );
  }

  /**
   * Remove a QA/BA user from a project assignment (ADMIN only)
   * @param request Project assignment request
   * @returns Observable<User> Updated user with assignments
   */
  removeUserFromProject(request: ProjectAssignmentRequest): Observable<User> {
    return this.http.delete<User>(`${this.apiUrl}/projects/assign`, { body: request })
      .pipe(
        catchError(this.handleError<User>('removeUserFromProject'))
      );
  }

  /**
   * Get all projects assigned to the current user
   * @returns Observable<Project[]> List of assigned projects
   */
  getProjectsAssignedToCurrentUser(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects/assigned-to-me`)
      .pipe(
        catchError(this.handleError<Project[]>('getProjectsAssignedToCurrentUser', []))
      );
  }

  /**
   * Get all users assigned to a specific project (ADMIN only)
   * @param projectId ID of the project
   * @returns Observable<User[]> List of assigned users
   */
  getUsersAssignedToProject(projectId: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/projects/${projectId}/assigned-users`)
      .pipe(
        catchError(this.handleError<User[]>('getUsersAssignedToProject', []))
      );
  }

  /**
   * Assign a TESTER (or QA/BA) user to a test module (ADMIN/QA/BA)
   * @param request Module assignment request
   * @returns Observable<User> Updated user with assignments
   */
  assignUserToTestModule(request: ModuleAssignmentRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/testmodules/assign`, request)
      .pipe(
        catchError(this.handleError<User>('assignUserToTestModule'))
      );
  }

  /**
   * Remove a user from a test module assignment (ADMIN/QA/BA)
   * @param request Module assignment request
   * @returns Observable<User> Updated user with assignments
   */
  removeUserFromTestModule(request: ModuleAssignmentRequest): Observable<User> {
    return this.http.delete<User>(`${this.apiUrl}/testmodules/assign`, { body: request })
      .pipe(
        catchError(this.handleError<User>('removeUserFromTestModule'))
      );
  }

  /**
   * Get all test modules assigned to the current user
   * @returns Observable<TestModule[]> List of assigned test modules
   */
  getTestModulesAssignedToCurrentUser(): Observable<TestModule[]> {
    return this.http.get<TestModule[]>(`${this.apiUrl}/testmodules/assigned-to-me`)
      .pipe(
        catchError(this.handleError<TestModule[]>('getTestModulesAssignedToCurrentUser', []))
      );
  }

  /**
   * Get all users assigned to a specific test module (ADMIN/QA/BA)
   * @param moduleId ID of the test module
   * @returns Observable<User[]> List of assigned users
   */
  getUsersAssignedToTestModule(moduleId: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/testmodules/${moduleId}/assigned-users`)
      .pipe(
        catchError(this.handleError<User[]>('getUsersAssignedToTestModule', []))
      );
  }

  /**
   * Get all users with a specific role (ADMIN/QA/BA)
   * @param roleName Role name (e.g., "QA", "BA", "TESTER")
   * @returns Observable<User[]> List of users with that role
   */
  getUsersByRole(roleName: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users/by-role/${roleName}`)
      .pipe(
        catchError(this.handleError<User[]>('getUsersByRole', []))
      );
  }

  // ==================== UTILITY METHODS ====================

  /**
   * Load and cache projects - used by multiple components
   * @returns Observable<Project[]> - Stream of projects
   */
  private loadProjects(): Observable<Project[]> {
    return this.getProjects();
  }

  /**
   * Error handling utility - shared across all API calls
   * @param operation - Name of the operation that failed
   * @param result - Value to return if operation fails
   * @returns Error handling function
   */
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      console.error(`${operation} error status:`, error.status);
      console.error(`${operation} error status text:`, error.statusText);
      console.error(`${operation} error message:`, error.message);
      console.error(`${operation} error body:`, error.error);

      // Note: 401 handling is now also done in auth.interceptor.ts
      // But keeping this for service-specific logging/handling if needed is fine.

      // Handle 409 Conflict errors specifically for duplicate projects
      if (error.status === 409) {
        console.error('Conflict error (likely duplicate project name):', error);
        throw error;
      }

      // Return empty result or rethrow based on result parameter
      if (result !== undefined) {
        return of(result as T);
      } else {
        throw error;
      }
    };
  }

  /**
   * Refresh all cached data
   */
  refreshData() {
    this.loadProjects().subscribe();
    // Could emit events to components to refresh their data
  }

  /**
   * Wait for authentication to be fully synchronized
   * This ensures all tokens are properly set before making API calls
   * @param maxWaitTime Maximum time to wait in milliseconds
   * @returns Promise that resolves when auth is synchronized
   */
  waitForAuthSync(maxWaitTime: number = 5000): Promise<boolean> {
    return this.authService.waitForAuthSync(maxWaitTime);
  }
}
