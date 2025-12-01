import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-module-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './module-dialog.component.html',
  styleUrls: ['./module-dialog.component.css']
})
export class ModuleDialogComponent {
  moduleForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<ModuleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.moduleForm = this.fb.group({
      name: ['', Validators.required],
      description: ['']
    });
  }

  onNoClick(): void {
    this.dialogRef.close();
  }
}