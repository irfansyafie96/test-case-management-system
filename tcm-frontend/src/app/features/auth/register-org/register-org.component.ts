import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register-org',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatStepperModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './register-org.component.html',
  styleUrls: ['./register-org.component.css']
})
export class RegisterOrgComponent {
  @ViewChild('stepper') stepper!: MatStepper;
  
  emailForm: FormGroup;
  detailsForm: FormGroup;
  
  isOtpSent = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });

    this.detailsForm = this.fb.group({
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      organizationName: ['', [Validators.required, Validators.minLength(3)]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSendOtp() {
    if (this.emailForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    const email = this.emailForm.get('email')?.value;

    this.authService.requestOtp(email).subscribe({
      next: () => {
        this.isOtpSent = true;
        this.isLoading = false;
        // Wait a tick for UI to update isOtpSent binding before moving
        setTimeout(() => this.stepper.next(), 0);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Failed to send OTP. Please try again.';
      }
    });
  }

  onRegister() {
    if (this.detailsForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';

    const registrationData = {
      email: this.emailForm.get('email')?.value,
      ...this.detailsForm.value
    };

    this.authService.registerOrganization(registrationData).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Registration failed. Please try again.';
      }
    });
  }
}
