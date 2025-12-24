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
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TestSuiteDialogComponent } from './test-suite-dialog.component';
import { TestCaseDialogComponent } from './test-case-dialog.component';
import { TestCaseDialogImprovedComponent } from './test-case-dialog-improved.component'; // New improved dialog
import { Project, TestModule, TestSuite, TestCase, ModuleAssignmentRequest, User } from '../../../core/models/project.model';
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
export class ModuleDetailComponent implements OnInit {  private route = inject(ActivatedRoute);
  private tcmService = inject(TcmService);
  public authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);

  displayedColumns: string[] = ['id', 'title', 'status', 'actions'];

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
    
    // First load assigned users, then load available TESTER users
    this.tcmService.getUsersAssignedToTestModule(moduleId).subscribe({
      next: (assignedUsers: User[]) => {
        this.assignedUsers = assignedUsers;
        
        // Now load TESTER users
        this.tcmService.getUsersByRole('TESTER').subscribe({
          next: (testerUsers: User[]) => {
            // Filter out already assigned users
            const assignedIds = this.assignedUsers.map(u => String(u.id));
            this.availableUsers = testerUsers.filter(user => !assignedIds.includes(String(user.id)));
            this.loadingAssignments = false;
            this.cdr.detectChanges();
          },
          error: (error: any) => {
            console.error('Error loading TESTER users:', error);
            this.loadingAssignments = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: (error: any) => {
        console.error('Error loading assigned users:', error);
        this.loadingAssignments = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Keep these methods for refreshing after assignments
  loadAssignedUsers(moduleId: string): void {
    this.tcmService.getUsersAssignedToTestModule(moduleId).subscribe({
      next: (users: User[]) => {
        this.assignedUsers = users;
        // Re-filter available users based on new assigned users
        this.refilterAvailableUsers();
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('Error refreshing assigned users:', error);
        this.cdr.detectChanges();
      }
    });
  }

  loadAvailableUsers(): void {
    this.tcmService.getUsersByRole('TESTER').subscribe({
      next: (testerUsers: User[]) => {
        const assignedIds = this.assignedUsers.map(u => String(u.id));
        this.availableUsers = testerUsers.filter(user => !assignedIds.includes(String(user.id)));
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('Error refreshing available users:', error);
        this.cdr.detectChanges();
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
      },
      (error: any) => {
        console.error('Error assigning user:', error);
        alert('Failed to assign user. Please try again.');
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

    if (!confirm('Are you sure you want to remove this user from the module?')) {
      return;
    }

    this.tcmService.removeUserFromTestModule(request).subscribe(
      (updatedUser: User) => {
        // Refresh all assignment data
        this.loadAssignmentData(moduleId);
      },
      (error: any) => {
        console.error('Error removing user:', error);
        alert('Failed to remove user. Please try again.');
      }
    );
  }
}
