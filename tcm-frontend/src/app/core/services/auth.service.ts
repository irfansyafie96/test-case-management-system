import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, tap, catchError, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { User } from '../models/project.model';
import { environment } from '../../../environments/environment';

/**
 * AuthService - Authentication Service for the Test Case Management System
 *
 * Handles all authentication-related operations including:
 * 1. Login: Sends credentials to backend, receives JWT token, stores in localStorage
 * 2. Logout: Clears stored authentication data and navigates to login page
 * 3. Registration: Creates new user accounts
 * 4. Token management: Stores, validates, and manages JWT tokens
 * 5. Role checking: Determines user permissions for access control
 *
 * Uses Reactive Programming (RxJS) with Observables for async operations
 * Implements shared state management for authentication status across components
 */
@Injectable({
  providedIn: 'root'  // Singleton service available app-wide
})
export class AuthService {
  private apiUrl = this.getApiUrl();  // Base API URL for auth endpoints
  private isBrowser: boolean;  // Flag to check if running in browser (not SSR)

  /**
   * Get API URL from environment configuration
   * @returns API base URL
   */
  private getApiUrl(): string {
    return environment.apiUrl || 'http://localhost:8080/api';
  }

  // Shared state management using RxJS Subjects for reactive authentication
  // These allow components to subscribe and react to authentication state changes
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);  // Authentication status
  private currentUserSubject = new BehaviorSubject<User | null>(null);   // Current user details

  // Public observables for components to subscribe to authentication state
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();  // Observable for auth status
  public currentUser$ = this.currentUserSubject.asObservable();          // Observable for current user

  constructor(
    private http: HttpClient,      // Angular's HTTP client for API calls
    private router: Router,        // Angular's router for navigation
    @Inject(PLATFORM_ID) private platformId: Object  // SSR: distinguish between browser/server
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);  // Check if running in browser

    // Only initialize auth state if we're in browser (no Server-Side Rendering)
    if (this.isBrowser) {
      // Initialize authentication state from localStorage on app startup
      this.initializeAuth();
    }
  }

  /**
   * Login method - Authenticate user with backend
   * Process:
   * 1. Send credentials to backend /api/auth/login
   * 2. Receive JWT token as HttpOnly cookie and user information in response
   * 3. Store only user data in localStorage (token is in secure cookie)
   * 4. Update authentication state
   * 5. Navigate to dashboard
   *
   * @param credentials {username, password} from login form
   * @returns Observable with authentication result
   */
  login(credentials: { username: string; password: string }): Observable<any> {
    const loginUrl = `${this.apiUrl}/auth/login`;

    return this.http.post(loginUrl, credentials, { withCredentials: true }).pipe(
      tap((response: any) => {
        // Check if response contains required user data (token is now in HttpOnly cookie)
        if (response && response.username && response.id && this.isBrowser) {
          // Convert backend JwtResponse format to User object expected by frontend
          const user = {
            id: response.id.toString(),           // Convert to string
            username: response.username,          // User's login name
            email: response.email || `${response.username}@example.com`, // Fallback if no email
            roles: response.roles || [],          // User's roles (["ADMIN", "QA", etc.])
            enabled: true                         // Account status
          };

          // Store only user data in localStorage (JWT token is in secure HttpOnly cookie)
          localStorage.setItem('currentUser', JSON.stringify(user));  // User information only

          // Update shared authentication state - notifies all subscribers
          this.isAuthenticatedSubject.next(true);    // User is now authenticated
          this.currentUserSubject.next(user);        // Set current user info

          // Navigate to dashboard after successful login
          this.router.navigate(['/dashboard']);
        } else {
          // Handle case where response doesn't have expected structure
          throw new Error('Invalid login response format');
        }
      }),
      catchError(error => {
        throw new Error('Login failed. Please check your credentials.');
      })
    );
  }

  /**
   * Logout method - Clear authentication data and navigate to login
   * Process:
   * 1. Call backend logout endpoint to clear HttpOnly cookie
   * 2. Remove stored user data from localStorage
   * 3. Update authentication state to not authenticated
   * 4. Navigate to login page
   */
  logout(): void {
    // Call backend logout to clear HttpOnly cookie
    // Using a separate request without the interceptor to avoid 401 loops
    this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => {
        // Clear user data from localStorage
        if (this.isBrowser) {
          localStorage.removeItem('jwtToken');  // Remove any old localStorage tokens
          localStorage.removeItem('currentUser');
        }

        // Update shared authentication state
        this.isAuthenticatedSubject.next(false);   // User is no longer authenticated
        this.currentUserSubject.next(null);        // Clear current user info

        // Navigate to login page
        this.router.navigate(['/login']);
      },
      error: (error) => {
        // Still clear local data even if backend call fails
        if (this.isBrowser) {
          localStorage.removeItem('jwtToken');
          localStorage.removeItem('currentUser');
        }
        this.isAuthenticatedSubject.next(false);
        this.currentUserSubject.next(null);
        this.router.navigate(['/login']);
      }
    });
  }

  /**
   * Register/Signup method - Create a new user account
   * @param userData {username, email, password, roles?} for new user
   * @returns Observable with registration result
   */
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

  /**
   * Check if user is currently authenticated
   * @returns true if user data exists (and by implication token in cookie), false otherwise
   */
  isAuthenticated(): boolean {
    // With HttpOnly cookies, we can't directly verify the JWT token in client-side code
    // So we rely primarily on the user data in localStorage and assume that if it exists,
    // then the corresponding HttpOnly cookie with valid JWT token exists as well
    return this.currentUserSubject.value !== null || this.getCurrentUser() !== null;
  }

  /**
   * Get current user details from stored data
   * @returns User object or null if not authenticated
   */
  getCurrentUser(): User | null {
    // Check if user data is already in memory
    if (this.currentUserSubject.value) {
      return this.currentUserSubject.value;
    }

    // If not in memory, check localStorage
    if (this.isBrowser) {
      const userJson = localStorage.getItem('currentUser');
      if (userJson) {
        try {
          const user = JSON.parse(userJson);
          // Update memory state for future lookups
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

  /**
   * Get JWT token from localStorage (deprecated - now using HttpOnly cookies)
   * Kept for backward compatibility but returns null
   * @returns null - tokens are now stored in secure HttpOnly cookies
   */
  getToken(): string | null {
    return null;  // Tokens are now in HttpOnly cookies, not localStorage
  }

  /**
   * Decode JWT token to extract payload information
   * Useful for checking token expiration without making API call
   * @param token JWT token string to decode
   * @returns decoded token payload or null if invalid
   */
  private decodeToken(token: string): any {
    try {
      const base64Url = token.split('.')[1];  // Extract payload part of JWT
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');  // Base64 decode
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      return JSON.parse(jsonPayload);
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  /**
   * Check if stored JWT token is valid and not expired
   * NOTE: With HttpOnly cookies, this method is deprecated but kept for compatibility
   * In a real implementation, token validation would happen server-side
   * @returns true always as we can't validate HttpOnly tokens client-side
   */
  private hasValidToken(): boolean {
    // Since JWT tokens are now in HttpOnly cookies, client-side validation is not possible
    // This method is kept for compatibility but no longer used for actual validation
    return true;
  }

  /**
   * Check if current user has a specific role
   * @param role Role to check for (e.g., "ADMIN", "QA", "TESTER")
   * @returns true if user has the specified role, false otherwise
   */
  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    return user ? user.roles.includes(role) : false;
  }

  /**
   * Check if current user has any of the provided roles
   * @param roles Array of roles to check for
   * @returns true if user has any of the specified roles, false otherwise
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    return user ? roles.some(role => user.roles.includes(role)) : false;
  }

  /**
   * Refresh current user data from backend
   * Useful after profile updates or when user data might have changed
   * @returns Observable with updated user data
   */
  refreshCurrentUser(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No authentication token available');
    }

    // Make API call to get fresh user data
    return this.http.get(`${this.apiUrl}/auth/me`, {
      headers: { 'Authorization': `Bearer ${token}` }  // Include Bearer token in header
    }).pipe(
      tap((user: any) => {
        if (this.isBrowser) {
          localStorage.setItem('currentUser', JSON.stringify(user));  // Update stored user data
        }
        this.currentUserSubject.next(user);  // Update memory state
      }),
      catchError(error => {
        console.error('Failed to refresh user data:', error);
        // If refresh fails, assume token is invalid and logout user
        this.logout();
        throw error;
      })
    );
  }

  /**
   * Initialize authentication state on app startup
   * Checks if user data exists (and by implication token in cookie)
   */
  initializeAuth(): void {
    if (this.getCurrentUser()) {
      // User was previously logged in and has user data
      this.isAuthenticatedSubject.next(true);
    } else {
      // No user data found, or could validate with backend
      this.isAuthenticatedSubject.next(false);
    }
  }


}
