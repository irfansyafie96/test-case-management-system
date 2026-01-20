import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
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
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TestSuiteDialogComponent } from './test-suite-dialog.component';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { TestCaseDialogImprovedComponent } from './test-case-dialog-improved.component';
import { ImportDialogComponent } from './import-dialog.component';
import { Project, TestModule, TestSuite, TestCase, ModuleAssignmentRequest, User } from '../../../core/models/project.model';
import { Observable, of, BehaviorSubject, combineLatest, forkJoin } from 'rxjs';
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
    MatSnackBarModule,
    MatSelectModule,
    MatOptionModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule
  ],
  templateUrl: './module-detail.component.html',
  styleUrls: ['./module-detail.component.css']
})
export class ModuleDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private tcmService = inject(TcmService);
  public authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  displayedColumns: string[] = ['id', 'title', 'actions'];

  // Assignment management
  showAssignments = false;
  assignedUsers: User[] = [];
  availableUsers: User[] = [];
  selectedUserId: number | string | null = null;
  loadingAssignments = false;

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
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to create test suite. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
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
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  openImportDialog(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (!moduleId) {
      this.snackBar.open('Module ID not found', 'Close', {
        duration: 3000,
        panelClass: ['error-snackbar'],
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    const dialogRef = this.dialog.open(ImportDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      maxHeight: '90vh',
      data: { moduleId: moduleId },
      panelClass: 'import-dialog-panel'
    });

    // Subscribe to the success event from the dialog component instance
    // This allows refreshing the background list immediately while the success modal is still open
    const subscription = dialogRef.componentInstance.importSuccess.subscribe(() => {
      this.refreshModuleData();
      this.snackBar.open('Test cases imported successfully', 'Close', {
        duration: 3000,
        panelClass: ['success-snackbar'],
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
    });

    dialogRef.afterClosed().subscribe(result => {
      subscription.unsubscribe(); // Clean up subscription
      // If we received a result via close (e.g. user clicked "Done"), refresh again just in case
      if (result && result.success) {
        this.refreshModuleData();
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
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to create test case. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
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
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  viewTestCase(testCase: TestCase): void {
    const testCaseId = testCase.id as string;
    this.router.navigate(['/test-cases', testCaseId]);
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
          console.error('Authentication sync error:', syncError);
          this.loadingSubject.next(false);
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

  deleteTestCase(testCase: TestCase): void {
    const testCaseId = testCase.id as string;
    
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Test Case',
        message: `Are you sure you want to delete test case "${testCase.title}"? This action cannot be undone.`,
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.deleteTestCase(testCaseId).subscribe({
            next: () => {
              this.refreshModuleData();
              this.snackBar.open('Test case deleted successfully', 'Close', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error: (error) => {
              this.loadingSubject.next(false);
              
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to delete test case. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
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
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  deleteTestSuite(suite: TestSuite): void {
    const suiteId = suite.id as string;
    
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Test Suite',
        message: `Are you sure you want to delete test suite "${suite.name}"? This will also delete all test cases within this suite. This action cannot be undone.`,
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.deleteTestSuite(suiteId).subscribe({
            next: () => {
              this.refreshModuleData();
              this.snackBar.open('Test suite deleted successfully', 'Close', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error: (error) => {
              this.loadingSubject.next(false);
              
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to delete test suite. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
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
            horizontalPosition: 'right',
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


  // ==================== ASSIGNMENT METHODS ====================

  toggleAssignments(): void {
    this.showAssignments = !this.showAssignments;
    if (this.showAssignments) {
      const moduleId = this.route.snapshot.paramMap.get('id');
      if (moduleId) {
        this.loadAssignmentData(moduleId);
      } else {
        console.error('No module ID found in route');
      }
    }
  }

  loadAssignmentData(moduleId: string): void {
    this.loadingAssignments = true;
    this.cdr.detectChanges(); // Force update to show loading state

    // First load assigned users, then load available QA, BA, and TESTER users
    this.tcmService.getUsersAssignedToTestModule(moduleId).subscribe({
      next: (assignedUsers: User[]) => {
        // Now load QA, BA, and TESTER users in parallel
        forkJoin({
          qaUsers: this.tcmService.getUsersByRole('QA'),
          baUsers: this.tcmService.getUsersByRole('BA'),
          testerUsers: this.tcmService.getUsersByRole('TESTER')
        }).subscribe({
          next: ({ qaUsers, baUsers, testerUsers }) => {
            // Defer all updates to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
            setTimeout(() => {
              // Update assigned users first
              this.assignedUsers = assignedUsers;

              // Combine QA, BA, and TESTER users, deduplicate by ID
              const userMap = new Map<string, User>();
              [...qaUsers, ...baUsers, ...testerUsers].forEach(user => {
                userMap.set(String(user.id), user);
              });
              const allUsers = Array.from(userMap.values());

              // Filter out already assigned users
              const assignedIds = assignedUsers.map(u => String(u.id));
              this.availableUsers = allUsers.filter(user => !assignedIds.includes(String(user.id)));
              this.loadingAssignments = false;
              this.cdr.detectChanges(); // Update template after changes
            }, 0);
          },
          error: (error: any) => {
            console.error('Error loading available users:', error);
            setTimeout(() => {
              this.loadingAssignments = false;
              this.cdr.detectChanges(); // Update template even on error
            }, 0);
          }
        });
      },
      error: (error: any) => {
        console.error('Error loading assigned users:', error);
        setTimeout(() => {
          this.loadingAssignments = false;
          this.cdr.detectChanges(); // Update template even on error
        }, 0);
      }
    });
  }

  // Keep these methods for refreshing after assignments
  loadAssignedUsers(moduleId: string): void {
    this.tcmService.getUsersAssignedToTestModule(moduleId).subscribe({
      next: (users: User[]) => {
        setTimeout(() => {
          this.assignedUsers = users;
          // Re-filter available users based on new assigned users
          this.refilterAvailableUsers();
          this.cdr.detectChanges(); // Update template after changes
        }, 0);
      },
      error: (error: any) => {
        console.error('Error refreshing assigned users:', error);
      }
    });
  }

  loadAvailableUsers(): void {
    forkJoin({
      qaUsers: this.tcmService.getUsersByRole('QA'),
      baUsers: this.tcmService.getUsersByRole('BA'),
      testerUsers: this.tcmService.getUsersByRole('TESTER')
    }).subscribe({
      next: ({ qaUsers, baUsers, testerUsers }) => {
        setTimeout(() => {
          // Combine QA, BA, and TESTER users, deduplicate by ID
          const userMap = new Map<string, User>();
          [...qaUsers, ...baUsers, ...testerUsers].forEach(user => {
            userMap.set(String(user.id), user);
          });
          const allUsers = Array.from(userMap.values());
          
          const assignedIds = this.assignedUsers.map(u => String(u.id));
          this.availableUsers = allUsers.filter(user => !assignedIds.includes(String(user.id)));
          this.cdr.detectChanges(); // Update template after changes
        }, 0);
      },
      error: (error: any) => {
        console.error('Error refreshing available users:', error);
      }
    });
  }

  private refilterAvailableUsers(): void {
    if (this.availableUsers.length > 0) {
      const assignedIds = this.assignedUsers.map(u => String(u.id));
      this.availableUsers = this.availableUsers.filter(user => !assignedIds.includes(String(user.id)));
    }
  }

  assignUser(userId: number | string): void {
    if (!userId) return;
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (!moduleId) return;

    const request: ModuleAssignmentRequest = {
      userId: Number(userId),
      testModuleId: Number(moduleId)
    };

    this.tcmService.assignUserToTestModule(request).subscribe(
      (updatedUser: User) => {
        // Refresh all assignment data
        this.loadAssignmentData(moduleId);
        this.selectedUserId = null;
        // Show success snackbar
        this.snackBar.open('User assigned successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      },
      (error: any) => {
        console.error('Error assigning user:', error);
        this.snackBar.open('Failed to assign user. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    );
  }

  removeUser(userId: number | string): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (!moduleId) return;

    const request: ModuleAssignmentRequest = {
      userId: Number(userId),
      testModuleId: Number(moduleId)
    };

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Remove User',
        message: 'Are you sure you want to remove this user from the module?',
        icon: 'warning',
        confirmButtonText: 'Remove',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.removeUserFromTestModule(request).subscribe(
          (updatedUser: User) => {
            // Refresh all assignment data
            this.loadAssignmentData(moduleId);
            // Show success snackbar
            this.snackBar.open('User removed successfully', 'Close', {
              duration: 3000,
              panelClass: ['success-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          },
          (error: any) => {
            console.error('Error removing user:', error);
            this.snackBar.open('Failed to remove user. Please try again.', 'Close', {
              duration: 5000,
              panelClass: ['error-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        );
      }
    });
  }

  regenerateExecutions(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (!moduleId) return;

    this.loadingSubject.next(true); // Show global loading since this is an important operation

    this.tcmService.regenerateExecutions(moduleId).subscribe({
      next: (message) => {
        this.loadingSubject.next(false);
        this.snackBar.open('Test executions regenerated successfully for all assigned users.', 'Close', {
          duration: 5000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      },
      error: (error) => {
        console.error('Error regenerating executions:', error);
        this.loadingSubject.next(false);
        this.snackBar.open('Failed to regenerate test executions. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }
}

