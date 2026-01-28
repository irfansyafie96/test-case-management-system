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
  selector: 'app-test-submodule-dialog',
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
  templateUrl: './test-submodule-dialog.component.html',
  styleUrls: ['./test-submodule-dialog.component.css']
})
export class TestSubmoduleDialogComponent {
  submoduleForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<TestSubmoduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.submoduleForm = this.fb.group({
      name: ['', Validators.required]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}