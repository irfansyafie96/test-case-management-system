import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, retry, timer } from 'rxjs';
import { AuthService } from './services/auth.service';
import { finalize } from 'rxjs/operators';

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
          if (!req.url.includes('/auth/login')) {
            // Don't call authService.logout() here to avoid recursion
          }
        }
        return throwError(() => error);
      })
    );
  }

  // For non-authentication requests, we need CSRF protection (state-changing requests)
  const needsCsrf = req.method !== 'GET' && req.method !== 'HEAD' && req.method !== 'OPTIONS';
  const isCsrfRefreshEndpoint = req.url.includes('/api/auth/csrf');

  let authReq: HttpRequest<unknown>;
  if (isCsrfRefreshEndpoint) {
    // Special handling for CSRF refresh endpoint - no authentication required
    authReq = req.clone({
      withCredentials: true  // Include cookies for CSRF token generation
    });
  } else if (needsCsrf) {
    // Get CSRF token from cookie (JWT token doesn't need to be read - it's HttpOnly)
    const csrfToken = getCookie('XSRF-TOKEN');

    if (csrfToken) {
      // Clone request to include both credentials and CSRF token
      authReq = req.clone({
        withCredentials: true,  // Include cookies (JWT token) in requests
        setHeaders: {
          'X-XSRF-TOKEN': csrfToken  // Include CSRF token in header
        }
      });
    } else {
      // If CSRF token is missing for state-changing request, make the request anyway
      // The backend may handle missing CSRF tokens differently than expected
      authReq = req.clone({
        withCredentials: true  // Include cookies (JWT token) in requests
      });
    }
  } else {
    // For requests that don't need CSRF
    authReq = req.clone({
      withCredentials: true  // Include cookies (JWT token) in requests
    });
  }

  // Pass on to the next interceptor chain with enhanced retry logic for CSRF token issues
  return next(authReq).pipe(
    retry({
      count: 3, // Increased retry count to 3 times
      delay: (error, retryIndex) => {
        // Only retry on 401 errors for state-changing requests that might have CSRF token issues
        if (error.status === 401 && needsCsrf && !getCookie('XSRF-TOKEN')) {
          // For the first retry, trigger a CSRF token refresh
          if (retryIndex === 0) {
            authService.refreshCsrfToken().subscribe();
          }
          
          // Exponential backoff: 200ms, 400ms, 800ms
          return timer(200 * Math.pow(2, retryIndex));
        }
        // Don't retry for other errors
        throw error;
      }
    }),
    catchError((error: HttpErrorResponse) => {
      // Enhanced error handling for different scenarios
      if (error.status === 401) {
        // Check if this is the CSRF refresh endpoint (shouldn't trigger logout)
        const isCsrfRefreshEndpoint = req.url.includes('/api/auth/csrf');

        if (isCsrfRefreshEndpoint) {
          // This endpoint is specifically for CSRF token sync, not for authentication
          // Don't treat it as an authentication failure that triggers logout
          return throwError(() => error);
        }

        // Check if this is likely a CSRF token issue vs. authentication issue
        const hasCsrfToken = !!getCookie('XSRF-TOKEN');
        // Note: We can't directly check for HttpOnly JWT tokens from JavaScript,
        // so we rely on the user data stored in localStorage to determine authentication state
        const hasStoredUser = authService.getCurrentUser() !== null;

        if (!hasCsrfToken && hasStoredUser && needsCsrf) {
          // Instead of failing, try to refresh the CSRF token and retry automatically
          // Make a request to refresh CSRF token
          authService.refreshCsrfToken().subscribe();
          
          // Don't logout immediately - let the user try again
          return throwError(() => ({
            ...error,
            isCsrfTokenIssue: true,
            userMessage: 'Security token synchronization issue. Please try again.'
          }));
        } else if (!hasStoredUser) {
          // No stored user - logout regardless of request type
          authService.logout();
          return throwError(() => error);
        } else {
          // User data exists but authentication still failed
          // This indicates the JWT token in the HttpOnly cookie is no longer valid
          // Logout the user since their authentication is indeed invalid
          authService.logout();
          return throwError(() => error);
        }
      }
      return throwError(() => error);
    })
  );
};

/**
 * Helper function to get a cookie value by name
 */
function getCookie(name: string): string | null {
  // More robust cookie reading function
  if (typeof document === 'undefined' || !document.cookie) {
    return null;
  }

  const nameEq = name + "=";
  const cookies = document.cookie.split(';');

  for (let i = 0; i < cookies.length; i++) {
    let cookie = cookies[i];
    while (cookie.charAt(0) === ' ') {
      cookie = cookie.substring(1, cookie.length);
    }

    if (cookie.indexOf(nameEq) === 0) {
      return cookie.substring(nameEq.length, cookie.length);
    }
  }

  return null;
}
