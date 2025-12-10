import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './services/auth.service';

/**
 * Auth Interceptor (Functional)
 * 
 * Intercepts all outgoing HTTP requests to:
 * 1. Add the JWT Authorization header if a user is logged in.
 * 2. Catch 401 Unauthorized errors globally and redirect to login.
 */
export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Get the auth token from the service
  const token = authService.getToken();
  
  // Clone the request to add the new header
  let authReq = req;
  if (token) {
    authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
  }

  // Pass on the cloned request to the next handler
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
