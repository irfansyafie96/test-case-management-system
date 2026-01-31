import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface RedmineIssueData {
  testCaseId: string;
  testCaseTitle: string;
  testSteps: any[];
  execution: any;
}

@Component({
  selector: 'app-redmine-issue-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatDialogModule, MatIconModule],
  templateUrl: './redmine-issue-dialog.component.html',
  styleUrls: ['./redmine-issue-dialog.component.css']
})
export class RedmineIssueDialogComponent {
  redmineForm: FormGroup;
  redmineUrl: string = 'http://tmsredmine.tmsasia.com/projects/hrdcncspilot/issues/new';
  
  constructor(
    public dialogRef: MatDialogRef<RedmineIssueDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RedmineIssueData,
    private fb: FormBuilder
  ) {
    // Pre-fill subject with test case ID and title
    const defaultSubject = `[${data.testCaseId}] ${data.testCaseTitle} - FAILED`;
    
    // Generate detailed description with test case details
    const defaultDescription = this.generateDefaultDescription();
    
    this.redmineForm = this.fb.group({
      subject: [defaultSubject, Validators.required],
      description: [defaultDescription, Validators.required],
      redmineLink: ['']
    });
  }
  
  /**
   * Generate a detailed bug report description
   * Includes test case information, execution details, and step results
   */
  private generateDefaultDescription(): string {
    let desc = `=== TEST CASE FAILURE REPORT ===\n\n`;
    desc += `Test Case ID: ${this.data.testCaseId}\n`;
    desc += `Test Case Title: ${this.data.testCaseTitle}\n`;
    desc += `Execution Date: ${this.data.execution.executionDate || 'N/A'}\n`;
    desc += `Environment: ${this.data.execution.environment || 'N/A'}\n`;
    desc += `Executed By: ${this.data.execution.executedBy || 'N/A'}\n`;
    desc += `Execution Notes: ${this.data.execution.notes || 'No notes provided'}\n\n`;
    
    // Add test steps information
    if (this.data.testSteps && this.data.testSteps.length > 0) {
      desc += `=== TEST STEPS ===\n\n`;
      this.data.testSteps.forEach((step, index) => {
        desc += `Step ${step.stepNumber}:\n`;
        desc += `Action: ${step.action}\n`;
        desc += `Expected Result: ${step.expectedResult}\n`;
        desc += `Actual Result: ${step.actualResult || 'N/A'}\n`;
        desc += `Status: ${step.status || 'N/A'}\n\n`;
      });
    }
    
    return desc;
  }
  
  /**
   * Open Redmine with pre-filled data
   * Encodes subject and description and opens in new tab
   */
  openRedmineDirectly(): void {
    const subject = this.redmineForm.value.subject;
    const description = this.redmineForm.value.description;
    
    // Encode parameters for URL
    const encodedSubject = encodeURIComponent(subject);
    const encodedDescription = encodeURIComponent(description);
    
    // Construct URL with pre-filled data
    const url = `${this.redmineUrl}?issue[subject]=${encodedSubject}&issue[description]=${encodedDescription}`;
    
    // Open in new tab
    window.open(url, '_blank');
    
    // Close dialog with the data
    this.dialogRef.close({
      subject: subject,
      description: description,
      redmineLink: url
    });
  }
  
  /**
   * Save Redmine data without opening Redmine
   * User manually creates issue and pastes link later
   */
  saveOnly(): void {
    this.dialogRef.close({
      subject: this.redmineForm.value.subject,
      description: this.redmineForm.value.description,
      redmineLink: this.redmineForm.value.redmineLink
    });
  }
  
  /**
   * Cancel and close dialog
   */
  cancel(): void {
    this.dialogRef.close();
  }
}
