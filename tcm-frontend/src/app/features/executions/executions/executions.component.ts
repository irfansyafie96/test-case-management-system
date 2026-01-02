import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { TcmService } from '../../../core/services/tcm.service';
import { TestExecution } from '../../../core/models/project.model';

interface ExecutionView {
  loading: boolean;
  error: boolean;
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
    MatProgressSpinnerModule
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
      map(({ loading, error, executions }) => ({ loading, error, executions }))
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

  getStatusClass(status: string | undefined): string {
    if (!status) return 'skipped';
    return status.toLowerCase();
  }
}
