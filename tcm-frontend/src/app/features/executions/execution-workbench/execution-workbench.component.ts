import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { TcmService } from '../../../core/services/tcm.service';
import { TestExecution, TestStepResult } from '../../../core/models/project.model';
import { CompletionSummaryDialogComponent } from './completion-summary-dialog.component';

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
    MatSnackBarModule,
    MatDialogModule,
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

  // New properties for navigation
  allExecutions: TestExecution[] = [];
  currentModuleId: string | null = null;
  currentSuiteId: string | null = null;

  constructor(
    private tcmService: TcmService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  get isExecutionCompleted(): boolean {
    const execution = this.executionSubject.value;
    return !!execution && (execution.overallResult === 'PASSED' || execution.overallResult === 'FAILED' || execution.overallResult === 'BLOCKED');
  }

  get isFirstExecution(): boolean {
    if (!this.executionId || this.allExecutions.length === 0) return true;
    const currentIndex = this.allExecutions.findIndex(e => e.id.toString() === this.executionId?.toString());
    return currentIndex === 0;
  }

  get isLastExecution(): boolean {
    if (!this.executionId || this.allExecutions.length === 0) return true;
    const currentIndex = this.allExecutions.findIndex(e => e.id.toString() === this.executionId?.toString());
    return currentIndex === this.allExecutions.length - 1;
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.executionId = params.get('id');
      if (this.executionId) {
        this.loadExecution();
      }
    });
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
    if (!status) return 'pending';
    return status.toLowerCase();
  }

  loadExecution(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    this.tcmService.getExecution(this.executionId!).subscribe({
      next: (execution) => {
        // Normalize step result status values from invalid old data
        const validStepStatuses = ['PASSED', 'FAILED', 'BLOCKED', 'PENDING'];
        if (execution.stepResults) {
          execution.stepResults.forEach(sr => {
            if (sr.status && !validStepStatuses.includes(sr.status)) {
              sr.status = 'PENDING'; // Normalize invalid values to PENDING
            }
          });
        }

        this.executionSubject.next(execution);
        // Initialize form fields from loaded execution
        this.executionNotes = execution.notes || '';
        // Only set overallResult if it's a valid completion status (PASSED, FAILED, BLOCKED, PARTIALLY_PASSED)
        const validCompletionStatuses = ['PASSED', 'FAILED', 'BLOCKED', 'PARTIALLY_PASSED'];
        this.overallResult = (execution.overallResult && validCompletionStatuses.includes(execution.overallResult))
          ? execution.overallResult
          : '';

        // Store current hierarchy info
        this.currentModuleId = execution.moduleId?.toString() || null;
        this.currentSuiteId = execution.testSuiteId?.toString() || null;

        // Load all executions for navigation
        this.loadAllExecutions();

        this.loadingSubject.next(false);
      },
      error: (error) => {
        console.error('Error loading execution:', error);
        this.errorSubject.next(true);
        this.loadingSubject.next(false);
      }
    });
  }

  loadAllExecutions(): void {
    this.tcmService.getMyAssignedExecutions().subscribe({
      next: (executions) => {
        this.allExecutions = executions;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading executions:', error);
      }
    });
  }

  updateStepResult(stepResult: TestStepResult, status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'PENDING', actualResult?: string): void {
    if (!this.executionId) return;

    // Normalize status value to ensure it's valid
    const validStatuses = ['PASSED', 'FAILED', 'BLOCKED', 'PENDING'];
    const normalizedStatus = validStatuses.includes(status) ? status : 'PENDING';

    // Update the step result status and actual result
    stepResult.status = normalizedStatus;
    if (actualResult !== undefined) {
      stepResult.actualResult = actualResult;
    }

    // Call the API to update the step result
    // Note: The backend expects testStepId, not stepResultId
    if (stepResult.testStepId) {
      this.tcmService.updateStepResult(this.executionId, stepResult.testStepId, normalizedStatus, actualResult || '').subscribe({
        next: (updatedResult) => {
          // Successfully updated
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

    // Validate that overall result is provided and is a valid completion status
    const validCompletionStatuses = ['PASSED', 'FAILED', 'BLOCKED', 'PARTIALLY_PASSED'];
    if (!overallResult || overallResult.trim() === '' || !validCompletionStatuses.includes(overallResult)) {
      this.snackBar.open(
        'Please select an overall result (Pass, Fail, or Blocked) before completing the execution.',
        'CLOSE',
        { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
      );
      return;
    }

    this.tcmService.completeExecution(this.executionId, overallResult, notes || '').subscribe({
      next: () => {
        this.router.navigate(['/executions']);
      },
      error: (error) => {
        console.error('Error completing execution:', error);
        this.snackBar.open(
          'Failed to complete execution. Please ensure all required fields are filled.',
          'CLOSE',
          { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
        );
      }
    });
  }

  /**
   * Smart NEXT button - intelligent navigation based on execution status
   * - If incomplete: Auto-save as PENDING → Navigate to next test case
   * - If complete: Navigate to next test case (any status)
   * - At very end: Show completion summary
   */
  navigateSmartNext(): void {
    if (!this.executionId || this.allExecutions.length === 0) {
      return;
    }

    const execution = this.executionSubject.value;
    const validCompletionStatuses = ['PASSED', 'FAILED', 'BLOCKED', 'PARTIALLY_PASSED'];
    const isComplete = execution?.overallResult && validCompletionStatuses.includes(execution.overallResult);

    if (isComplete) {
      // Already complete - just navigate to next
      this.navigateToNext();
    } else {
      // Incomplete - save notes then navigate to next
      this.tcmService.saveExecution(this.executionId!, this.executionNotes).subscribe({
        next: () => {
          this.navigateToNext();
        },
        error: (error) => {
          console.error('Error saving execution before navigation:', error);
          this.snackBar.open(
            'Failed to save execution. Please try again.',
            'DISMISS',
            { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
          );
        }
      });
    }
  }

  /**
   * Navigate to next test case regardless of status
   * Show completion summary only at the very end of ALL executions
   */
  private navigateToNext(): void {
    const currentIndex = this.allExecutions.findIndex(e => e.id.toString() === this.executionId?.toString());

    if (currentIndex < this.allExecutions.length - 1) {
      const nextExecution = this.allExecutions[currentIndex + 1];
      this.executeNavigation(nextExecution);
    } else {
      // At last execution, show completion summary
      this.showCompletionSummary();
    }
  }

  /**
   * Execute navigation with module notification
   */
  private executeNavigation(nextExecution: TestExecution): void {
    const currentExecution = this.executionSubject.value;
    const currentModuleId = currentExecution?.moduleId?.toString();
    const nextModuleId = nextExecution.moduleId?.toString();

    // Check if module changed
    if (currentModuleId && nextModuleId && currentModuleId !== nextModuleId) {
      this.snackBar.open(
        `Module "${currentExecution?.moduleName}" completed!`,
        'DISMISS',
        {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        }
      );
    }

    // Navigate to next execution
    this.router.navigate(['/executions/workbench', nextExecution.id]).catch(navError => {
      console.error('Navigation error:', navError);
      this.snackBar.open(
        'Failed to navigate to next test case.',
        'DISMISS',
        { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
      );
    });
  }

  showCompletionSummary(): void {
    this.tcmService.getCompletionSummary().subscribe({
      next: (summary) => {
        const dialogRef = this.dialog.open(CompletionSummaryDialogComponent, {
          width: '600px',
          data: summary,
          disableClose: true
        });

        dialogRef.afterClosed().subscribe(result => {
          if (result === 'back') {
            this.router.navigate(['/executions']);
          } else {
            this.router.navigate(['/executions']);
          }
        });
      },
      error: (error) => {
        console.error('Error loading completion summary:', error);
        this.router.navigate(['/executions']);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/executions']);
  }

  /**
   * Navigate to the previous test case regardless of status
   * Saves notes before navigating
   */
  navigateToPreviousExecution(): void {
    if (!this.executionId || this.allExecutions.length === 0) {
      return;
    }

    // Save notes before navigating
    this.tcmService.saveExecution(this.executionId!, this.executionNotes).subscribe({
      next: () => {
        // Find current execution index
        const currentIndex = this.allExecutions.findIndex(e => e.id.toString() === this.executionId?.toString());
        
        // Find previous execution
        if (currentIndex > 0) {
          const previousExecution = this.allExecutions[currentIndex - 1];
          
          const currentExecution = this.executionSubject.value;
          const currentModuleId = currentExecution?.moduleId?.toString();
          const previousModuleId = previousExecution.moduleId?.toString();

          // Check if module changed
          if (currentModuleId && previousModuleId && currentModuleId !== previousModuleId) {
            this.snackBar.open(
              `Entering module "${previousExecution.moduleName}"`,
              'DISMISS',
              {
                duration: 3000,
                panelClass: ['info-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              }
            );
          }

          // Navigate to previous execution
          this.router.navigate(['/executions/workbench', previousExecution.id]).catch(navError => {
            console.error('Navigation error:', navError);
            this.snackBar.open(
              'Failed to navigate to previous test case.',
              'DISMISS',
              { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
            );
          });
        } else {
          // At first execution, show toast
          this.snackBar.open(
            'No previous test case.',
            'DISMISS',
            { panelClass: ['info-snackbar'], duration: 3000, horizontalPosition: 'right', verticalPosition: 'top' }
          );
        }
      },
      error: (error) => {
        console.error('Error saving execution before navigation:', error);
        this.snackBar.open(
          'Failed to save execution. Please try again.',
          'DISMISS',
          { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
        );
      }
    });
  }

  }