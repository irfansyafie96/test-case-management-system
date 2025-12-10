import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Auth Guard (Functional)
 * 
 * Protects routes from unauthorized access.
 * Checks if the user is authenticated:
 * - If YES: Allows navigation to proceed.
 * - If NO: Redirects to the login page.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // User is not authenticated, redirect to login
  console.log('AuthGuard: Access denied, redirecting to login');
  return router.createUrlTree(['/login']);
};
