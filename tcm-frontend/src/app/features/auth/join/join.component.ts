import { Component, OnInit } from '@angular/core';
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
    private teamService: TeamService
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
      return;
    }

    this.loadInvitation();
  }

  loadInvitation() {
    this.teamService.getInvitation(this.token).subscribe({
      next: (invite) => {
        this.invitation = invite;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error || 'Invitation invalid or expired.';
        this.isLoading = false;
      }
    });
  }

  onJoin() {
    if (this.joinForm.invalid) return;

    this.isSubmitting = true;
    this.errorMessage = '';

    const data = {
      token: this.token,
      ...this.joinForm.value
    };

    this.teamService.acceptInvitation(data).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/login'], { queryParams: { joined: 'true' } });
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err.error || 'Failed to create account.';
      }
    });
  }
}
