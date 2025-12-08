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

@Component({
  selector: 'app-test-case-dialog',
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
    MatTooltipModule
  ],
  templateUrl: './test-case-dialog.component.html',
  styleUrls: ['./test-case-dialog.component.css']
})
export class TestCaseDialogComponent {
  testCaseForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<TestCaseDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: FormBuilder
  ) {
    this.testCaseForm = this.fb.group({
      testCaseId: ['', Validators.required],
      title: ['', Validators.required],
      description: [''],
      testSteps: this.fb.array([this.createTestStep()])
    });
  }

  get testSteps(): FormArray {
    return this.testCaseForm.get('testSteps') as FormArray;
  }

  createTestStep(): FormGroup {
    return this.fb.group({
      action: ['', Validators.required],
      expectedResult: ['', Validators.required]
    });
  }

  addTestStep(): void {
    this.testSteps.push(this.createTestStep());
  }

  removeTestStep(index: number): void {
    this.testSteps.removeAt(index);
  }

  getFormData(): any {
    return this.testCaseForm.value;
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
