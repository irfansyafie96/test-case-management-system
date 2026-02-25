import { Component, OnInit, Inject, PLATFORM_ID, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
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
  isMobileView = false;

  constructor(
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
    this.currentUser$ = this.authService.currentUser$;
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      setTimeout(() => {
        this.checkMobileView();
        this.cdr.detectChanges();
      }, 0);
      window.addEventListener('resize', () => this.checkMobileView());
    }
  }

  checkMobileView() {
    this.isMobileView = window.innerWidth <= 1024;
    this.cdr.detectChanges();
  }

  toggleSidebar() {
    if (this.isBrowser) {
      const sidebar = document.querySelector('.sidebar-container');
      const overlay = document.querySelector('.mobile-overlay');
      if (sidebar) {
        sidebar.classList.toggle('mobile-open');
      }
      if (overlay) {
        overlay.classList.toggle('visible');
      }
    }
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


}
