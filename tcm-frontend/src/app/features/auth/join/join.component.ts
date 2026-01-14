import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TeamService } from '../../../core/services/team.service';
import { timeout, catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

@Component({
  selector: 'app-join',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './join.component.html',
  styleUrls: ['./join.component.css']
})
export class JoinComponent implements OnInit {
  joinForm: FormGroup;
  token = '';
  invitation: any = null;
  isLoading = true;
  isSubmitting = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private teamService: TeamService,
    private cdRef: ChangeDetectorRef
  ) {
    this.joinForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.errorMessage = 'Invalid invitation link.';
      this.isLoading = false;
      this.cdRef.detectChanges();
      return;
    }

    this.loadInvitation();
  }

  loadInvitation() {
    console.log('Loading invitation for token:', this.token);
    // Clear any previous error message
    this.errorMessage = '';
    this.cdRef.detectChanges();
    
    this.teamService.getInvitation(this.token).pipe(
      timeout(10000), // 10 second timeout
      catchError(error => {
        console.error('Error in invitation request:', error);
        // Convert timeout error to a more user-friendly message
        if (error.name === 'TimeoutError') {
          return throwError(() => ({ status: 0, error: 'Request timed out. Please try again.' }));
        }
        return throwError(() => error);
      })
    ).subscribe({
      next: (invite) => {
        console.log('Invitation loaded:', invite);
        console.log('Setting invitation and isLoading to false');
        this.invitation = invite;
        this.isLoading = false;
        this.errorMessage = ''; // Clear any error message on success
        this.cdRef.detectChanges(); // Force change detection
        console.log('State updated - invitation:', this.invitation, 'isLoading:', this.isLoading, 'errorMessage:', this.errorMessage);
      },
      error: (err) => {
        console.error('Error loading invitation:', err);
        // Handle different error types
        if (err.status === 0) {
          this.errorMessage = err.error || 'Cannot connect to server. Please check your connection.';
        } else if (err.status === 400) {
          this.errorMessage = err.error || 'Invitation invalid or expired.';
        } else if (err.status === 404) {
          this.errorMessage = 'Invitation not found.';
        } else {
          this.errorMessage = err.error || 'Failed to load invitation.';
        }
        this.isLoading = false;
        this.cdRef.detectChanges(); // Force change detection
      }
    });
  }

  onJoin() {
    if (this.joinForm.invalid) return;

    this.isSubmitting = true;
    this.errorMessage = '';
    this.cdRef.detectChanges();

    const data = {
      token: this.token,
      ...this.joinForm.value
    };

    this.teamService.acceptInvitation(data).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.cdRef.detectChanges();
        this.router.navigate(['/login'], { queryParams: { joined: 'true' } });
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error || 'Failed to create account.';
        this.cdRef.detectChanges();
      }
    });
  }
}
