
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, of, map } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { Project, TestModule, TestSuite, TestCase, TestExecution, TestStepResult } from '../models/project.model';
import { AuthService } from './auth.service';

/**
 * TCM Service - Main API Service for the Test Case Management System
 *
 * This service handles all communication with the backend API, providing
 * methods to interact with the test case hierarchy: Projects → Modules → Suites → Test Cases → Test Steps
 * and their executions/results.
 *
 * Key Features:
 * - HTTP API communication with proper authentication headers
 * - Shared state management for projects and modules
 * - Error handling with automatic logout on authentication failures
 * - Server-Side Rendering (SSR) compatibility
 */
@Injectable({
  providedIn: 'root'  // Singleton service, available app-wide
})
export class TcmService {
  private apiUrl = '/api';  // Base API URL (proxied through proxy.conf.json)
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

  // Helper method to get authentication headers (JWT token)
  private getAuthHeaders(): HttpHeaders {
    const authHeaders = this.authService.getAuthHeaders();
    return new HttpHeaders(authHeaders);
  }

  // ==================== PROJECT METHODS ====================

  /**
   * Get all projects from the backend
   * @returns Observable<Project[]> - Stream of projects array
   */
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`, { headers: this.getAuthHeaders() })
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
    return this.http.get<Project>(`${this.apiUrl}/projects/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.post<Project>(`${this.apiUrl}/projects`, project, { headers: this.getAuthHeaders() })
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
    return this.http.delete<void>(`${this.apiUrl}/projects/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.get<TestModule>(`${this.apiUrl}/testmodules/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.post<TestModule>(`${this.apiUrl}/projects/${projectId}/testmodules`, module, { headers: this.getAuthHeaders() })
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
    return this.http.put<TestModule>(`${this.apiUrl}/testmodules/${id}`, updates, { headers: this.getAuthHeaders() })
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
    return this.http.delete<void>(`${this.apiUrl}/testmodules/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.post<TestSuite>(`${this.apiUrl}/testmodules/${moduleId}/testsuites`, suite, { headers: this.getAuthHeaders() })
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
    return this.http.put<TestSuite>(`${this.apiUrl}/testsuites/${id}`, updates, { headers: this.getAuthHeaders() })
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
    return this.http.delete<void>(`${this.apiUrl}/testsuites/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.get<TestCase[]>(`${this.apiUrl}/testcases`, { headers: this.getAuthHeaders() })
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
    return this.http.get<TestCase>(`${this.apiUrl}/testcases/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.post<TestCase>(`${this.apiUrl}/testsuites/${suiteId}/testcases`, testCase, { headers: this.getAuthHeaders() })
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
    return this.http.put<TestCase>(`${this.apiUrl}/testcases/${id}`, testCase, { headers: this.getAuthHeaders() })
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
    return this.http.delete<void>(`${this.apiUrl}/testcases/${id}`, { headers: this.getAuthHeaders() })
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
    return this.http.post<TestExecution>(`${this.apiUrl}/testcases/${testCaseId}/executions`, {}, { headers: this.getAuthHeaders() })
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
    return this.http.get<TestExecution[]>(`${this.apiUrl}/testcases/${testCaseId}/executions`, { headers: this.getAuthHeaders() })
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
    return this.http.get<TestExecution>(`${this.apiUrl}/executions/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestExecution>('getExecution'))
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

      // Handle 401 Unauthorized errors by logging out the user
      if (error.status === 401) {
        console.error('Unauthorized request - logging out user');
        // The auth service will handle the logout and navigation
        this.authService.logout();
      }

      // Handle 409 Conflict errors specifically for duplicate projects
      if (error.status === 409) {
        console.error('Conflict error (likely duplicate project name):', error);
        // Don't logout user for validation conflicts
        // Just re-throw the error so the component can handle it
        throw error;
      }

      // Could send error to error reporting service here

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
}

