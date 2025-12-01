import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProjectDialogComponent } from './project-dialog.component';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { TcmService } from '../../../core/services/tcm.service';
import { Project } from '../../../core/models/project.model';
import { Observable } from 'rxjs';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltipModule,
    MatDialogModule,
    RouterModule
  ],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css']
})
export class ProjectsComponent implements OnInit {
  projects$!: Observable<Project[]>;

  constructor(public dialog: MatDialog, private tcmService: TcmService) {}

  ngOnInit(): void {
    this.projects$ = this.tcmService.projects$;
    this.tcmService.getProjects().subscribe();
  }

  openProjectDialog(): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '400px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.createProject(result).subscribe();
      }
    });
  }

  deleteProject(event: Event, projectId: string | number): void {
    const idAsString = String(projectId);
    // Prevent the click from navigating to the project page
    event.stopPropagation();
    event.preventDefault();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Project',
        message: 'Are you sure you want to delete this project? This action cannot be undone and will also delete all modules, test suites, test cases and executions associated with this project.',
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) { // User confirmed
        this.tcmService.deleteProject(idAsString).subscribe(
          () => {
            // The project list will be refreshed automatically due to the tap() in the service
            console.log('Project deleted successfully');
          },
          error => {
            console.error('Error deleting project:', error);
            alert('Failed to delete project. Please try again.');
          }
        );
      }
    });
  }
}
