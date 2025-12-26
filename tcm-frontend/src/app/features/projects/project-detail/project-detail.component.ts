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
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, RouterModule, MatDialogModule, MatTooltipModule, MatProgressSpinnerModule, MatSelectModule, MatOptionModule, MatFormFieldModule, MatInputModule, FormsModule],
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
    const dialogRef = this.dialog.open(ModuleDialogComponent, {
      width: '400px',
      data: { projectId: idAsString }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.creatingModule$.next(true);
        this.loading$.next(true);

        this.tcmService.createModule(idAsString, result).subscribe(
          (createdModule) => {
            this.project$ = this.tcmService.getProject(idAsString);
            this.creatingModule$.next(false);
            this.loading$.next(false);
          },
          error => {
            console.error('Error creating test module:', error);
            this.creatingModule$.next(false);
            this.loading$.next(false);
          }
        );
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
          },
          error => {
            console.error('Error deleting module:', error);
            alert('Failed to delete module. Please try again.');
            this.isDeletingModule$.next(false);
            this.loading$.next(false);
            this.deletingModuleId = null;
          }
        );
      }
    });
  }

  getTotalTestCases(testSuites: any[] | undefined): number {
    if (!testSuites) return 0;
    return testSuites.reduce((total, suite) => {
      return total + (suite.testCases ? suite.testCases.length : 0);
    }, 0);
  }

  // ==================== ASSIGNMENT METHODS ====================

  toggleAssignments(): void {
    this.showAssignments = !this.showAssignments;
    if (this.showAssignments) {
      const projectId = this.route.snapshot.paramMap.get('id');
      if (projectId) {
        this.loadAssignmentData(projectId);
      } else {
        console.error('No project ID found in route');
      }
    }
  }

  loadAssignmentData(projectId: string): void {
    this.loadingAssignments = true;
    this.cdr.detectChanges(); // Force update to show loading state
    
    // First load assigned users, then load available users
    this.tcmService.getUsersAssignedToProject(projectId).subscribe({
      next: (assignedUsers: User[]) => {
        this.assignedUsers = assignedUsers;
        
        // Now load QA and BA users in parallel
        forkJoin({
          qaUsers: this.tcmService.getUsersByRole('QA'),
          baUsers: this.tcmService.getUsersByRole('BA')
        }).subscribe({
          next: ({ qaUsers, baUsers }) => {
            // Defer updates to next tick to avoid ExpressionChangedAfterItHasBeenCheckedError
            setTimeout(() => {
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
        console.error('Error refreshing assigned users:', error);
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
      },
      (error: any) => {
        console.error('Error assigning user:', error);
        alert('Failed to assign user. Please try again.');
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

    if (!confirm('Are you sure you want to remove this user from the project?')) {
      return;
    }

    this.tcmService.removeUserFromProject(request).subscribe(
      (updatedUser: User) => {
        // Refresh all assignment data
        this.loadAssignmentData(projectId);
      },
      (error: any) => {
        console.error('Error removing user:', error);
        alert('Failed to remove user. Please try again.');
      }
    );
  }
}
