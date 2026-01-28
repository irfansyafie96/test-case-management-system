import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ProjectDialogComponent } from './project-dialog.component';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { Project } from '../../../core/models/project.model';
import { Observable } from 'rxjs';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    RouterModule
  ],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit {
  projects$!: Observable<Project[]>;

  constructor(
    public dialog: MatDialog,
    private tcmService: TcmService,
    public authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.projects$ = this.tcmService.projects$;
    this.tcmService.getProjects().subscribe();
  }

  openProjectDialog(): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '400px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) {
        // Wait for auth synchronization before creating project
        await this.authService.waitForAuthSync(3000);
        
        // Ensure CSRF token is available with explicit check
        let csrfTokenAvailable = false;
        let attempts = 0;
        const maxAttempts = 5;
        
        while (!csrfTokenAvailable && attempts < maxAttempts) {
          attempts++;
          
          // Refresh CSRF token
          try {
            await this.authService.refreshCsrfToken().toPromise();
            // Wait for token to be set in cookie
            await new Promise(resolve => setTimeout(resolve, 300));
            
            // Check if token is now available
            if (this.authService.getCsrfToken()) {
              csrfTokenAvailable = true;
              break;
            }
          } catch (error) {
            // Continue trying
          }
        }
        
        // Wait for authentication to be synchronized before making the API call
        try {
          const isAuthSynced = await this.tcmService.waitForAuthSync();
          if (!isAuthSynced) {
            this.snackBar.open('Authentication synchronization failed. Please refresh the page and try again.', 'CLOSE', {
              duration: 5000,
              panelClass: ['error-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
            return;
          }
          
          this.tcmService.createProject(result).subscribe({
            next: () => {
              this.snackBar.open('Project initialized successfully.', 'ACKNOWLEDGE', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error: (error) => {
              if (error.status === 409) {
                // Handle duplicate project name error
                this.snackBar.open('Error: Project name already exists. Please choose a unique designation.', 'DISMISS', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else if (error.isCsrfTokenIssue) {
                // Handle CSRF token synchronization issue
                this.snackBar.open(error.userMessage || 'Security token synchronization issue. Please try again.', 'RETRY', {
                  duration: 8000,
                  panelClass: ['warning-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              }).onAction().subscribe(async () => {
                // Retry the operation when user clicks RETRY
                try {
                  await this.tcmService.waitForAuthSync();
                  this.tcmService.createProject(result).subscribe({
                    next: () => {
                      this.snackBar.open('Project initialized successfully.', 'ACKNOWLEDGE', {
                        duration: 3000,
                        panelClass: ['success-snackbar'],
                        horizontalPosition: 'right',
                        verticalPosition: 'top'
                      });
                    },
                    error: (retryError) => {
                      this.snackBar.open('System Failure: Unable to initialize project after retry.', 'CLOSE', {
                        duration: 5000,
                        panelClass: ['error-snackbar'],
                        horizontalPosition: 'right',
                        verticalPosition: 'top'
                      });
                    }
                  });
                } catch (syncError) {
                  this.snackBar.open('Authentication synchronization failed on retry. Please refresh and try again.', 'CLOSE', {
                    duration: 5000,
                    panelClass: ['error-snackbar'],
                    horizontalPosition: 'right',
                    verticalPosition: 'top'
                  });
                }
              });
            } else {
              let errorMessage = 'System Failure: Unable to initialize project.';

              if (error.status === 401) {
                errorMessage = 'Authentication required. Please login again.';
              } else if (error.status === 403) {
                errorMessage = 'Permission denied. You do not have rights to create projects.';
              } else if (error.status === 500) {
                errorMessage = 'Server error. Please try again later.';
              }

              this.snackBar.open(errorMessage, 'CLOSE', {
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

  deleteProject(event: Event, projectId: string | number): void {
    const idAsString = String(projectId);
    // Prevent the click from navigating to the project page
    event.stopPropagation();
    event.preventDefault();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Project',
        message: 'Are you sure you want to delete this project? This action cannot be undone and will also delete all modules, test suites, test cases and executions associated with this project.',
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(async result => {
      if (result) { // User confirmed
        // Wait for authentication to be synchronized before making the API call
        try {
          await this.tcmService.waitForAuthSync();
          this.tcmService.deleteProject(idAsString).subscribe(
            () => {
              // The project list will be refreshed automatically due to the tap() in the service
              this.snackBar.open('Project deleted successfully.', 'DISMISS', {
                duration: 3000,
                panelClass: ['success-snackbar'],
                horizontalPosition: 'right',
                verticalPosition: 'top'
              });
            },
            error => {
              // Check if this is a CSRF token issue
              if (error.isCsrfTokenIssue) {
                this.snackBar.open('Security token synchronization issue. Please try again.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              } else {
                this.snackBar.open('Failed to delete project.', 'CLOSE', {
                  duration: 5000,
                  panelClass: ['error-snackbar'],
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              }
            }
          );
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
}

