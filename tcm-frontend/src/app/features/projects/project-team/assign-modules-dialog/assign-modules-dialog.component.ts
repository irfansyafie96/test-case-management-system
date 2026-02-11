import { Component, Inject, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule, MatSelectionListChange } from '@angular/material/list';
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
export class AssignModulesDialogComponent implements OnInit {
  modules: TestModule[] = [];
  selectedModuleIds: string[] = [];
  isLoading = true;
  isSaving = false;

  constructor(
    private tcmService: TcmService,
    private cdr: ChangeDetectorRef,
    private dialogRef: MatDialogRef<AssignModulesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AssignModulesDialogData
  ) {
    // Ensure all IDs are strings for consistent comparison
    this.selectedModuleIds = (data.currentModuleIds || []).map(id => String(id));
  }

  ngOnInit() {
    this.loadModules();
  }

  loadModules() {
    this.isLoading = true;
    this.tcmService.getModulesByProject(this.data.projectId.toString()).subscribe({
      next: (modules) => {
        this.modules = modules;
        this.isLoading = false;
        // Use setTimeout to ensure change detection happens in next tick to avoid NG0100
        setTimeout(() => {
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.isLoading = false;
        setTimeout(() => {
          this.cdr.detectChanges();
        });
      }
    });
  }

  isModuleSelected(moduleId: number | string): boolean {
    return this.selectedModuleIds.includes(String(moduleId));
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

    const assignments = this.selectedModuleIds
      .filter(id => !currentIdsStr.includes(id))
      .map(moduleId => ({
        userId: this.data.userId,
        testModuleId: moduleId
      }));

    const removals = currentIdsStr
      .filter(id => !selectedSet.has(id))
      .map(moduleId => ({
        userId: this.data.userId,
        testModuleId: moduleId
      }));

    this.tcmService.bulkAssignModules(assignments, removals).subscribe({
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
  }

  onCancel() {
    this.dialogRef.close(false);
  }
}
