import { Component, Inject, inject, EventEmitter, Output, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TcmService } from '../../../core/services/tcm.service';

export interface ImportDialogData {
  moduleId: string;
}

@Component({
  selector: 'app-import-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatDividerModule,
    MatDialogModule,
    MatTooltipModule
  ],
  templateUrl: './import-dialog.component.html',
  styleUrls: ['./import-dialog.component.css']
})
export class ImportDialogComponent {
  private tcmService = inject(TcmService);
  private dialogRef = inject(MatDialogRef<ImportDialogComponent>);
  private data = inject<ImportDialogData>(MAT_DIALOG_DATA);
  private cdr = inject(ChangeDetectorRef);

  @Output() importSuccess = new EventEmitter<void>();

  uploadForm: FormGroup;
  selectedFile: File | null = null;
  isUploading = false;
  uploadResult: any = null;
  errorMessage: string = '';

  constructor() {
    this.uploadForm = new FormBuilder().group({
      file: [null, Validators.required]
    });
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      // Validate file type
      if (!file.name.toLowerCase().endsWith('.xlsx')) {
        this.errorMessage = 'Only .xlsx files are allowed';
        this.selectedFile = null;
        this.uploadForm.patchValue({ file: null });
        return;
      }

      // Validate file size (max 10MB)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        this.errorMessage = 'File size exceeds 10MB limit';
        this.selectedFile = null;
        this.uploadForm.patchValue({ file: null });
        return;
      }

      this.selectedFile = file;
      this.errorMessage = '';
      this.uploadForm.patchValue({ file: file });
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      const file = files[0];
      const inputEvent = { target: { files: [file] } };
      this.onFileSelected(inputEvent);
    }
  }

  removeFile(): void {
    this.selectedFile = null;
    this.errorMessage = '';
    this.uploadForm.patchValue({ file: null });
  }

  downloadTemplate(): void {
    this.tcmService.downloadTemplate().subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'test-case-import-template.xlsx';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (err) => {
        this.errorMessage = 'Failed to download template. Please try again.';
      }
    });
  }

  onSubmit(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Please select a file to import';
      return;
    }

    this.isUploading = true;
    this.errorMessage = '';
    this.uploadResult = null;

    this.tcmService.importTestCases(this.data.moduleId, this.selectedFile).subscribe({
      next: (result) => {
        this.isUploading = false;
        this.uploadResult = result;
        if (!result.success) {
          this.errorMessage = result.message || 'Import failed';
        } else {
          // Notify parent immediately on success
          this.importSuccess.emit();
        }
        this.cdr.detectChanges(); // Force update to prevent NG0100 error
      },
      error: (err) => {
        this.isUploading = false;
        this.errorMessage = err.error?.message || err.message || 'Import failed. Please try again.';
        this.cdr.detectChanges(); // Force update on error too
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onClose(): void {
    this.dialogRef.close(this.uploadResult?.success ? { success: true } : null);
  }

  public formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}