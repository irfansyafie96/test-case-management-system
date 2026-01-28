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
  selector: 'app-submodule-dialog',
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
  templateUrl: './submodule-dialog.component.html',
  styleUrls: ['./submodule-dialog.component.css']
})
export class SubmoduleDialogComponent {
  submoduleForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<SubmoduleDialogComponent>,
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