import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { TcmService } from '../../../core/services/tcm.service';
import { TestCycle } from '../../../core/models/project.model';

@Component({
  selector: 'app-cycle-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ data.isEdit ? 'edit' : 'add' }}</mat-icon>
      {{ data.isEdit ? 'Edit Phase' : 'Create New Phase' }}
    </h2>
    
    <mat-dialog-content>
      <form [formGroup]="cycleForm" class="form-container">
        <!-- Phase Name -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Phase Name</mat-label>
          <input matInput formControlName="name" placeholder="e.g., Phase 1 - UAT">
          <mat-error *ngIf="cycleForm.get('name')?.hasError('required')">
            Phase name is required
          </mat-error>
        </mat-form-field>

        <!-- Description -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3" placeholder="Optional description..."></textarea>
        </mat-form-field>

        <!-- Redmine Project URL -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Redmine Project URL</mat-label>
          <input matInput formControlName="redmineProjectUrl" placeholder="e.g., http://tmsredmine.tmsasia.com/projects/hrdcncspilot">
          <mat-hint>Link to Redmine project for this phase</mat-hint>
          <mat-error *ngIf="cycleForm.get('redmineProjectUrl')?.hasError('pattern')">
            Please enter a valid URL (e.g., http://example.com or https://example.com)
          </mat-error>
        </mat-form-field>

        <!-- Start Date -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Start Date</mat-label>
          <input matInput [matDatepicker]="startPicker" formControlName="startDate">
          <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
          <mat-datepicker #startPicker></mat-datepicker>
        </mat-form-field>

        <!-- End Date -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>End Date</mat-label>
          <input matInput [matDatepicker]="endPicker" formControlName="endDate">
          <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
          <mat-datepicker #endPicker></mat-datepicker>
          <mat-hint>Leave empty for ongoing phase</mat-hint>
        </mat-form-field>

        <!-- Active -->
        <mat-checkbox formControlName="isActive" class="full-width">
          Active Phase
        </mat-checkbox>
      </form>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!cycleForm.valid || saving">
        {{ data.isEdit ? 'Update' : 'Create' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 400px;
      padding: 8px 0;
    }
    
    .full-width {
      width: 100%;
    }
    
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
    }
    
    mat-dialog-content {
      padding-top: 16px !important;
      max-height: 70vh;
      overflow-y: auto;
    }

    /* Minimalist scrollbar */
    mat-dialog-content::-webkit-scrollbar {
      width: 6px;
    }
    mat-dialog-content::-webkit-scrollbar-track {
      background: transparent;
    }
    mat-dialog-content::-webkit-scrollbar-thumb {
      background: #ccc;
      border-radius: 3px;
    }
    mat-dialog-content::-webkit-scrollbar-thumb:hover {
      background: #aaa;
    }

    @media (max-width: 600px) {
      .form-container {
        min-width: 100%;
      }
      
      mat-dialog-content {
        max-height: 60vh;
      }
    }
  `]
})
export class CycleDialogComponent implements OnInit {
  cycleForm: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private tcmService: TcmService,
    public dialogRef: MatDialogRef<CycleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { projectId: string | number; isEdit: boolean; cycle?: TestCycle }
  ) {
    this.cycleForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      redmineProjectUrl: ['', [Validators.pattern(/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/)]],
      startDate: [null],
      endDate: [null],
      isActive: [true]
    });
  }

  ngOnInit(): void {
    if (this.data.isEdit && this.data.cycle) {
      this.cycleForm.patchValue({
        name: this.data.cycle.name,
        description: this.data.cycle.description || '',
        redmineProjectUrl: this.data.cycle.redmineProjectUrl || '',
        startDate: this.data.cycle.startDate ? new Date(this.data.cycle.startDate) : null,
        endDate: this.data.cycle.endDate ? new Date(this.data.cycle.endDate) : null,
        isActive: this.data.cycle.isActive
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (!this.cycleForm.valid) return;
    
    this.saving = true;
    const cycleData = this.cycleForm.value;
    
    if (this.data.isEdit && this.data.cycle) {
      this.tcmService.updateCycle(this.data.cycle.id!, cycleData).subscribe({
        next: (updated) => {
          this.dialogRef.close(updated);
        },
        error: (err) => {
          console.error('Failed to update cycle', err);
          this.saving = false;
        }
      });
    } else {
      this.tcmService.createCycle(this.data.projectId, cycleData).subscribe({
        next: (created) => {
          this.dialogRef.close(created);
        },
        error: (err) => {
          console.error('Failed to create cycle', err);
          this.saving = false;
        }
      });
    }
  }
}
