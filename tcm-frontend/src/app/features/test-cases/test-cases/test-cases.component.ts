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
import { PdfExportService } from '../../../core/services/pdf-export.service';
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

  // Filter properties for admin
  users: User[] = [];
  projects: any[] = [];
  allModules: any[] = [];
  selectedUserId: string = 'all';
  selectedProjectId: string = 'all';
  selectedModuleId: string = 'all';
  filteredModulesList: any[] = [];
  isAdmin: boolean = false;

  constructor(
    private tcmService: TcmService,
    private authService: AuthService,
    private pdfExportService: PdfExportService
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
    // Check if current user can view all executions (admin or project manager)
    this.isAdmin = this.authService.canViewAllExecutions();

    // Load users, projects and modules if admin or project manager
    if (this.isAdmin) {
      this.loadUsers();
      this.loadProjects();
      this.loadAllModules();
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
        // Error loading users
      }
    });
  }

  loadProjects() {
    this.tcmService.getProjects().subscribe({
      next: (projects) => {
        this.projects = projects;
      },
      error: (error) => {
        // Error loading projects
      }
    });
  }

  loadAllModules() {
    this.tcmService.getAllModulesInOrganization().subscribe({
      next: (modules) => {
        this.allModules = modules;
      },
      error: (error) => {
        // Error loading modules
      }
    });
  }

  loadAnalytics(userId?: number, projectId?: number, moduleId?: number) {
    this.loadingSubject.next(true);
    // Note: Backend expects submoduleId but we're using moduleId for module-level filtering
    this.tcmService.getTestAnalytics(userId, projectId, moduleId).pipe(
      catchError(error => {
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

  onUserFilterChange(userId: string) {
    this.selectedUserId = userId;
    const filterUserId = userId !== 'all' ? parseInt(userId, 10) : undefined;
    const filterProjectId = this.selectedProjectId !== 'all' ? parseInt(this.selectedProjectId, 10) : undefined;
    const filterModuleId = this.selectedModuleId !== 'all' ? parseInt(this.selectedModuleId, 10) : undefined;
    this.loadAnalytics(filterUserId, filterProjectId, filterModuleId);
  }

  onProjectFilterChange() {
    // Reset module filter when project changes
    this.selectedModuleId = 'all';
    
    // Filter modules based on selected project
    if (this.selectedProjectId === 'all') {
      // Show all modules when "All Projects" is selected
      this.filteredModulesList = [];
    } else {
      // Filter modules to only show modules from selected project
      const projectId = this.selectedProjectId;
      this.filteredModulesList = this.allModules.filter(
        (m: any) => m.projectId?.toString() === projectId
      );
      console.log('Filtered modules for project', projectId, ':', this.filteredModulesList);
    }
    
    // Reload analytics with project filter
    const filterUserId = this.selectedUserId !== 'all' ? parseInt(this.selectedUserId, 10) : undefined;
    const filterProjectId = this.selectedProjectId !== 'all' ? parseInt(this.selectedProjectId, 10) : undefined;
    this.loadAnalytics(filterUserId, filterProjectId, undefined);
  }

  onModuleFilterChange() {
    // Reload analytics with module filter
    const filterUserId = this.selectedUserId !== 'all' ? parseInt(this.selectedUserId, 10) : undefined;
    const filterProjectId = this.selectedProjectId !== 'all' ? parseInt(this.selectedProjectId, 10) : undefined;
    const filterModuleId = this.selectedModuleId !== 'all' ? parseInt(this.selectedModuleId, 10) : undefined;
    this.loadAnalytics(filterUserId, filterProjectId, filterModuleId);
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }

  exportToPdf() {
    const analytics = this.analyticsSubject.getValue();
    const filterContext = {
      user: this.selectedUserId !== 'all' ? this.getUserName(this.selectedUserId) : undefined,
      project: this.selectedProjectId !== 'all' ? this.getProjectName(this.selectedProjectId) : undefined,
      module: this.selectedModuleId !== 'all' ? this.getModuleName(this.selectedModuleId) : undefined
    };
    this.pdfExportService.exportAnalyticsReport(analytics, filterContext);
  }

  private getUserName(userId: string): string {
    const user = this.users.find(u => u.id.toString() === userId);
    return user ? user.username : userId;
  }

  private getProjectName(projectId: string): string {
    const project = this.projects.find(p => p.id.toString() === projectId);
    return project ? project.name : projectId;
  }

  private getModuleName(moduleId: string): string {
    const module = this.allModules.find(m => m.id.toString() === moduleId);
    return module ? module.name : moduleId;
  }
}
