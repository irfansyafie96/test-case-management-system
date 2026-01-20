import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { TcmService } from '../../core/services/tcm.service';
import { TeamService } from '../../core/services/team.service';
import { User } from '../../core/models/project.model';

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
    MatSnackBarModule,
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
    private cdr: ChangeDetectorRef
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['QA', Validators.required]
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
    if (this.isAdmin) {
      this.tcmService.getAllNonAdminUsers().subscribe(users => {
        this.teamMembers = users;
        this.cdr.detectChanges(); // Fix NG0100
      });
    }
  }

  onUpdatePassword() {
    if (this.passwordForm.invalid) return;
    // Implement password update logic
    console.log('Update password', this.passwordForm.value);
  }

  onInviteMember() {
    if (this.inviteForm.invalid) return;
    
    this.isInviting = true;
    const { email, role } = this.inviteForm.value;

    this.teamService.inviteMember(email, role).subscribe({
      next: (response) => {
        this.isInviting = false;
        this.inviteForm.reset({ role: 'QA' }); // Reset form but keep default role
        this.snackBar.open('Invitation sent successfully!', 'Close', {
          duration: 3000,
          panelClass: ['success-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      },
      error: (error) => {
        this.isInviting = false;
        console.error('Error inviting member:', error);
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
        console.error('Error downloading template:', error);
        this.snackBar.open('Failed to download template. Please try again.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'],
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }
}
