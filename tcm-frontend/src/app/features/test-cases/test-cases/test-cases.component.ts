import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatLabel } from '@angular/material/form-field';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { Observable, BehaviorSubject, combineLatest, of } from 'rxjs';
import { map, catchError, finalize } from 'rxjs/operators';
import { User } from '../../../core/models/project.model';

interface TestAnalytics {
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
  passRate: number;
  failRate: number;
  byProject: ProjectAnalytics[];
  byModule: ModuleAnalytics[];
}

interface ProjectAnalytics {
  projectId: number;
  projectName: string;
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
}

interface ModuleAnalytics {
  moduleId: number;
  moduleName: string;
  projectId: number;
  projectName: string;
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
}

@Component({
  selector: 'app-test-cases',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule, 
    MatIconModule, 
    MatProgressBarModule, 
    MatProgressSpinnerModule,
    RouterModule,
    MatSelectModule,
    MatFormFieldModule,
    MatLabel
  ],
  templateUrl: './test-cases.component.html',
  styleUrls: ['./test-cases.component.css']
})
export class TestCasesComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private analyticsSubject = new BehaviorSubject<TestAnalytics>({
    totalTestCases: 0,
    executedCount: 0,
    passedCount: 0,
    failedCount: 0,
    notExecutedCount: 0,
    passRate: 0,
    failRate: 0,
    byProject: [],
    byModule: []
  });

  vm$: Observable<{ loading: boolean; error: boolean; analytics: TestAnalytics }>;
  
  // User filter properties for admin
  users: User[] = [];
  selectedUserId: number | null = null;
  isAdmin: boolean = false;

  constructor(
    private tcmService: TcmService,
    private authService: AuthService
  ) {
    this.vm$ = combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      analytics: this.analyticsSubject.asObservable()
    }).pipe(
      map(({ loading, error, analytics }) => ({ 
        loading, 
        error, 
        analytics
      }))
    );
  }

  ngOnInit() {
    // Check if current user is admin
    this.isAdmin = this.authService.hasRole('ADMIN');
    
    // Load users if admin
    if (this.isAdmin) {
      this.loadUsers();
    }
    
    // Load analytics
    this.loadAnalytics();
  }

  loadUsers() {
    this.tcmService.getAllNonAdminUsers().subscribe({
      next: (users) => {
        this.users = users;
      },
      error: (error) => {
        console.error('Error loading users:', error);
      }
    });
  }

  loadAnalytics(userId?: number) {
    this.loadingSubject.next(true);
    this.tcmService.getTestAnalytics(userId).pipe(
      catchError(error => {
        console.error('Error loading analytics:', error);
        this.errorSubject.next(true);
        return of({
          totalTestCases: 0,
          executedCount: 0,
          passedCount: 0,
          failedCount: 0,
          notExecutedCount: 0,
          passRate: 0,
          failRate: 0,
          byProject: [],
          byModule: []
        });
      }),
      finalize(() => this.loadingSubject.next(false))
    ).subscribe(analytics => {
      this.analyticsSubject.next(analytics);
    });
  }

  onUserFilterChange(userId: number | null) {
    this.selectedUserId = userId;
    this.loadAnalytics(userId || undefined);
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }
}
