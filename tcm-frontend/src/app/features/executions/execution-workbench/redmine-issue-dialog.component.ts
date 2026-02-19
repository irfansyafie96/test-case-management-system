import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RedmineIssue } from '../../../core/models/project.model';
import { TcmService } from '../../../core/services/tcm.service';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';

export interface RedmineIssueData {
  testCaseId: string;
  testCaseTitle: string;
  testSteps: any[];
  execution: any;
  existingIssues?: RedmineIssue[];
}

export interface RedmineIssueResult {
  subject: string;
  description: string;
  redmineLink: string;
  redmineIssueId?: string;
}

@Component({
  selector: 'app-redmine-issue-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatDialogModule, MatIconModule, MatListModule, MatSnackBarModule],
  templateUrl: './redmine-issue-dialog.component.html',
  styleUrls: ['./redmine-issue-dialog.component.css']
})
export class RedmineIssueDialogComponent implements OnInit {
  redmineForm: FormGroup;
  redmineUrl: string = 'http://tmsredmine.tmsasia.com/projects/hrdcncspilot/issues/new';
  existingIssues: RedmineIssue[] = [];
  editingIssue: RedmineIssue | null = null;

  constructor(
    public dialogRef: MatDialogRef<RedmineIssueDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RedmineIssueData,
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private tcmService: TcmService
  ) {
    this.existingIssues = data.existingIssues || [];
    this.redmineForm = this.fb.group({
      subject: ['', Validators.required],
      description: ['', Validators.required],
      redmineLink: ['']
    });
  }
  
  ngOnInit(): void {
    if (!this.data.existingIssues || this.data.existingIssues.length === 0) {
      this.prefillForm();
    }
  }
  
  prefillForm(): void {
    const defaultSubject = `[${this.data.testCaseId}] ${this.data.testCaseTitle} - FAILED`;
    const defaultDescription = this.generateDefaultDescription();
    
    this.redmineForm.patchValue({
      subject: defaultSubject,
      description: defaultDescription,
      redmineLink: ''
    });
  }
  
  generateDefaultDescription(): string {
    let desc = `=== TEST CASE FAILURE REPORT ===\n\n`;
    desc += `Test Case ID: ${this.data.testCaseId}\n`;
    desc += `Test Case Title: ${this.data.testCaseTitle}\n`;
    desc += `Execution Date: ${this.data.execution.executionDate || 'N/A'}\n`;
    desc += `Environment: ${this.data.execution.environment || 'N/A'}\n`;
    desc += `Executed By: ${this.data.execution.executedBy || 'N/A'}\n`;
    desc += `Execution Notes: ${this.data.execution.notes || 'No notes provided'}\n\n`;
    
    if (this.data.testSteps && this.data.testSteps.length > 0) {
      desc += `=== TEST STEPS ===\n\n`;
      this.data.testSteps.forEach((step) => {
        desc += `Step ${step.stepNumber}:\n`;
        desc += `Action: ${step.action}\n`;
        desc += `Expected Result: ${step.expectedResult}\n`;
        desc += `Actual Result: ${step.actualResult || 'N/A'}\n`;
        desc += `Status: ${step.status || 'N/A'}\n\n`;
      });
    }
    
    return desc;
  }
  
  openRedmineDirectly(): void {
    const subject = this.redmineForm.value.subject;
    const description = this.redmineForm.value.description;
    
    const encodedSubject = encodeURIComponent(subject);
    const encodedDescription = encodeURIComponent(description);
    
    const url = `${this.redmineUrl}?issue[subject]=${encodedSubject}&issue[description]=${encodedDescription}`;
    
    window.open(url, '_blank');
    
    this.redmineForm.patchValue({
      redmineLink: 'After creating the issue in Redmine, paste the issue URL here'
    });
  }
  
  saveIssue(): void {
    if (this.redmineForm.invalid) return;
    
    const result: RedmineIssueResult = {
      subject: this.redmineForm.value.subject,
      description: this.redmineForm.value.description,
      redmineLink: this.redmineForm.value.redmineLink,
      redmineIssueId: this.extractIssueId(this.redmineForm.value.redmineLink)
    };
    
    this.dialogRef.close(result);
  }
  
  cancel(): void {
    this.dialogRef.close();
  }
  
  editIssue(issue: RedmineIssue): void {
    this.editingIssue = issue;
    this.redmineForm.patchValue({
      subject: issue.bugReportSubject || `[${this.data.testCaseId}] ${this.data.testCaseTitle} - FAILED`,
      description: issue.bugReportDescription || this.generateDefaultDescription(),
      redmineLink: issue.redmineIssueUrl
    });
  }
  
  cancelEdit(): void {
    this.editingIssue = null;
    this.prefillForm();
  }
  
  saveEditedIssue(): void {
    if (this.redmineForm.invalid || !this.editingIssue) return;

    const result: RedmineIssueResult & { issueId: number | string } = {
      subject: this.redmineForm.value.subject,
      description: this.redmineForm.value.description,
      redmineLink: this.redmineForm.value.redmineLink,
      redmineIssueId: this.extractIssueId(this.redmineForm.value.redmineLink),
      issueId: this.editingIssue.id
    };

    this.dialogRef.close(result);
  }

  /**
   * Delete a Redmine issue from the execution
   */
  deleteIssue(issue: RedmineIssue): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Redmine Issue Link',
        message: `Are you sure you want to remove the link to Redmine issue "${issue.bugReportSubject}"? This will only remove the link from TCM. The Redmine issue itself will not be deleted.`,
        icon: 'delete',
        confirmButtonText: 'DELETE',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.tcmService.deleteRedmineIssue(String(this.data.execution.id), String(issue.id)).subscribe({
          next: () => {
            // Remove from local array
            this.existingIssues = this.existingIssues.filter(i => i.id !== issue.id);
            this.snackBar.open(
              'Redmine issue link removed successfully!',
              'DISMISS',
              { panelClass: ['success-snackbar'], duration: 3000, horizontalPosition: 'right', verticalPosition: 'top' }
            );
          },
          error: (error) => {
            this.snackBar.open(
              'Failed to delete Redmine issue link. Please try again.',
              'RETRY',
              { panelClass: ['error-snackbar'], duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
            );
          }
        });
      }
    });
  }

  private extractIssueId(url: string): string | undefined {
    if (!url) return undefined;
    const match = url.match(/issues\/(\d+)/);
    return match ? match[1] : undefined;
  }
}
