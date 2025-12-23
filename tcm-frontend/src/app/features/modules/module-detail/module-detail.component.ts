import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TestSuiteDialogComponent } from './test-suite-dialog.component';
import { TestCaseDialogComponent } from './test-case-dialog.component';
import { TestCaseDialogImprovedComponent } from './test-case-dialog-improved.component'; // New improved dialog
import { Project, TestModule, TestSuite, TestCase } from '../../../core/models/project.model';
import { Observable, of, BehaviorSubject, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-module-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatToolbarModule,
    MatTableModule,
    MatTooltipModule,
    MatDialogModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './module-detail.component.html',
  styleUrls: ['./module-detail.component.css']
})
export class ModuleDetailComponent implements OnInit {  private route = inject(ActivatedRoute);
  private tcmService = inject(TcmService);
  public authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  displayedColumns: string[] = ['id', 'title', 'status', 'actions'];

  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private moduleSubject = new BehaviorSubject<TestModule | null>(null);

  vm$: Observable<{ loading: boolean; error: boolean; module: TestModule | null }>;

  constructor() {
    this.vm$ = combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      module: this.moduleSubject.asObservable()
    }).pipe(
      map(({ loading, error, module }) => ({ loading, error, module }))
    );
  }

  ngOnInit(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    if (moduleId) {
      this.tcmService.getModule(moduleId).pipe(
        catchError(error => {
          console.error('Error loading module:', error);
          this.errorSubject.next(true);
          this.loadingSubject.next(false); // Stop loading on error
          return of(null);
        })
      ).subscribe(module => {
        if (module) {
          this.moduleSubject.next(module);
          this.loadingSubject.next(false); // Stop loading only after data is emitted
        }
      });
    } else {
      this.loadingSubject.next(false);
      this.errorSubject.next(true);
    }
  }

  createTestSuite(moduleId: string | number): void {
    const idAsString = String(moduleId);
    const dialogRef = this.dialog.open(TestSuiteDialogComponent, {
      width: '400px',
      data: { moduleId: idAsString }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        
        // Wait for authentication to be synchronized before making the API call
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.createTestSuite(idAsString, result).subscribe({
            next: (createdSuite) => {
              // Instead of calling ngOnInit(), refresh the module data directly
              this.refreshModuleData();
            },
            error: (error) => {
              this.loadingSubject.next(false); // Hide loading indicator
              
              // Check if this is a CSRF token issue
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to create test suite. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              }
            }
          });
        } catch (syncError) {
          console.error('Authentication sync error:', syncError);
          this.loadingSubject.next(false);
          this.snackBar.open('Authentication synchronization failed. Please refresh and try again.', 'CLOSE', {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'center',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  createTestCase(suiteId: string | number): void {
    const idAsString = String(suiteId);
    const dialogRef = this.dialog.open(TestCaseDialogImprovedComponent, {
      width: '900px', // Wider for better layout
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: { suiteId: idAsString }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        
        // Wait for authentication to be synchronized before making the API call
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.createTestCase(idAsString, result).subscribe({
            next: (createdTestCase) => {
              // Instead of calling ngOnInit(), refresh the module data directly
              this.refreshModuleData();
            },
            error: (error) => {
              this.loadingSubject.next(false); // Hide loading indicator
              
              // Check if this is a CSRF token issue
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to create test case. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              }
            }
          });
        } catch (syncError) {
          console.error('Authentication sync error:', syncError);
          this.loadingSubject.next(false);
          this.snackBar.open('Authentication synchronization failed. Please refresh and try again.', 'CLOSE', {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'center',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  editTestCase(testCase: TestCase): void {
    const dialogRef = this.dialog.open(TestCaseDialogImprovedComponent, {
      width: '900px', // Wider for better layout
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: testCase // Pass the entire test case object for editing
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        
        // Wait for authentication to be synchronized before making the API call
        try {
          await this.tcmService.waitForAuthSync();
          // Use the ID from the original test case if editing, or from the result
          const testCaseId = testCase.id as string;
          this.tcmService.updateTestCase(testCaseId, result).subscribe({
            next: (updatedTestCase) => {
              // Refresh the module data to show changes
              this.refreshModuleData();
            },
            error: (error) => {
              this.loadingSubject.next(false); // Hide loading indicator
              
              // Check if this is a CSRF token issue
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to update test case. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'center',
                  verticalPosition: 'top'
                });
              }
            }
          });
        } catch (syncError) {
          console.error('Authentication sync error:', syncError);
          this.loadingSubject.next(false);
          this.snackBar.open('Authentication synchronization failed. Please refresh and try again.', 'CLOSE', {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'center',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  private refreshModuleData(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (moduleId) {
      this.tcmService.getModule(moduleId).pipe(
        catchError(error => {
          console.error('Error refreshing module:', error);
          this.errorSubject.next(true);
          return of(null);
        }),
        finalize(() => {
          this.loadingSubject.next(false);
        })
      ).subscribe(module => {
        if (module) {
          this.moduleSubject.next(module);
          this.errorSubject.next(false); // Clear error state when module loads successfully
        }
      });
    }
  }


  getStatusClass(status: string | undefined): string {
    if (!status) return 'not_executed';
    return status.toLowerCase();
  }
}
