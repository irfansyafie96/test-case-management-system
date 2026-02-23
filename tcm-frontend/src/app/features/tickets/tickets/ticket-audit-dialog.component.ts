import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { Ticket, TicketAuditLog } from '../../../core/models/project.model';

@Component({
  selector: 'app-ticket-audit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>history</mat-icon>
      Audit Trail
    </h2>
    
    <mat-dialog-content>
      <div class="ticket-info" *ngIf="data.ticket">
        <p><strong>Subject:</strong> {{ data.ticket.bugReportSubject || 'N/A' }}</p>
        <p><strong>Project:</strong> {{ data.ticket.projectName || 'N/A' }}</p>
        <p><strong>Current Status:</strong> {{ data.ticket.status }}</p>
      </div>

      <div class="audit-list" *ngIf="data.ticket.auditLogs && data.ticket.auditLogs.length > 0">
        <table mat-table [dataSource]="data.ticket.auditLogs || []" class="audit-table">
          
          <ng-container matColumnDef="changedAt">
            <th mat-header-cell *matHeaderCellDef> Date </th>
            <td mat-cell *matCellDef="let log"> {{ log.changedAt | date:'medium' }} </td>
          </ng-container>

          <ng-container matColumnDef="action">
            <th mat-header-cell *matHeaderCellDef> Action </th>
            <td mat-cell *matCellDef="let log"> 
              <span class="action-badge" [ngClass]="getActionClass(log.action)">
                {{ getActionLabel(log.action) }}
              </span>
            </td>
          </ng-container>

          <ng-container matColumnDef="changedBy">
            <th mat-header-cell *matHeaderCellDef> By </th>
            <td mat-cell *matCellDef="let log"> {{ log.changedBy }} </td>
          </ng-container>

          <ng-container matColumnDef="details">
            <th mat-header-cell *matHeaderCellDef> Details </th>
            <td mat-cell *matCellDef="let log"> 
              <span *ngIf="log.action === 'STATUS_CHANGED'">
                {{ log.oldValue || 'None' }} → {{ log.newValue }}
              </span>
              <span *ngIf="log.action !== 'STATUS_CHANGED'">
                {{ log.notes || '-' }}
              </span>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>

      <div class="no-logs" *ngIf="!data.ticket.auditLogs?.length">
        <p>No audit trail available for this ticket.</p>
      </div>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
    }
    
    .ticket-info {
      background: #f5f5f5;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
    }
    
    .ticket-info p {
      margin: 4px 0;
      font-size: 14px;
    }
    
    .audit-table {
      width: 100%;
    }
    
    .action-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 500;
      text-transform: uppercase;
    }
    
    .action-badge.created {
      background: #e3f2fd;
      color: #1976D2;
    }
    
    .action-badge.status-changed {
      background: #fff3e0;
      color: #F57C00;
    }
    
    .action-badge.updated {
      background: #f3e5f5;
      color: #7B1FA2;
    }
    
    .no-logs {
      text-align: center;
      color: #666;
      padding: 20px;
    }
  `]
})
export class TicketAuditDialogComponent {
  displayedColumns: string[] = ['changedAt', 'action', 'changedBy', 'details'];

  constructor(
    public dialogRef: MatDialogRef<TicketAuditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { ticket: Ticket }
  ) {}

  getActionClass(action: string): string {
    switch (action) {
      case 'CREATED': return 'created';
      case 'STATUS_CHANGED': return 'status-changed';
      default: return 'updated';
    }
  }

  getActionLabel(action: string): string {
    switch (action) {
      case 'CREATED': return 'Created';
      case 'STATUS_CHANGED': return 'Status Changed';
      case 'UPDATED': return 'Updated';
      default: return action;
    }
  }
}
