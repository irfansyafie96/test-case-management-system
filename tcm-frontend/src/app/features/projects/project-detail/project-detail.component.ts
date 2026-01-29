import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { ModuleDialogComponent } from '../../modules/modules/module-dialog.component';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { Project, TestModule, User, ProjectAssignmentRequest } from '../../../core/models/project.model';
import { Observable, BehaviorSubject, forkJoin } from 'rxjs';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, RouterModule, MatDialogModule, MatTooltipModule, MatProgressSpinnerModule, MatSelectModule, MatOptionModule, MatFormFieldModule, MatInputModule, FormsModule, MatSnackBarModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css']
})
export class ProjectDetailComponent implements OnInit {  
  project$!: Observable<Project>;
  loading$ = new BehaviorSubject<boolean>(false);
  creatingModule$ = new BehaviorSubject<boolean>(false);
  isDeletingModule$ = new BehaviorSubject<boolean>(false);
  deletingModuleId: string | number | null = null;

  // Assignment management
  showAssignments = false;
  assignedUsers: User[] = [];
  availableUsers: User[] = [];
  selectedUserId: number | string | null = null;
  loadingAssignments = false;

  constructor(
    private route: ActivatedRoute,
    private tcmService: TcmService,
    public authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId) {
      this.project$ = this.tcmService.getProject(projectId);
    }
  }

  openModuleDialog(projectId: string | number): void {
    const idAsString = String(projectId);
    
    let dialogRef;
    try {
      dialogRef = this.dialog.open(ModuleDialogComponent, {
        width: '400px',
        data: { projectId: idAsString }
      });
    } catch (dialogError) {
      this.snackBar.open('Failed to open module dialog. Please refresh the page.', 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar'],
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }
    
    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        this.creatingModule$.next(true);
        this.loading$.next(true);

        try {
          // Wait for authentication to be synchronized before making the API call
          await this.tcmService.waitForAuthSync();
          
          this.tcmService.createModule(idAsString, result).subscribe({
            next: (createdModule) => {
              this.project$ = this.tcmService.getProject(idAsString);
              this.creatingModule$.next(false);
              this.loading$.next(false);
              
              this.snackBar.open('Module created successfully!', 'Close', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error: (error) => {
              this.creatingModule$.next(false);
              this.loading$.next(false);
              
              // Check if this is a CSRF token issue
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'Close', {
                  duration: 5000,
                  panelClass: ['warning-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to create module. Please try again.', 'Close', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              }
            }
          });
        } catch (syncError) {
          this.creatingModule$.next(false);
          this.loading$.next(false);
          this.snackBar.open('Authentication synchronization failed. Please refresh and try again.', 'Close', {
            duration: 5000,
            panelClass: ['error-snackbar'],
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      }
    });
  }

  deleteModule(event: Event, moduleId: string | number): void {
    const idAsString = String(moduleId);
    event.stopPropagation();
    event.preventDefault();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Module',
        message: 'Are you sure you want to delete this module? This action cannot be undone.',
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.isDeletingModule$.next(true);
        this.loading$.next(true);
        this.deletingModuleId = moduleId;

        this.tcmService.deleteModule(idAsString).subscribe(
          () => {
            const currentProjectId = this.route.snapshot.paramMap.get('id');
            if (currentProjectId) {
              this.project$ = this.tcmService.getProject(currentProjectId);
            }
            this.isDeletingModule$.next(false);
            this.loading$.next(false);
            this.deletingModuleId = null;
            // Show success snackbar
            this.snackBar.open('Module deleted successfully', 'Close', {
              duration: 3000,
              panelClass: ['success-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          },
          error => {
            this.snackBar.open('Failed to delete module. Please try again.', 'Close', {
              duration: 5000,
              panelClass: ['error-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
            this.isDeletingModule$.next(false);
            this.loading$.next(false);
            this.deletingModuleId = null;
          }
        );
      }
    });
  }

  

  // ==================== ASSIGNMENT METHODS ====================

  toggleAssignments(): void {
    this.showAssignments = !this.showAssignments;
    if (this.showAssignments) {
      const projectId = this.route.snapshot.paramMap.get('id');
      if (projectId) {
        this.loadAssignmentData(projectId);
      }
    }
  }

  loadAssignmentData(projectId: string): void {
    this.loadingAssignments = true;
    this.cdr.detectChanges(); // Force update to show loading state
    
    // First load assigned users, then load available users
    this.tcmService.getUsersAssignedToProject(projectId).subscribe({
      next: (assignedUsers: User[]) => {
        // Now load QA and BA users in parallel
        forkJoin({
          qaUsers: this.tcmService.getUsersByRole('QA'),
          baUsers: this.tcmService.getUsersByRole('BA')
        }).subscribe({
          next: ({ qaUsers, baUsers }) => {
            // Defer all updates to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
            setTimeout(() => {
              // Update assigned users first
              this.assignedUsers = assignedUsers;
              
              // Combine QA and BA users, deduplicate by ID
              const userMap = new Map<string, User>();
              [...qaUsers, ...baUsers].forEach(user => {
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
            setTimeout(() => {
              this.loadingAssignments = false;
              this.cdr.detectChanges(); // Update template even on error
            }, 0);
          }
        });
      },
      error: (error: any) => {
        setTimeout(() => {
          this.loadingAssignments = false;
          this.cdr.detectChanges(); // Update template even on error
        }, 0);
      }
    });
  }

  // Keep these methods for refreshing after assignments
  loadAssignedUsers(projectId: string): void {
    this.tcmService.getUsersAssignedToProject(projectId).subscribe({
      next: (users: User[]) => {
        setTimeout(() => {
          this.assignedUsers = users;
          // Re-filter available users based on new assigned users
          this.refilterAvailableUsers();
        }, 0);
      },
      error: (error: any) => {
        // Error refreshing assigned users
      }
    });
  }

  loadAvailableUsers(): void {
    forkJoin({
      qaUsers: this.tcmService.getUsersByRole('QA'),
      baUsers: this.tcmService.getUsersByRole('BA')
    }).subscribe({
      next: ({ qaUsers, baUsers }) => {
        setTimeout(() => {
          // Combine QA and BA users, deduplicate by ID
          const userMap = new Map<string, User>();
          [...qaUsers, ...baUsers].forEach(user => {
            userMap.set(String(user.id), user);
          });
          const allUsers = Array.from(userMap.values());
          const assignedIds = this.assignedUsers.map(u => String(u.id));
          this.availableUsers = allUsers.filter(user => !assignedIds.includes(String(user.id)));
        }, 0);
      },
      error: (error: any) => {
        // Error refreshing available users
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
    const projectId = this.route.snapshot.paramMap.get('id');
    if (!projectId) return;

    const request: ProjectAssignmentRequest = {
      userId: Number(userId),
      projectId: Number(projectId)
    };

    this.tcmService.assignUserToProject(request).subscribe(
      (updatedUser: User) => {
        // Refresh all assignment data
        this.loadAssignmentData(projectId);
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
    const projectId = this.route.snapshot.paramMap.get('id');
    if (!projectId) return;

    const request: ProjectAssignmentRequest = {
      userId: Number(userId),
      projectId: Number(projectId)
    };

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Remove User',
        message: 'Are you sure you want to remove this user from the project?',
        icon: 'warning',
        confirmButtonText: 'Remove',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.removeUserFromProject(request).subscribe(
          (updatedUser: User) => {
            // Refresh all assignment data
            this.loadAssignmentData(projectId);
            // Show success snackbar
            this.snackBar.open('User removed successfully', 'Close', {
              duration: 3000,
              panelClass: ['success-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          },
          (error: any) => {
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
}
