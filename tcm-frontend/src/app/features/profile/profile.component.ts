import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { TcmService } from '../../core/services/tcm.service';
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

  constructor(
    private authService: AuthService,
    private tcmService: TcmService,
    private fb: FormBuilder
  ) {
    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    });

    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['TESTER', Validators.required]
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
    // We can reuse the API call that fetches users for the org
    // Currently relying on getAllNonAdminUsers for Admin, 
    // or we need a new endpoint 'getTeamMembers' accessible to everyone.
    // For now, let's use what we have or placeholder.
    if (this.isAdmin) {
      this.tcmService.getAllNonAdminUsers().subscribe(users => {
        this.teamMembers = users;
      });
    } else {
        // TODO: Need an endpoint for non-admins to view team
        // For now, empty or mock
    }
  }

  onUpdatePassword() {
    if (this.passwordForm.invalid) return;
    // Implement password update logic
    console.log('Update password', this.passwordForm.value);
  }

  onInviteMember() {
    if (this.inviteForm.invalid) return;
    // Implement invite logic
    console.log('Invite member', this.inviteForm.value);
  }

  logout() {
    this.authService.logout();
  }
}
