import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { filter } from 'rxjs';
import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { HeaderComponent } from './shared/header/header.component';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarComponent,
    HeaderComponent
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  // App-level title
  title = 'Test Case Management System';
  showSidebar = false;
  private isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (this.isBrowser) {
      // Initialize authentication state on app startup
      this.authService.initializeAuth();

      // Check initial route
      this.updateSidebarVisibility();

      // Listen for route changes
      this.router.events
        .pipe(filter(event => event instanceof NavigationEnd))
        .subscribe(() => {
          this.updateSidebarVisibility();
        });
    }
  }

  private updateSidebarVisibility() {
    const currentRoute = this.router.url;
    // Show sidebar only for authenticated routes (not login, register, etc.)
    this.showSidebar = currentRoute !== '/login' && currentRoute !== '/register';
  }
}
