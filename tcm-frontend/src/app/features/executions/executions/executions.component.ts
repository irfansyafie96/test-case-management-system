import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { TcmService } from '../../../core/services/tcm.service';
import { TestExecution } from '../../../core/models/project.model';

interface ExecutionView {
  loading: boolean;
  error: boolean;
  executions: TestExecution[];
  groupedExecutions: ProjectGroup[];
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
    MatExpansionModule
  ],
  templateUrl: './executions.component.html',
  styleUrls: ['./executions.component.css']
})
export class ExecutionsComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private executionsSubject = new BehaviorSubject<TestExecution[]>([]);

  vm$ = this.createViewModel();

  displayedColumns: string[] = ['testCaseId', 'testCaseTitle', 'status', 'actions'];

  constructor(private tcmService: TcmService) {}

  ngOnInit(): void {
    this.loadMyAssignedExecutions();
  }

  private createViewModel() {
    return combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      executions: this.executionsSubject.asObservable(),
    }).pipe(
      map(({ loading, error, executions }) => ({
        loading,
        error,
        executions,
        groupedExecutions: this.groupExecutionsByHierarchy(executions)
      }))
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

  groupExecutionsByHierarchy(executions: TestExecution[]): ProjectGroup[] {
    const projectMap = new Map<string, ProjectGroup>();

    executions.forEach(execution => {
      const testCase = execution.testCase;
      if (!testCase?.testSuite) return;

      const suite = testCase.testSuite;
      const module = suite.testModule;
      const project = module?.project;

      if (!project) return;

      const projectKey = project.id.toString();
      const moduleKey = `${projectKey}-${module.id}`;
      const suiteKey = `${moduleKey}-${suite.id}`;

      // Get or create project group
      if (!projectMap.has(projectKey)) {
        projectMap.set(projectKey, {
          projectName: project.name,
          projectId: projectKey,
          modules: []
        });
      }

      const projectGroup = projectMap.get(projectKey)!;

      // Get or create module group
      let moduleGroup = projectGroup.modules.find(m => m.moduleId === moduleKey);
      if (!moduleGroup) {
        moduleGroup = {
          moduleName: module.name,
          moduleId: moduleKey,
          suites: []
        };
        projectGroup.modules.push(moduleGroup);
      }

      // Get or create suite group
      let suiteGroup = moduleGroup.suites.find(s => s.suiteId === suiteKey);
      if (!suiteGroup) {
        suiteGroup = {
          suiteName: suite.name,
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
