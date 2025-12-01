
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, of, map } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { Project, TestModule, TestSuite, TestCase, TestExecution, TestStepResult } from '../models/project.model';

// TCM Service - Centralized API service shared across components
@Injectable({
  providedIn: 'root'
})
export class TcmService {
  private apiUrl = '/api';
  private isBrowser: boolean;

  // Shared state for components - demonstrating service state management
  private projectsSubject = new BehaviorSubject<Project[]>([]);
  public projects$ = this.projectsSubject.asObservable();

  private modulesSubject = new BehaviorSubject<TestModule[]>([]);
  public modules$ = this.modulesSubject.asObservable();

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);

    // Only load initial data in browser environment (not SSR)
    if (this.isBrowser) {
      // Load initial data on service creation
      this.loadProjects().subscribe();
    }
  }

  // Helper method to get auth headers
  private getAuthHeaders(): HttpHeaders {
    let jwtToken = '';
    if (this.isBrowser) {
      jwtToken = localStorage.getItem('jwtToken') || '';
    }
    return new HttpHeaders({
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    });
  }

  // ==================== PROJECTS ====================

  // Get all projects - used by sidebar and dashboard
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(projects => this.projectsSubject.next(projects)),
        catchError(this.handleError<Project[]>('getProjects', []))
      );
  }

  // Get single project with modules
  getProject(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/projects/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<Project>('getProject'))
      );
  }

  // Create project
  createProject(project: { name: string; description?: string }): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}/projects`, project, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => this.loadProjects().subscribe()), // Refresh projects list
        catchError(this.handleError<Project>('createProject'))
      );
  }

  // Delete project
  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/projects/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        tap(() => this.loadProjects().subscribe()), // Refresh projects list
        catchError(this.handleError<void>('deleteProject'))
      );
  }

  // ==================== MODULES ====================

  // Get single module with full details
  getModule(id: string): Observable<TestModule> {
    return this.http.get<TestModule>(`${this.apiUrl}/testmodules/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestModule>('getModule'))
      );
  }

  // Create module
  createModule(projectId: string, module: { name: string }): Observable<TestModule> {
    return this.http.post<TestModule>(`${this.apiUrl}/projects/${projectId}/testmodules`, module, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestModule>('createModule'))
      );
  }

  // Update module
  updateModule(id: string, updates: { name?: string; description?: string }): Observable<TestModule> {
    return this.http.put<TestModule>(`${this.apiUrl}/testmodules/${id}`, updates, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestModule>('updateModule'))
      );
  }

  // Delete module
  deleteModule(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testmodules/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<void>('deleteModule'))
      );
  }

  // ==================== TEST SUITES ====================

  // Create test suite
  createTestSuite(moduleId: string, suite: { name: string }): Observable<TestSuite> {
    return this.http.post<TestSuite>(`${this.apiUrl}/testmodules/${moduleId}/testsuites`, suite, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestSuite>('createTestSuite'))
      );
  }

  // Update test suite
  updateTestSuite(id: string, updates: { name?: string }): Observable<TestSuite> {
    return this.http.put<TestSuite>(`${this.apiUrl}/testsuites/${id}`, updates, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestSuite>('updateTestSuite'))
      );
  }

  // Delete test suite
  deleteTestSuite(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testsuites/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<void>('deleteTestSuite'))
      );
  }

  // ==================== TEST CASES ====================

  // Get single test case
  getTestCase(id: string): Observable<TestCase> {
    return this.http.get<TestCase>(`${this.apiUrl}/testcases/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestCase>('getTestCase'))
      );
  }

  // Create test case
  createTestCase(suiteId: string, testCase: any): Observable<TestCase> {
    return this.http.post<TestCase>(`${this.apiUrl}/testsuites/${suiteId}/testcases`, testCase, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestCase>('createTestCase'))
      );
  }

  // Update test case
  updateTestCase(id: string, testCase: any): Observable<TestCase> {
    return this.http.put<TestCase>(`${this.apiUrl}/testcases/${id}`, testCase, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestCase>('updateTestCase'))
      );
  }

  // Delete test case
  deleteTestCase(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/testcases/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<void>('deleteTestCase'))
      );
  }

  // ==================== TEST EXECUTIONS ====================

  // Execute test case
  executeTestCase(testCaseId: string): Observable<TestExecution> {
    return this.http.post<TestExecution>(`${this.apiUrl}/testcases/${testCaseId}/executions`, {}, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestExecution>('executeTestCase'))
      );
  }

  // Get executions for a test case
  getTestCaseExecutions(testCaseId: string): Observable<TestExecution[]> {
    return this.http.get<TestExecution[]>(`${this.apiUrl}/testcases/${testCaseId}/executions`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestExecution[]>('getTestCaseExecutions', []))
      );
  }

  // Get single execution
  getExecution(id: string): Observable<TestExecution> {
    return this.http.get<TestExecution>(`${this.apiUrl}/executions/${id}`, { headers: this.getAuthHeaders() })
      .pipe(
        catchError(this.handleError<TestExecution>('getExecution'))
      );
  }

  // ==================== UTILITY METHODS ====================

  // Load and cache projects - used by multiple components
  private loadProjects(): Observable<Project[]> {
    return this.getProjects();
  }

  // Error handling utility - shared across all API calls
  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed:`, error);
      // Could send error to error reporting service here

      // Return empty result or rethrow based on result parameter
      if (result !== undefined) {
        return of(result as T);
      } else {
        throw error;
      }
    };
  }

  // Cache management - demonstrating service-level state management
  refreshData() {
    this.loadProjects().subscribe();
    // Could emit events to components to refresh their data
  }
}

