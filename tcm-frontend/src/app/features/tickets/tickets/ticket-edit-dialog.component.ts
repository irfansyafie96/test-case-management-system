import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';

import { Ticket } from '../../../core/models/project.model';

@Component({
  selector: 'app-ticket-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>edit</mat-icon>
      Edit Ticket
    </h2>
    
    <mat-dialog-content>
      <form [formGroup]="ticketForm" class="form-container">
        <!-- Subject -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Subject</mat-label>
          <input matInput formControlName="bugReportSubject" placeholder="Ticket subject">
        </mat-form-field>

        <!-- Description -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="bugReportDescription" rows="4" placeholder="Ticket description"></textarea>
        </mat-form-field>

        <!-- Redmine URL -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Redmine URL</mat-label>
          <input matInput formControlName="redmineIssueUrl" placeholder="https://redmine.example.com/issues/123">
          <mat-error *ngIf="ticketForm.get('redmineIssueUrl')?.hasError('pattern')">
            Please enter a valid URL
          </mat-error>
        </mat-form-field>

        <!-- Status -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="OPEN">Open</mat-option>
            <mat-option value="CLOSED">Closed</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!ticketForm.valid || saving">
        {{ saving ? 'Saving...' : 'Save' }}
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
      -ms-overflow-style: none;
      scrollbar-width: none;
    }
    mat-dialog-content::-webkit-scrollbar {
      display: none;
    }
    
    /* Thin scrollbar for textarea */
    textarea.mat-mdc-input-element {
      scrollbar-width: thin;
      scrollbar-color: #ddd transparent;
    }
    textarea.mat-mdc-input-element::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }
    textarea.mat-mdc-input-element::-webkit-scrollbar-track {
      background: transparent;
    }
    textarea.mat-mdc-input-element::-webkit-scrollbar-thumb {
      background-color: #ddd;
      border-radius: 3px;
    }
  `]
})
export class TicketEditDialogComponent implements OnInit {
  ticketForm: FormGroup;
  saving = false;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<TicketEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket }
  ) {
    this.ticketForm = this.fb.group({
      bugReportSubject: ['', Validators.required],
      bugReportDescription: [''],
      redmineIssueUrl: ['', [Validators.pattern(/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/)]],
      status: ['OPEN', Validators.required]
    });
  }

  ngOnInit(): void {
    if (this.data.ticket) {
      this.ticketForm.patchValue({
        bugReportSubject: this.data.ticket.bugReportSubject || '',
        bugReportDescription: this.data.ticket.bugReportDescription || '',
        redmineIssueUrl: this.data.ticket.redmineIssueUrl || '',
        status: this.data.ticket.status || 'OPEN'
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (!this.ticketForm.valid) return;
    
    this.saving = true;
    this.dialogRef.close(this.ticketForm.value);
  }
}
