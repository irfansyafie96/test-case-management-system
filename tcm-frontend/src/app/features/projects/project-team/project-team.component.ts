import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { TeamService } from '../../../core/services/team.service';
import { User } from '../../../core/models/project.model';
import { Observable, BehaviorSubject } from 'rxjs';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { AssignModulesDialogComponent } from './assign-modules-dialog/assign-modules-dialog.component';

@Component({
  selector: 'app-project-team',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatMenuModule,
    MatDialogModule,
    MatTooltipModule,
    ReactiveFormsModule,
    RouterModule,
    AssignModulesDialogComponent
  ],
  templateUrl: './project-team.component.html',
  styleUrls: ['./project-team.component.css']
})
export class ProjectTeamComponent implements OnInit {
  projectId: string | null = null;
  projectName: string = '';
  teamMembers$: Observable<User[]> | null = null;
  loading$ = new BehaviorSubject<boolean>(true);
  isAdmin = false;
  currentUser: User | null = null;

  inviteForm: FormGroup;
  isInviting = false;

  constructor(
    private route: ActivatedRoute,
    private tcmService: TcmService,
    public authService: AuthService,
    private teamService: TeamService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['QA', Validators.required],
      external: [true]
    });
  }

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id');
    this.isAdmin = this.authService.hasRole('ADMIN');
    this.currentUser = this.authService.getCurrentUser();

    if (this.projectId) {
      this.loadProjectDetails();
      this.loadTeamMembers();
    }
  }

  getModulesAbbreviated(member: User): string {
    const modules = member.assignedTestModules || [];
    if (modules.length === 0) return '';
    if (modules.length <= 2) {
      return modules.map(m => m.name).join(', ');
    }
    return `${modules[0].name}, ${modules[1].name} +${modules.length - 2} more`;
  }

  getModulesTooltip(member: User): string {
    const modules = member.assignedTestModules || [];
    return modules.map(m => m.name).join('\n');
  }

  private loadProjectDetails(): void {
    this.tcmService.getProject(this.projectId!).subscribe(project => {
      this.projectName = project.name || 'Untitled Project';
    });
  }

  private loadTeamMembers(): void {
    if (this.projectId) {
      this.teamMembers$ = this.tcmService.getUsersAssignedToProject(this.projectId);
      this.loading$.next(false);
    }
  }

  onInviteMember(): void {
    if (this.inviteForm.invalid || !this.projectId) return;

    this.isInviting = true;
    const { email, role, external } = this.inviteForm.value;

    this.teamService.inviteMember(email, role, external, this.projectId).subscribe({
      next: () => {
        this.isInviting = false;
        this.inviteForm.reset({ role: 'QA', external: true });
        this.snackBar.open('Invitation sent successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTeamMembers();
      },
      error: (error) => {
        this.isInviting = false;
        this.snackBar.open(error.error || 'Failed to send invitation.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  changeRole(member: User, newRole: string): void {
    if (!member.id) return;

    this.tcmService.updateUserRole(member.id, newRole).subscribe({
      next: () => {
        this.snackBar.open(`Role updated to ${newRole} for ${member.username}`, 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTeamMembers();
      },
      error: (error) => {
        this.snackBar.open(error.error || 'Failed to update role.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  removeMember(member: User): void {
    if (!member.id || !this.projectId) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Remove Team Member',
        message: `Are you sure you want to remove ${member.username} from the project? They will lose all access (project and module assignments). Their testing history will be preserved.`,
        confirmButtonText: 'Remove',
        confirmButtonColor: 'warn',
        icon: 'person_remove'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.removeUserFromProject(member.id!, this.projectId!).subscribe({
          next: () => {
            this.snackBar.open(`${member.username} removed from the project`, 'Close', {
              duration: 3000,
              panelClass: ['success-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
            this.loadTeamMembers();
          },
          error: (error) => {
            this.snackBar.open(error.error || 'Failed to remove member.', 'Close', {
              duration: 5000,
              panelClass: ['error-snackbar'],
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        });
      }
    });
  }

  openAssignModulesDialog(member: User): void {
    if (!member.id || !this.projectId) return;

    const dialogRef = this.dialog.open(AssignModulesDialogComponent, {
      width: '400px',
      data: {
        userId: member.id,
        username: member.username,
        projectId: this.projectId,
        currentModuleIds: member.assignedTestModules?.map(m => m.id) || []
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTeamMembers();
      }
    });
  }
}
