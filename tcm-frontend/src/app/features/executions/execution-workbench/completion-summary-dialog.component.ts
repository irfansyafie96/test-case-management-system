import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { CompletionSummary } from '../../../core/models/project.model';

@Component({
  selector: 'app-completion-summary-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, MatCardModule],
  templateUrl: './completion-summary-dialog.component.html',
  styleUrls: ['./completion-summary-dialog.component.css']
})
export class CompletionSummaryDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<CompletionSummaryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CompletionSummary
  ) {}

  onBackToList(): void {
    this.dialogRef.close('back');
  }

  onClose(): void {
    this.dialogRef.close();
  }
}