import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  currentUser: any = null;
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
      this.currentUser = this.authService.getCurrentUser();

      // If not authenticated, redirect to login
      if (!this.authService.isAuthenticated()) {
        this.router.navigate(['/login']);
        return;
      }
    }
  }

  navigateToProjects() {
    this.router.navigate(['/projects']);
  }

  navigateToModules() {
    this.router.navigate(['/modules']);
  }

  navigateToTestCases() {
    this.router.navigate(['/test-cases']);
  }

  navigateToExecutions() {
    this.router.navigate(['/executions']);
  }
}
