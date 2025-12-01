import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { User } from '../models/project.model';
import { environment } from '../../../environments/environment';

// Authentication Service - Shared across all components needing auth
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = this.getApiUrl();
  private isBrowser: boolean;

  private getApiUrl(): string {
    // For development with proxy, use relative path
    // The proxy.conf.json will forward /api requests to http://localhost:8080
    return '/api';
  }

  // Shared authentication state - components can subscribe to this
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  private currentUserSubject = new BehaviorSubject<User | null>(null);

  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);

    // Only initialize auth state if we're in browser (no SSR)
    if (this.isBrowser) {
      // Initialize authentication state
      this.initializeAuth();
    }
  }

  // Login - matches your existing backend API
  login(credentials: { username: string; password: string }): Observable<any> {
    console.log('Making login request to:', `${this.apiUrl}/auth/login`);
    return this.http.post(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((response: any) => {
        console.log('Login response received:', response);
        if (response && response.token && response.username && response.id && this.isBrowser) {
          // Convert JwtResponse format to User object expected by frontend
          const user = {
            id: response.id.toString(),
            username: response.username,
            email: response.email || `${response.username}@example.com`, // fallback if no email
            roles: response.roles || [],
            enabled: true
          };

          // Store auth data in localStorage (matching your original implementation)
          localStorage.setItem('jwtToken', response.token);
          localStorage.setItem('currentUser', JSON.stringify(user));

          // Update shared state
          this.isAuthenticatedSubject.next(true);
          this.currentUserSubject.next(user);

          console.log('Login successful, navigating to dashboard...');
          // Navigate to dashboard after successful login
          this.router.navigate(['/dashboard']);
        } else {
          // Handle case where response doesn't have expected structure
          console.warn('Login response does not contain expected token/user structure:', response);
          throw new Error('Invalid login response format');
        }
      }),
      catchError(error => {
        console.error('Login API failed:', error);
        // Don't expose sensitive error details to components
        throw new Error('Login failed. Please check your credentials.');
      })
    );
  }

  // Logout - clean up and navigate to login
  logout(): void {
    if (this.isBrowser) {
      // Clear localStorage (matching your original implementation)
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('currentUser');
    }

    // Update shared state
    this.isAuthenticatedSubject.next(false);
    this.currentUserSubject.next(null);

    // Navigate to login page
    this.router.navigate(['/login']);
  }

  // Register/Signup
  register(userData: { username: string; email: string; password: string; roles?: string[] }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/signup`, userData).pipe(
      tap(response => {
        console.log('Registration successful:', response);
        // Could auto-login after registration or just navigate to login
      }),
      catchError(error => {
        console.error('Registration failed:', error);
        throw error;
      })
    );
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return this.hasValidToken() && this.currentUserSubject.value !== null;
  }

  // Get current user
  getCurrentUser(): User | null {
    if (this.currentUserSubject.value) {
      return this.currentUserSubject.value;
    }

    if (this.isBrowser) {
      const userJson = localStorage.getItem('currentUser');
      if (userJson) {
        try {
          const user = JSON.parse(userJson);
          this.currentUserSubject.next(user);
          return user;
        } catch (error) {
          console.error('Error parsing current user:', error);
          return null;
        }
      }
    }
    return null;
  }

  // Get JWT token
  getToken(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('jwtToken');
    }
    return null;
  }

  // Check if token exists and is valid (basic check)
  private hasValidToken(): boolean {
    if (this.isBrowser) {
      const token = localStorage.getItem('jwtToken');
      return !!token; // Could add JWT expiration check here
    }
    return false;
  }

  // Check user roles
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? user.roles.includes(role) : false;
  }

  // Check if user has any of the provided roles
  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    return user ? roles.some(role => user.roles.includes(role)) : false;
  }

  // Refresh user data (useful after profile updates)
  refreshCurrentUser(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No authentication token available');
    }

    // Make API call to get fresh user data
    return this.http.get(`${this.apiUrl}/auth/me`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(
      tap((user: any) => {
        if (this.isBrowser) {
          localStorage.setItem('currentUser', JSON.stringify(user));
        }
        this.currentUserSubject.next(user);
      }),
      catchError(error => {
        console.error('Failed to refresh user data:', error);
        // If refresh fails, assume token is invalid
        this.logout();
        throw error;
      })
    );
  }

  // Initialize auth state on app startup
  initializeAuth(): void {
    if (this.hasValidToken() && this.getCurrentUser()) {
      this.isAuthenticatedSubject.next(true);
    } else {
      // Clear any stale data
      this.logout();
    }
  }

  // Public method for mock authentication (for development when backend is not available)
  setMockAuth(mockUser: any, token: string = 'mock-token'): void {
    if (this.isBrowser) {
      localStorage.setItem('jwtToken', token);
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
    }

    this.isAuthenticatedSubject.next(true);
    this.currentUserSubject.next(mockUser);
  }

  // Utility method to get auth headers (used by other services)
  getAuthHeaders(): { [header: string]: string } {
    const token = this.getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }
}
