import { Component, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule, MatSelectionListChange } from '@angular/material/list';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { TcmService } from '../../../core/services/tcm.service';
import { User } from '../../../core/models/project.model';

interface AssignExecutionDialogData {
  userId: number | string;
  username: string;
  projectId: number | string;
  testModuleId: number | string;
  testModuleName: string;
  currentModuleIds: (number | string)[];
}

@Component({
  selector: 'app-assign-execution-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatListModule,
    FormsModule,
    MatIconModule
  ],
  templateUrl: './assign-execution-dialog.component.html',
  styleUrls: ['./assign-execution-dialog.component.css']
})
export class AssignExecutionDialogComponent {
  modules: any[] = [];
  selectedModuleIds: string[] = [];
  isLoading = true;
  isSaving = false;

  constructor(
    private tcmService: TcmService,
    private cdr: ChangeDetectorRef,
    private dialogRef: MatDialogRef<AssignExecutionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssignExecutionDialogData
  ) {
    // This dialog is for managing execution assignments for a specific user
    // Allow QA/BA/TESTER roles to be assigned for execution
  }

  ngOnInit() {
    this.selectedModuleIds = (this.data.currentModuleIds || []).map(id => String(id));
    this.isLoading = false;
    // For execution assignee dialog, we're managing assignments for a specific module
    // The checkbox represents whether the user is assigned to this module for execution
  }

  isModuleAssigned(): boolean {
    return this.selectedModuleIds.includes(String(this.data.testModuleId));
  }

  onSelectionChange(event: MatSelectionListChange) {
    const option = event.options[0];
    const moduleId = String(option.value);
    
    if (option.selected) {
      if (!this.selectedModuleIds.includes(moduleId)) {
        this.selectedModuleIds.push(moduleId);
      }
    } else {
      this.selectedModuleIds = this.selectedModuleIds.filter(id => id !== moduleId);
    }
  }

  onSave() {
    this.isSaving = true;
    this.cdr.detectChanges();

    const currentIdsStr = (this.data.currentModuleIds || []).map(id => String(id));
    const selectedSet = new Set(this.selectedModuleIds);

    const isCurrentlyAssigned = currentIdsStr.includes(String(this.data.testModuleId));
    const isSelected = selectedSet.has(String(this.data.testModuleId));

    if (isSelected && !isCurrentlyAssigned) {
      // Assign user to module for execution
      this.tcmService.assignExecutionAssignee({
        userId: this.data.userId,
        testModuleId: this.data.testModuleId
      }).subscribe({
        next: () => {
          this.isSaving = false;
          this.cdr.detectChanges();
          this.dialogRef.close(true);
        },
        error: () => {
          this.isSaving = false;
          this.cdr.detectChanges();
        }
      });
    } else if (!isSelected && isCurrentlyAssigned) {
      // Remove user from module execution assignment
      this.tcmService.removeExecutionAssignee({
        userId: this.data.userId,
        testModuleId: this.data.testModuleId
      }).subscribe({
        next: () => {
          this.isSaving = false;
          this.cdr.detectChanges();
          this.dialogRef.close(true);
        },
        error: () => {
          this.isSaving = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.isSaving = false;
      this.cdr.detectChanges();
      this.dialogRef.close(false);
    }
  }

  onCancel() {
    this.dialogRef.close(false);
  }
}