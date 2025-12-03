import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { filter } from 'rxjs';
import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { HeaderComponent } from './shared/header/header.component';
import { AuthService } from './core/services/auth.service';

/**
 * Main Application Component - Root Component of the Test Case Management System
 *
 * This is the top-level component that orchestrates the entire application:
 * - Sets up authentication state on initialization
 * - Manages sidebar visibility based on current route
 * - Provides the main layout structure
 * - Handles Server-Side Rendering (SSR) compatibility
 *
 * The component uses:
 * - RouterOutlet: To display different views based on current route
 * - SidebarComponent: Navigation sidebar (shown on authenticated routes)
 * - HeaderComponent: Top header with user info and navigation
 */
@Component({
  selector: 'app-root',  // CSS selector for this component
  imports: [            // Components this component uses
    CommonModule,      // Angular's common directives (ngIf, ngFor, etc.)
    RouterOutlet,      // Router outlet for displaying routed components
    SidebarComponent,  // Sidebar navigation component
    HeaderComponent    // Top header component
  ],
  templateUrl: './app.html',  // HTML template
  styleUrl: './app.css'       // CSS styles
})
export class App implements OnInit {
  // App-level title - displayed in the header
  title = 'Test Case Management System';

  // Flag to control sidebar visibility
  showSidebar = false;

  // Flag to check if running in browser (not server-side rendering)
  private isBrowser: boolean;

  constructor(
    private authService: AuthService,  // Service for managing authentication state
    private router: Router,            // Angular's router for navigation
    @Inject(PLATFORM_ID) private platformId: Object  // SSR: identify platform (browser/server)
  ) {
    // Check if code is running in browser environment
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  /**
   * Initialize component after creation
   * This runs once when the component is created
   */
  ngOnInit() {
    if (this.isBrowser) {
      // Initialize authentication state on app startup
      // This checks for existing JWT token in localStorage
      this.authService.initializeAuth();

      // Check initial route when app starts
      this.updateSidebarVisibility();

      // Listen for route changes to update sidebar visibility
      // Only run in browser (not during SSR)
      this.router.events
        .pipe(filter(event => event instanceof NavigationEnd))  // Only process NavigationEnd events
        .subscribe(() => {
          this.updateSidebarVisibility();  // Update sidebar based on new route
        });
    }
  }

  /**
   * Update sidebar visibility based on current route
   * Sidebar is hidden on login/register pages, shown on authenticated routes
   */
  private updateSidebarVisibility() {
    const currentRoute = this.router.url;
    // Show sidebar only for authenticated routes (not login, register, etc.)
    this.showSidebar = currentRoute !== '/login' && currentRoute !== '/register';
  }
}
