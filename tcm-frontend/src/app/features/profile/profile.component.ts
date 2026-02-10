import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { TcmService } from '../../core/services/tcm.service';
import { TeamService } from '../../core/services/team.service';
import { User } from '../../core/models/project.model';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatTabsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatMenuModule,
    MatDialogModule,
    ReactiveFormsModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  currentUser: User | null = null;
  teamMembers: User[] = [];
  isAdmin = false;
  
  // Forms
  passwordForm: FormGroup;
  inviteForm: FormGroup;
  isInviting = false;

  constructor(
    private authService: AuthService,
    private tcmService: TcmService,
    private teamService: TeamService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['QA', Validators.required],
      external: [false]
    });
  }

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    this.isAdmin = this.authService.hasRole('ADMIN');
    
    if (this.currentUser) {
      this.loadTeamMembers();
    }
  }

  loadTeamMembers() {
    this.tcmService.getAllTeamMembers().subscribe(users => {
      this.teamMembers = users;
      this.cdr.detectChanges();
    });
  }

  onUpdatePassword() {
    if (this.passwordForm.invalid) return;
    // Implement password update logic
  }

  onInviteMember() {
    if (this.inviteForm.invalid) return;
    
    this.isInviting = true;
    const { email, role, external } = this.inviteForm.value;

    this.teamService.inviteMember(email, role, external).subscribe({
      next: (response) => {
        this.isInviting = false;
        this.inviteForm.reset({ role: 'QA', external: false }); // Reset form but keep default role
        this.snackBar.open('Invitation sent successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
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

  logout() {
    this.authService.logout();
  }

  downloadTemplate() {
    this.tcmService.downloadTemplate().subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'test-case-import-template.xlsx';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        this.snackBar.open('Template downloaded successfully', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      },
      error: (error) => {
        this.snackBar.open('Failed to download template. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  changeRole(member: User, newRole: string) {
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

  removeMember(member: User) {
    if (!member.id) return;

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Remove Team Member',
        message: `Are you sure you want to remove ${member.username} from the team? They will no longer be able to log in, but their testing history will be preserved.`,
        confirmButtonText: 'Remove',
        confirmButtonColor: 'warn',
        icon: 'person_remove'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.removeUserFromTeam(member.id!).subscribe({
          next: () => {
            this.snackBar.open(`${member.username} removed from the team`, 'Close', {
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
}
