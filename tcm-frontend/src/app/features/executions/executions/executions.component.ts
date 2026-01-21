import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { map, filter } from 'rxjs/operators';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TestExecution, User, TestModule } from '../../../core/models/project.model';

interface ExecutionView {
  loading: boolean;
  error: boolean;
  executions: TestExecution[];
  groupedExecutions: ProjectGroup[];
  isAdmin: boolean;
  filterUsers: User[];
  filterModules: TestModule[];
}

interface ProjectGroup {
  projectName: string;
  projectId: string;
  modules: ModuleGroup[];
}

interface ModuleGroup {
  moduleName: string;
  moduleId: string;
  suites: SuiteGroup[];
}

interface SuiteGroup {
  suiteName: string;
  suiteId: string;
  executions: TestExecution[];
}

@Component({
  selector: 'app-executions',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './executions.component.html',
  styleUrls: ['./executions.component.css']
})
export class ExecutionsComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private executionsSubject = new BehaviorSubject<TestExecution[]>([]);
  private isAdminSubject = new BehaviorSubject<boolean>(false);
  private filterUsersSubject = new BehaviorSubject<User[]>([]);
  private filterModulesSubject = new BehaviorSubject<TestModule[]>([]);

  // Filter state
  selectedUser: string = 'all';
  selectedModule: string = 'all';
  selectedStatus: string = 'all';

  vm$ = this.createViewModel();

  displayedColumns: string[] = ['testCaseId', 'testCaseTitle', 'status', 'actions'];

  constructor(
    private tcmService: TcmService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadMyAssignedExecutions();
    this.loadAdminFilters();
  }

  // Helper methods to calculate counts
  getProjectTestCount(project: ProjectGroup): number {
    return project.modules.reduce((acc, m) => acc + this.getModuleTestCount(m), 0);
  }

  getModuleTestCount(module: ModuleGroup): number {
    return module.suites.reduce((acc, s) => acc + s.executions.length, 0);
  }

  private createViewModel() {
    return combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      executions: this.executionsSubject.asObservable(),
      isAdmin: this.isAdminSubject.asObservable(),
      filterUsers: this.filterUsersSubject.asObservable(),
      filterModules: this.filterModulesSubject.asObservable(),
    }).pipe(
      map(({ loading, error, executions, isAdmin, filterUsers, filterModules }) => {
        const filteredExecutions = this.applyFilters(executions);
        return {
          loading,
          error,
          executions: filteredExecutions,
          groupedExecutions: this.groupExecutionsByHierarchy(filteredExecutions),
          isAdmin,
          filterUsers,
          filterModules,
        };
      })
    );
  }

  loadMyAssignedExecutions(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    this.tcmService.getMyAssignedExecutions().subscribe({
      next: (executions) => {
        this.executionsSubject.next(executions);
        this.loadingSubject.next(false);
      },
      error: (error) => {
        console.error('Error loading assigned executions:', error);
        this.errorSubject.next(true);
        this.loadingSubject.next(false);
      }
    });
  }

  loadAdminFilters(): void {
    const currentUser = this.authService.getCurrentUser();
    const isAdmin = currentUser?.roles?.includes('ADMIN') || false;
    this.isAdminSubject.next(isAdmin);

    if (isAdmin) {
      // Load users and modules for filtering
      this.tcmService.getUsersInOrganization().subscribe({
        next: (users) => {
          this.filterUsersSubject.next(users);
        },
        error: (error) => {
          console.error('Error loading users:', error);
        }
      });

      this.tcmService.getAllModulesInOrganization().subscribe({
        next: (modules) => {
          this.filterModulesSubject.next(modules);
        },
        error: (error) => {
          console.error('Error loading modules:', error);
        }
      });
    }
  }

  applyFilters(executions: TestExecution[]): TestExecution[] {
    let filtered = executions;

    // Filter by user
    if (this.selectedUser !== 'all') {
      filtered = filtered.filter(e => e.assignedToUser?.id?.toString() === this.selectedUser);
    }

    // Filter by module
    if (this.selectedModule !== 'all') {
      filtered = filtered.filter(e => e.moduleId?.toString() === this.selectedModule);
    }

    // Filter by status
    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(e => e.overallResult === this.selectedStatus);
    }

    return filtered;
  }

  onFilterChange(): void {
    // Trigger re-filtering by updating the executions subject
    this.executionsSubject.next(this.executionsSubject.value);
  }

  groupExecutionsByHierarchy(executions: TestExecution[]): ProjectGroup[] {
    const projectMap = new Map<string, ProjectGroup>();

    executions.forEach(execution => {
      // Try to get data from flattened fields first (new backend logic), then fallback to traversal
      let projectId = execution.projectId?.toString();
      let projectName = execution.projectName;
      let moduleId = execution.moduleId?.toString();
      let moduleName = execution.moduleName;
      let suiteId = (execution.testSuiteId || execution.suiteId)?.toString();
      let suiteName = execution.testSuiteName || execution.suiteName;

      // Fallback: Traverse object graph if flat fields are missing
      if (!projectId) {
        const testCase = execution.testCase;
        if (!testCase?.testSuite) return;

        const suite = testCase.testSuite;
        const module = suite.testModule;
        const project = module?.project;

        if (!project) return;

        projectId = project.id.toString();
        projectName = project.name;
        moduleId = module?.id.toString();
        moduleName = module?.name;
        suiteId = suite.id.toString();
        suiteName = suite.name;
      }

      if (!projectId || !moduleId || !suiteId) return;

      const projectKey = projectId;
      const moduleKey = `${projectKey}-${moduleId}`;
      const suiteKey = `${moduleKey}-${suiteId}`;

      // Get or create project group
      if (!projectMap.has(projectKey)) {
        projectMap.set(projectKey, {
          projectName: projectName || 'Unknown Project',
          projectId: projectKey,
          modules: []
        });
      }

      const projectGroup = projectMap.get(projectKey)!;

      // Get or create module group
      let moduleGroup = projectGroup.modules.find(m => m.moduleId === moduleKey);
      if (!moduleGroup) {
        moduleGroup = {
          moduleName: moduleName || 'Unknown Module',
          moduleId: moduleKey,
          suites: []
        };
        projectGroup.modules.push(moduleGroup);
      }

      // Get or create suite group
      let suiteGroup = moduleGroup.suites.find(s => s.suiteId === suiteKey);
      if (!suiteGroup) {
        suiteGroup = {
          suiteName: suiteName || 'Unknown Suite',
          suiteId: suiteKey,
          executions: []
        };
        moduleGroup.suites.push(suiteGroup);
      }

      suiteGroup.executions.push(execution);
    });

    return Array.from(projectMap.values());
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'pending';
    return status.toLowerCase();
  }
}
