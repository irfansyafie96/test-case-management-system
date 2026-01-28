import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TcmService } from '../../../core/services/tcm.service';
import { User } from '../../../core/models/project.model';
import { InviteDialogComponent } from '../invite-dialog/invite-dialog.component';

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDialogModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './team.component.html',
  styleUrls: ['./team.component.css']
})
export class TeamComponent implements OnInit {
  users: User[] = [];
  displayedColumns: string[] = ['username', 'email', 'roles', 'status'];
  isLoading = false;

  constructor(
    private tcmService: TcmService,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.loadTeam();
  }

  loadTeam() {
    this.isLoading = true;
    this.tcmService.getAllNonAdminUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
      }
    });
  }

  openInviteDialog() {
    const dialogRef = this.dialog.open(InviteDialogComponent, {
      width: '400px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Invite sent successfully, maybe show a snackbar?
        // Refresh list if we were listing pending invites (future feature)
      }
    });
  }
}
