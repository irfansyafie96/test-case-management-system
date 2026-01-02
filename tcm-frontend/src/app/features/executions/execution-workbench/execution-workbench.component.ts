import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { TcmService } from '../../../core/services/tcm.service';
import { TestExecution, TestStepResult } from '../../../core/models/project.model';

interface ExecutionWorkbenchView {
  loading: boolean;
  error: boolean;
  execution: TestExecution | null;
}

@Component({
  selector: 'app-execution-workbench',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    MatTooltipModule,
    RouterModule
  ],
  templateUrl: './execution-workbench.component.html',
  styleUrls: ['./execution-workbench.component.css']
})
export class ExecutionWorkbenchComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private executionSubject = new BehaviorSubject<TestExecution | null>(null);
  
  vm$ = this.createViewModel();

  displayedColumns: string[] = ['stepNumber', 'action', 'expectedResult', 'status', 'actualResult', 'actions'];
  executionId: string | null = null;
  executionNotes: string = '';
  overallResult: string = '';

  constructor(
    private tcmService: TcmService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get isExecutionCompleted(): boolean {
    const execution = this.executionSubject.value;
    return execution && (execution.overallResult === 'PASSED' || execution.overallResult === 'FAILED' || execution.overallResult === 'BLOCKED');
  }

  ngOnInit(): void {
    this.executionId = this.route.snapshot.paramMap.get('id');
    if (this.executionId) {
      this.loadExecution();
    }
  }

  private createViewModel() {
    return combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      execution: this.executionSubject.asObservable(),
    }).pipe(
      map(({ loading, error, execution }) => ({ loading, error, execution }))
    );
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'skipped';
    return status.toLowerCase();
  }

  loadExecution(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    this.tcmService.getExecution(this.executionId!).subscribe({
      next: (execution) => {
        this.executionSubject.next(execution);
        // Initialize form fields from loaded execution
        this.executionNotes = execution.notes || '';
        this.overallResult = execution.overallResult || '';
        this.loadingSubject.next(false);
      },
      error: (error) => {
        console.error('Error loading execution:', error);
        this.errorSubject.next(true);
        this.loadingSubject.next(false);
      }
    });
  }

  updateStepResult(stepResult: TestStepResult, status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'SKIPPED', actualResult?: string): void {
    if (!this.executionId) return;

    // Update the step result status and actual result
    stepResult.status = status;
    if (actualResult !== undefined) {
      stepResult.actualResult = actualResult;
    }

    // Call the API to update the step result
    if (stepResult.id) {
      this.tcmService.updateStepResult(this.executionId, stepResult.id, status, actualResult || '').subscribe({
        next: (updatedResult) => {

        },
        error: (error) => {
          console.error('Error updating step result:', error);
          // Revert the change if the API call fails
          stepResult.status = stepResult.status; // This doesn't actually revert, but shows the intent
        }
      });
    }
  }

  onNotesChange(notes: string): void {
    this.executionNotes = notes;
  }

  onResultChange(result: string): void {
    this.overallResult = result;
  }

  onFileSelected(event: any, stepResult: TestStepResult): void {
    const files: FileList | null = event.target.files;
    if (files && files.length > 0) {
      // In a real implementation, we would upload the files to the server
      // For now, we'll just track them in an array
      if (!stepResult.attachments) {
        stepResult.attachments = [];
      }

      for (let i = 0; i < files.length; i++) {
        stepResult.attachments.push(files[i].name);
      }
    }
  }

  completeExecution(overallResult: string, notes?: string): void {
    if (!this.executionId) return;

    // In a real implementation, we would call the API to complete the execution


    // For now, just navigate back to the assignments page
    this.tcmService.completeExecution(this.executionId, overallResult, notes || '').subscribe({
      next: () => {
        this.router.navigate(['/executions']);
      },
      error: (error) => {
        console.error('Error completing execution:', error);
      }
    });
  }

  completeAndNextExecution(overallResult: string, notes?: string): void {
    if (!this.executionId) return;

    // Complete the current execution first
    this.tcmService.completeExecution(this.executionId, overallResult, notes || '').subscribe({
      next: () => {
        // Get all assigned executions to find the next one
        this.tcmService.getMyAssignedExecutions().subscribe({
          next: (executions) => {
            // Sort executions by ID (convert to numbers for proper sorting)
            const sortedExecutions = executions.sort((a, b) => Number(a.id || 0) - Number(b.id || 0));

            // Find the current execution index
            const currentExecutionId = Number(this.executionId);
            const currentIndex = sortedExecutions.findIndex(e => Number(e.id) === currentExecutionId);

            // If there's a next execution, navigate to it
            if (currentIndex !== -1 && currentIndex < sortedExecutions.length - 1) {
              const nextExecution = sortedExecutions[currentIndex + 1];
              this.router.navigate(['/executions/workbench', nextExecution.id]);
            } else {
              // No more executions, go back to assignments page
              this.router.navigate(['/executions']);
            }
          },
          error: (error) => {
            console.error('Error loading assigned executions:', error);
            this.router.navigate(['/executions']);
          }
        });
      },
      error: (error) => {
        console.error('Error completing execution:', error);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/executions']);
  }
}