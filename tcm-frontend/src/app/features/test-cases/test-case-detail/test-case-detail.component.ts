import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TestCase } from '../../../core/models/project.model';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';
import { TestCaseDialogImprovedComponent } from '../../modules/module-detail/test-case-dialog-improved.component';

@Component({
  selector: 'app-test-case-detail',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule, 
    MatIconModule, 
    MatChipsModule, 
    MatDividerModule,
    RouterModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './test-case-detail.component.html',
  styleUrls: ['./test-case-detail.component.css']
})
export class TestCaseDetailComponent implements OnInit, OnDestroy {
  testCaseId: string | null = null;
  testCase: TestCase | null = null;
  isLoading = true;
  error: string | null = null;
  private routeSub: Subscription | null = null;
  
  // Navigation properties
  allTestCases: TestCase[] = [];

  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  constructor(
    private route: ActivatedRoute,
    private tcmService: TcmService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Force change detection after a short delay to handle SSR hydration
    setTimeout(() => {
      this.cdr.detectChanges();
    }, 100);
    
    // Subscribe to route parameter changes instead of using snapshot
    this.routeSub = this.route.paramMap.subscribe(params => {
      this.testCaseId = params.get('id');
      
      if (this.testCaseId) {
        this.loadTestCase(this.testCaseId);
        this.loadAllTestCases();
      }
    });
  }

  ngOnDestroy() {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  loadTestCase(id: string) {
    this.isLoading = true;
    this.error = null;
    this.cdr.detectChanges(); // Force UI update immediately
    
    this.tcmService.getTestCase(id).subscribe({
      next: (data) => {
        this.testCase = data;
        this.isLoading = false;
        this.cdr.detectChanges(); // Force UI update
      },
      error: (err) => {
        this.error = 'Failed to load test case details.';
        this.isLoading = false;
        this.cdr.detectChanges(); // Force UI update
      }
    });
  }

  loadAllTestCases() {
    this.tcmService.getAllTestCases().subscribe({
      next: (testCases) => {
        this.allTestCases = testCases;
      },
      error: (err) => {
        // Error loading test cases
      }
    });
  }

  get isPreviousDisabled(): boolean {
    if (!this.testCaseId || this.allTestCases.length === 0) return true;
    const currentIndex = this.allTestCases.findIndex(tc => tc.id?.toString() === this.testCaseId);
    return currentIndex <= 0;
  }

  get isNextDisabled(): boolean {
    if (!this.testCaseId || this.allTestCases.length === 0) return true;
    const currentIndex = this.allTestCases.findIndex(tc => tc.id?.toString() === this.testCaseId);
    return currentIndex >= this.allTestCases.length - 1 || currentIndex === -1;
  }

  navigateToPrevious(): void {
    if (this.isPreviousDisabled) return;
    const currentIndex = this.allTestCases.findIndex(tc => tc.id?.toString() === this.testCaseId);
    if (currentIndex > 0) {
      const previousTestCase = this.allTestCases[currentIndex - 1];
      this.router.navigate(['/test-cases', previousTestCase.id]);
    }
  }

  navigateToNext(): void {
    if (this.isNextDisabled) return;
    const currentIndex = this.allTestCases.findIndex(tc => tc.id?.toString() === this.testCaseId);
    if (currentIndex < this.allTestCases.length - 1) {
      const nextTestCase = this.allTestCases[currentIndex + 1];
      this.router.navigate(['/test-cases', nextTestCase.id]);
    }
  }

  editTestCase(): void {
    if (!this.testCase) return;

    const testCaseId = this.testCase.id as string;
    
    // Open the edit dialog with the current test case data
    const dialogRef = this.dialog.open(TestCaseDialogImprovedComponent, {
      width: '900px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: this.testCase // Pass the test case with steps for editing
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.updateTestCase(testCaseId, result).subscribe({
            next: (updatedTestCase) => {
              // Reload the test case to show updated data
              this.loadTestCase(testCaseId);
              this.snackBar.open('Test case updated successfully', 'Close', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error: (error) => {
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to update test case. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              }
            }
          });
        } catch (syncError) {
          this.snackBar.open('Authentication synchronization failed. Please refresh and try again.', 'CLOSE', {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  navigateToExecutions(): void {
    this.router.navigate(['/executions']);
  }
}