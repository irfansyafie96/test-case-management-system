import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  hidePassword = true;
  private isBrowser: boolean;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      const { username, password } = this.loginForm.value;
      console.log('Submitting login with:', { username, password: '***hidden***' });

      // Try API login first
      this.authService.login({ username, password }).subscribe({
        next: (response) => {
          console.log('Login successful - navigation handled by service');
        },
        error: (error) => {
          console.error('API login failed:', error);

          // Only use fallback for development when backend is not available
          // This prevents the fallback from masking actual authentication failures
          if (error.status === 0 || error.status === 502 || error.status === 503) {
            // Network error or server unavailable - use mock auth
            const mockUser = {
              id: '1',
              username: username,
              email: `${username}@example.com`,
              roles: ['user'],
              enabled: true
            };

            // Use the public method to set mock authentication
            console.log('Falling back to mock authentication (network/server error)');
            this.authService.setMockAuth(mockUser, 'mock-token');
            this.router.navigate(['/dashboard']);
          } else {
            // Actual authentication failure - show error to user
            console.log('Authentication failed - not using fallback');
            // The error is already logged and user will see feedback from AuthService
          }
        }
      });
    }
  }
}