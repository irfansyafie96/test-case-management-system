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
import { TestExecution, User, TestModule, Project } from '../../../core/models/project.model';

interface ExecutionView {
  loading: boolean;
  error: boolean;
  executions: TestExecution[];
  groupedExecutions: ProjectGroup[];
  isAdmin: boolean;
  filterUsers: User[];
  filterModules: TestModule[];
  filterProjects: Project[];
}

interface ProjectGroup {
  projectName: string;
  projectId: string;
  modules: ModuleGroup[];
}

interface ModuleGroup {
  moduleName: string;
  moduleId: string;
  submodules: SubmoduleGroup[];
}

interface SubmoduleGroup {
  submoduleName: string;
  submoduleId: string;
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
  private filterProjectsSubject = new BehaviorSubject<Project[]>([]);

  // Filter state
  selectedUser: string = 'all';
  selectedModule: string = 'all';
  selectedStatus: string = 'all';
  selectedProject: string = 'all';
  filteredModulesList: TestModule[] = [];

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
    return module.submodules.reduce((acc, s) => acc + s.executions.length, 0);
  }

  private createViewModel() {
    return combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      executions: this.executionsSubject.asObservable(),
      isAdmin: this.isAdminSubject.asObservable(),
      filterUsers: this.filterUsersSubject.asObservable(),
      filterModules: this.filterModulesSubject.asObservable(),
      filterProjects: this.filterProjectsSubject.asObservable(),
    }).pipe(
      map(({ loading, error, executions, isAdmin, filterUsers, filterModules, filterProjects }) => {
        const filteredExecutions = this.applyFilters(executions);
        return {
          loading,
          error,
          executions: filteredExecutions,
          groupedExecutions: this.groupExecutionsByHierarchy(filteredExecutions),
          isAdmin,
          filterUsers,
          filterModules,
          filterProjects,
        };
      })
    );
  }

  loadMyAssignedExecutions(userId?: number): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    const canViewAll = this.authService.canViewAllExecutions();

    if (canViewAll) {
      // Admin/PM: Load executions in organization/assigned projects with filters
      const filterUserId = this.selectedUser !== 'all' ? parseInt(this.selectedUser, 10) : undefined;
      const filterProjectId = this.selectedProject !== 'all' ? parseInt(this.selectedProject, 10) : undefined;
      
      this.tcmService.getAllExecutionsInOrganization(filterUserId, filterProjectId, undefined).subscribe({
        next: (executions) => {
          this.executionsSubject.next(executions);
          this.loadingSubject.next(false);
        },
        error: (error) => {
          this.errorSubject.next(true);
          this.loadingSubject.next(false);
        }
      });
    } else {
      // Non-admin/PM: Load only assigned executions
      this.tcmService.getMyAssignedExecutions().subscribe({
        next: (executions) => {
          this.executionsSubject.next(executions);
          this.loadingSubject.next(false);
        },
        error: (error) => {
          this.errorSubject.next(true);
          this.loadingSubject.next(false);
        }
      });
    }
  }

  loadAdminFilters(): void {
    const canViewAll = this.authService.canViewAllExecutions();
    this.isAdminSubject.next(canViewAll);

    if (canViewAll) {
      // Load users for filtering
      this.tcmService.getUsersInOrganization().subscribe({
        next: (users) => {
          this.filterUsersSubject.next(users);
        },
        error: (error) => {
          // Error loading users
        }
      });

      // Load projects for filtering
      this.tcmService.getProjects().subscribe({
        next: (projects) => {
          this.filterProjectsSubject.next(projects);
        },
        error: (error) => {
          // Error loading projects
        }
      });

      // Load modules for filtering
      this.tcmService.getAllModulesInOrganization().subscribe({
        next: (modules) => {
          this.filterModulesSubject.next(modules);
        },
        error: (error) => {
          // Error loading modules
        }
      });
    }
  }

  applyFilters(executions: TestExecution[]): TestExecution[] {
    let filtered = executions;

    // Filter by module (frontend filtering)
    if (this.selectedModule !== 'all') {
      filtered = filtered.filter(e => e.moduleId?.toString() === this.selectedModule);
    }

    // Filter by status (frontend filtering)
    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(e => e.overallResult === this.selectedStatus);
    }

    return filtered;
  }

  onProjectChange(): void {
    // Reset module filter when project changes
    this.selectedModule = 'all';
    
    // Filter modules based on selected project
    if (this.selectedProject === 'all') {
      // Show all modules when "All Projects" is selected
      this.filteredModulesList = [];
    } else {
      // Filter modules to only show modules from selected project
      const projectId = this.selectedProject;
      // Get all modules from the view model and filter by projectId
      const allModules = this.filterModulesSubject.getValue();
      this.filteredModulesList = allModules.filter(
        (m: TestModule) => m.projectId?.toString() === projectId
      );
      console.log('Filtered modules for project', projectId, ':', this.filteredModulesList);
    }
    
    // Reload executions with project filter
    this.loadMyAssignedExecutions();
  }

  onFilterChange(): void {
    // Reload executions with filters
    this.loadMyAssignedExecutions();
  }

  groupExecutionsByHierarchy(executions: TestExecution[]): ProjectGroup[] {
    const projectMap = new Map<string, ProjectGroup>();

    executions.forEach(execution => {
      // Try to get data from flattened fields first (new backend logic), then fallback to traversal
      let projectId = execution.projectId?.toString();
      let projectName = execution.projectName;
      let moduleId = execution.moduleId?.toString();
      let moduleName = execution.moduleName;
      let submoduleId = (execution.submoduleId)?.toString();
      let submoduleName = execution.submoduleName;

      // Fallback: Traverse object graph if flat fields are missing
      if (!projectId) {
        const testCase = execution.testCase;
        if (!testCase?.submodule) return;

        const submodule = testCase.submodule;
        const module = submodule.testModule;
        const project = module?.project;

        if (!project) return;

        projectId = project.id.toString();
        projectName = project.name;
        moduleId = module?.id.toString();
        moduleName = module?.name;
        submoduleId = submodule.id.toString();
        submoduleName = submodule.name;
      }

      if (!projectId || !moduleId || !submoduleId) return;

      const projectKey = projectId;
      const moduleKey = `${projectKey}-${moduleId}`;
      const submoduleKey = `${moduleKey}-${submoduleId}`;

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
          submodules: []
        };
        projectGroup.modules.push(moduleGroup);
      }

      // Get or create submodule group
      let submoduleGroup = moduleGroup.submodules.find(s => s.submoduleId === submoduleKey);
      if (!submoduleGroup) {
        submoduleGroup = {
          submoduleName: submoduleName || 'Unknown Submodule',
          submoduleId: submoduleKey,
          executions: []
        };
        moduleGroup.submodules.push(submoduleGroup);
      }

      submoduleGroup.executions.push(execution);
    });

    return Array.from(projectMap.values());
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'pending';
    return status.toLowerCase();
  }
}
