import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './services/auth.service';

/**
 * Auth Interceptor (Functional)
 *
 * Intercepts all outgoing HTTP requests to:
 * 1. Include credentials (cookies) for JWT token transmission.
 * 2. Include CSRF token for state-changing requests (except authentication).
 * 3. Catch 401 Unauthorized errors globally and redirect to login.
 */
export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check if this is an authentication request
  const isAuthRequest = req.url.includes('/auth/');

  // Special handling for logout request (needs CSRF token since it's a POST)
  if (req.url.includes('/auth/logout')) {
    const csrfToken = getCookie('XSRF-TOKEN');
    let authReq = req.clone({
      withCredentials: true,  // Include cookies (JWT token) in requests
      setHeaders: csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {} // Include CSRF token if available
    });

    return next(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // For logout errors, don't trigger another logout (to avoid infinite loops)
        if (error.status === 401) {
          console.error('Logout failed - may already be logged out or token invalid');
          // Still clear local storage even if backend call failed
          if (typeof window !== 'undefined' && window.localStorage) {
            localStorage.removeItem('currentUser');
          }
          // Update auth state to not authenticated without triggering another logout
          authService['isAuthenticatedSubject']?.next(false);
          authService['currentUserSubject']?.next(null);
        }
        // Always navigate to login after logout attempt
        router.navigate(['/login']);
        return throwError(() => error);
      })
    );
  }

  // For other authentication requests (login, etc.), we don't need CSRF tokens
  if (isAuthRequest) {
    let authReq = req.clone({
      withCredentials: true  // Include cookies (JWT token) in requests
    });

    return next(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle 401 Unauthorized errors, but don't create infinite loops during auth requests
        if (error.status === 401) {
          if (req.url.includes('/auth/login')) {
            console.error('Login failed - invalid credentials');
          } else {
            console.error('Interceptor caught 401 Unauthorized on auth request');
            // Don't call authService.logout() here to avoid recursion
          }
        }
        return throwError(() => error);
      })
    );
  }

  // For non-authentication requests, we need CSRF protection (state-changing requests)
  const needsCsrf = req.method !== 'GET' && req.method !== 'HEAD' && req.method !== 'OPTIONS';

  if (needsCsrf) {
    // Get CSRF token from cookie
    const csrfToken = getCookie('XSRF-TOKEN');

    if (csrfToken) {
      // Clone request to include both credentials and CSRF token
      let authReq = req.clone({
        withCredentials: true,  // Include cookies (JWT token) in requests
        setHeaders: {
          'X-XSRF-TOKEN': csrfToken  // Include CSRF token in header
        }
      });

      return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          // Handle 401 Unauthorized errors
          if (error.status === 401) {
            console.error('Interceptor caught 401 Unauthorized - logging out');
            authService.logout();
          }
          return throwError(() => error);
        })
      );
    }
  }

  // For requests that don't need CSRF or when CSRF token isn't available
  let authReq = req.clone({
    withCredentials: true  // Include cookies (JWT token) in requests
  });

  // Pass on to the next interceptor chain.
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized errors
      if (error.status === 401) {
        console.error('Interceptor caught 401 Unauthorized - logging out');
        authService.logout();
      }
      return throwError(() => error);
    })
  );
};

/**
 * Helper function to get a cookie value by name
 */
function getCookie(name: string): string | null {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()?.split(';').shift() || null;
  return null;
}
