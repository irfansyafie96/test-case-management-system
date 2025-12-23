import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  isAuthenticated$: Observable<boolean>;
  currentUser$: Observable<any>;
  private isBrowser: boolean;

  constructor(
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
    this.currentUser$ = this.authService.currentUser$;
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
  }

  logout(): void {
    if (this.isBrowser) {
      this.authService.logout();
    }
  }

  refreshData(): void {
    if (this.isBrowser) {
      window.location.reload();
    }
  }

  getPrimaryRole(roles: string[] | undefined): string {
    if (!roles || roles.length === 0) {
      return 'USER';
    }
    // Return the highest privilege role (prioritizing ADMIN, then QA/BA, then TESTER)
    if (roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('QA')) return 'QA';
    if (roles.includes('BA')) return 'BA';
    if (roles.includes('TESTER')) return 'TESTER';
    return roles[0];
  }
}
