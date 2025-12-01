import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-test-suite-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './test-suite-dialog.component.html',
  styleUrls: ['./test-suite-dialog.component.css']
})
export class TestSuiteDialogComponent {
  suiteForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<TestSuiteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.suiteForm = this.fb.group({
      name: ['', Validators.required]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
