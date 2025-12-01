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
  selector: 'app-project-dialog',
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
  templateUrl: './project-dialog.component.html',
  styleUrls: ['./project-dialog.component.css']
})
export class ProjectDialogComponent {
  projectForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<ProjectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.projectForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
    });
  }

  onNoClick(): void {
    this.dialogRef.close();
  }
}