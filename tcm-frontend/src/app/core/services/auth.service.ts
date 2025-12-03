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
   * Get API URL - handles development proxy configuration
   * In development, requests are proxied through proxy.conf.json to backend
   * @returns API base URL
   */
  private getApiUrl(): string {
    // For development with proxy, use relative path
    // The proxy.conf.json will forward /api requests to http://localhost:8080
    return '/api';
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
   * 2. Receive JWT token and user information if valid
   * 3. Store token and user data in localStorage
   * 4. Update authentication state
   * 5. Navigate to dashboard
   *
   * @param credentials {username, password} from login form
   * @returns Observable with authentication result
   */
  login(credentials: { username: string; password: string }): Observable<any> {
    console.log('Making login request to:', `${this.apiUrl}/auth/login`);
    return this.http.post(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap((response: any) => {
        console.log('Login response received:', response);
        // Check if response contains required data (token, username, id)
        if (response && response.token && response.username && response.id && this.isBrowser) {
          // Convert backend JwtResponse format to User object expected by frontend
          const user = {
            id: response.id.toString(),           // Convert to string
            username: response.username,          // User's login name
            email: response.email || `${response.username}@example.com`, // Fallback if no email
            roles: response.roles || [],          // User's roles (["ADMIN", "QA", etc.])
            enabled: true                         // Account status
          };

          // Store authentication data in localStorage (persists between browser sessions)
          localStorage.setItem('jwtToken', response.token);    // JWT token
          localStorage.setItem('currentUser', JSON.stringify(user));  // User information

          // Update shared authentication state - notifies all subscribers
          this.isAuthenticatedSubject.next(true);    // User is now authenticated
          this.currentUserSubject.next(user);        // Set current user info

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

  /**
   * Logout method - Clear authentication data and navigate to login
   * Process:
   * 1. Remove stored JWT token and user data from localStorage
   * 2. Update authentication state to not authenticated
   * 3. Navigate to login page
   */
  logout(): void {
    if (this.isBrowser) {
      // Clear localStorage (matching your original implementation)
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('currentUser');
    }

    // Update shared authentication state
    this.isAuthenticatedSubject.next(false);   // User is no longer authenticated
    this.currentUserSubject.next(null);        // Clear current user info

    // Navigate to login page
    this.router.navigate(['/login']);
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
   * @returns true if token exists and is valid, false otherwise
   */
  isAuthenticated(): boolean {
    return this.hasValidToken() && this.currentUserSubject.value !== null;
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
   * Get JWT token from localStorage
   * @returns JWT token string or null if not available
   */
  getToken(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('jwtToken');
    }
    return null;
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
   * @returns true if token exists and is not expired, false otherwise
   */
  private hasValidToken(): boolean {
    if (this.isBrowser) {
      const token = localStorage.getItem('jwtToken');
      if (!token) {
        return false;  // No token stored
      }

      const decodedToken = this.decodeToken(token);
      if (!decodedToken) {
        return false;  // Invalid token format
      }

      // Check if token is expired (exp is in seconds since epoch)
      const currentTime = Math.floor(Date.now() / 1000);
      return decodedToken.exp > currentTime;  // Return true if not expired
    }
    return false;
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
   * Checks if user was previously logged in (token exists in localStorage)
   */
  initializeAuth(): void {
    if (this.hasValidToken() && this.getCurrentUser()) {
      // User was previously logged in and token is still valid
      this.isAuthenticatedSubject.next(true);
    } else {
      // Token is invalid or expired, clear any stale data
      this.logout();
    }
  }

  /**
   * Set mock authentication (for development when backend is not available)
   * @param mockUser Mock user object
   * @param token Mock JWT token (defaults to 'mock-token')
   */
  setMockAuth(mockUser: any, token: string = 'mock-token'): void {
    if (this.isBrowser) {
      localStorage.setItem('jwtToken', token);
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
    }

    this.isAuthenticatedSubject.next(true);
    this.currentUserSubject.next(mockUser);
  }

  /**
   * Get authentication headers for API calls
   * Includes JWT token in Authorization header for authenticated requests
   * @returns Headers object with Authorization header if authenticated
   */
  getAuthHeaders(): { [header: string]: string } {
    const token = this.getToken();
    // Check if token is valid before returning headers
    if (token && !this.hasValidToken()) {
      // Token is expired, logout user
      this.logout();
      return {};
    }
    // Return Authorization header with Bearer token if authenticated
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }
}
