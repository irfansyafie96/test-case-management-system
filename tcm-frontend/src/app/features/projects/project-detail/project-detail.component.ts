import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { ModuleDialogComponent } from '../../modules/modules/module-dialog.component';
import { ConfirmationDialogComponent } from '../../../shared/confirmation-dialog/confirmation-dialog.component';
import { Project, TestModule } from '../../../core/models/project.model';
import { Observable, BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, RouterModule, MatDialogModule, MatTooltipModule, MatProgressSpinnerModule],
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css']
})
export class ProjectDetailComponent implements OnInit {  
  project$!: Observable<Project>;
  loading$ = new BehaviorSubject<boolean>(false);
  creatingModule$ = new BehaviorSubject<boolean>(false);
  isDeletingModule$ = new BehaviorSubject<boolean>(false);
  deletingModuleId: string | number | null = null;

  constructor(
    private route: ActivatedRoute,
    private tcmService: TcmService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('id');
    if (projectId) {
      this.project$ = this.tcmService.getProject(projectId);
    }
  }

  openModuleDialog(projectId: string | number): void {
    const idAsString = String(projectId);
    const dialogRef = this.dialog.open(ModuleDialogComponent, {
      width: '400px',
      data: { projectId: idAsString }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.creatingModule$.next(true);
        this.loading$.next(true);

        this.tcmService.createModule(idAsString, result).subscribe(
          (createdModule) => {
            this.project$ = this.tcmService.getProject(idAsString);
            this.creatingModule$.next(false);
            this.loading$.next(false);
          },
          error => {
            console.error('Error creating test module:', error);
            this.creatingModule$.next(false);
            this.loading$.next(false);
          }
        );
      }
    });
  }

  deleteModule(event: Event, moduleId: string | number): void {
    const idAsString = String(moduleId);
    event.stopPropagation();
    event.preventDefault();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Delete Module',
        message: 'Are you sure you want to delete this module? This action cannot be undone.',
        icon: 'warning',
        confirmButtonText: 'Delete',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.isDeletingModule$.next(true);
        this.loading$.next(true);
        this.deletingModuleId = moduleId;

        this.tcmService.deleteModule(idAsString).subscribe(
          () => {
            const currentProjectId = this.route.snapshot.paramMap.get('id');
            if (currentProjectId) {
              this.project$ = this.tcmService.getProject(currentProjectId);
            }
            this.isDeletingModule$.next(false);
            this.loading$.next(false);
            this.deletingModuleId = null;
          },
          error => {
            console.error('Error deleting module:', error);
            alert('Failed to delete module. Please try again.');
            this.isDeletingModule$.next(false);
            this.loading$.next(false);
            this.deletingModuleId = null;
          }
        );
      }
    });
  }

  getTotalTestCases(testSuites: any[] | undefined): number {
    if (!testSuites) return 0;
    return testSuites.reduce((total, suite) => {
      return total + (suite.testCases ? suite.testCases.length : 0);
    }, 0);
  }
}
