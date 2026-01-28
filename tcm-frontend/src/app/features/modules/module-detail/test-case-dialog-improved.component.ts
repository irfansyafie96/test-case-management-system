import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormArray } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-test-case-dialog-improved',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDialogModule,
    MatIconModule,
    MatTooltipModule,
    MatCardModule,
    MatDividerModule
  ],
  templateUrl: './test-case-dialog-improved.component.html',
  styleUrls: ['./test-case-dialog-improved.component.css']
})
export class TestCaseDialogImprovedComponent {
  testCaseForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<TestCaseDialogImprovedComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    // Initialize form with data or default values
    this.testCaseForm = this.fb.group({
      testCaseId: [data?.testCaseId || '', Validators.required],
      title: [data?.title || '', Validators.required],
      description: [data?.description || ''],
      scenario: [data?.scenario || ''],
      testSteps: this.fb.array([])
    });

    // Initialize test steps if editing existing test case
    if (data?.testSteps && data.testSteps.length > 0) {
      data.testSteps.forEach((step: any) => {
        this.addTestStepWithValues(step.action, step.expectedResult);
      });
    } else {
      // Add one empty step by default
      this.addTestStep();
    }
  }

  get testSteps(): FormArray {
    return this.testCaseForm.get('testSteps') as FormArray;
  }

  createTestStep(action: string = '', expectedResult: string = ''): FormGroup {
    return this.fb.group({
      action: [action, Validators.required],
      expectedResult: [expectedResult, Validators.required]
    });
  }

  addTestStep(): void {
    this.testSteps.push(this.createTestStep());
  }

  addTestStepWithValues(action: string, expectedResult: string): void {
    this.testSteps.push(this.createTestStep(action, expectedResult));
  }

  removeTestStep(index: number): void {
    this.testSteps.removeAt(index);
  }

  moveStepUp(index: number): void {
    if (index > 0) {
      const steps = this.testSteps.controls;
      const step = steps[index];
      steps[index] = steps[index - 1];
      steps[index - 1] = step;
      this.testSteps.setValue(steps.map(s => s.value));
    }
  }

  moveStepDown(index: number): void {
    if (index < this.testSteps.length - 1) {
      const steps = this.testSteps.controls;
      const step = steps[index];
      steps[index] = steps[index + 1];
      steps[index + 1] = step;
      this.testSteps.setValue(steps.map(s => s.value));
    }
  }

  getFormData(): any {
    return {
      ...this.testCaseForm.value,
      id: this.data?.id // Include ID if editing existing test case
    };
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.testCaseForm.valid) {
      this.dialogRef.close(this.getFormData());
    }
  }
}