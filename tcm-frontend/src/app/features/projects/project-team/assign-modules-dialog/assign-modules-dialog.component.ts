import { Component, Inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { FormsModule } from '@angular/forms';
import { TcmService } from '../../../../core/services/tcm.service';
import { TestModule } from '../../../../core/models/project.model';

interface AssignModulesDialogData {
  userId: number | string;
  username: string;
  projectId: number | string;
  currentModuleIds: (number | string)[];
}

@Component({
  selector: 'app-assign-modules-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatListModule,
    FormsModule
  ],
  templateUrl: './assign-modules-dialog.component.html',
  styleUrls: ['./assign-modules-dialog.component.css']
})
export class AssignModulesDialogComponent {
  modules: TestModule[] = [];
  selectedModuleIds: Set<number | string> = new Set();
  isLoading = true;
  isSaving = false;

  constructor(
    private tcmService: TcmService,
    private cdr: ChangeDetectorRef,
    private dialogRef: MatDialogRef<AssignModulesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssignModulesDialogData
  ) {
    this.selectedModuleIds = new Set(data.currentModuleIds);
    this.loadModules();
  }

  loadModules() {
    this.tcmService.getModulesByProject(this.data.projectId.toString()).subscribe({
      next: (modules) => {
        this.modules = modules;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleModule(moduleId: number | string) {
    if (this.selectedModuleIds.has(moduleId)) {
      this.selectedModuleIds.delete(moduleId);
    } else {
      this.selectedModuleIds.add(moduleId);
    }
  }

  isModuleSelected(moduleId: number | string): boolean {
    return this.selectedModuleIds.has(moduleId);
  }

  onSave() {
    this.isSaving = true;
    const assignments = Array.from(this.selectedModuleIds).map(moduleId => ({
      userId: this.data.userId,
      moduleId: moduleId
    }));

    const removals = this.data.currentModuleIds.filter(
      id => !this.selectedModuleIds.has(id)
    ).map(moduleId => ({
      userId: this.data.userId,
      moduleId: moduleId
    }));

    this.tcmService.bulkAssignModules(assignments, removals).subscribe({
      next: () => {
        this.isSaving = false;
        this.dialogRef.close(true);
      },
      error: () => {
        this.isSaving = false;
      }
    });
  }

  onCancel() {
    this.dialogRef.close(false);
  }
}
