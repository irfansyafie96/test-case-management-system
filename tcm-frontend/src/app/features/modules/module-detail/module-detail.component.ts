import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { TestSuiteDialogComponent } from './test-suite-dialog.component';
import { TestCaseDialogComponent } from './test-case-dialog.component';
import { Project, TestModule, TestSuite, TestCase } from '../../../core/models/project.model';
import { Observable, of, BehaviorSubject, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-module-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatToolbarModule,
    MatTableModule,
    MatTooltipModule,
    MatDialogModule,
    RouterModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './module-detail.component.html',
  styleUrls: ['./module-detail.component.css']
})
export class ModuleDetailComponent implements OnInit {  private route = inject(ActivatedRoute);
  private tcmService = inject(TcmService);
  private dialog = inject(MatDialog);

  displayedColumns: string[] = ['id', 'title', 'priority', 'status'];
  
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private moduleSubject = new BehaviorSubject<TestModule | null>(null);

  vm$: Observable<{ loading: boolean; error: boolean; module: TestModule | null }>;

  constructor() {
    this.vm$ = combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      module: this.moduleSubject.asObservable()
    }).pipe(
      map(({ loading, error, module }) => ({ loading, error, module }))
    );
  }

  ngOnInit(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    this.loadingSubject.next(true);
    this.errorSubject.next(false);

    if (moduleId) {
      this.tcmService.getModule(moduleId).pipe(
        catchError(error => {
          console.error('Error loading module:', error);
          this.errorSubject.next(true);
          this.loadingSubject.next(false); // Stop loading on error
          return of(null);
        })
      ).subscribe(module => {
        if (module) {
          this.moduleSubject.next(module);
          this.loadingSubject.next(false); // Stop loading only after data is emitted
        }
      });
    } else {
      this.loadingSubject.next(false);
      this.errorSubject.next(true);
    }
  }

  createTestSuite(moduleId: string | number): void {
    const idAsString = String(moduleId);
    const dialogRef = this.dialog.open(TestSuiteDialogComponent, {
      width: '400px',
      data: { moduleId: idAsString }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        this.tcmService.createTestSuite(idAsString, result).subscribe({
          next: (createdSuite) => {
            console.log('Test suite created successfully:', createdSuite);
            // Instead of calling ngOnInit(), refresh the module data directly
            this.refreshModuleData();
          },
          error: (error) => {
            console.error('Error creating test suite:', error);
            this.loadingSubject.next(false); // Hide loading indicator
            alert('Failed to create test suite. Please try again.');
          }
        });
      }
    });
  }

  createTestCase(suiteId: string | number): void {
    const idAsString = String(suiteId);
    const dialogRef = this.dialog.open(TestCaseDialogComponent, {
      width: '600px',
      data: { suiteId: idAsString }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadingSubject.next(true); // Show loading indicator
        this.tcmService.createTestCase(idAsString, result).subscribe({
          next: (createdTestCase) => {
            console.log('Test case created successfully:', createdTestCase);
            // Instead of calling ngOnInit(), refresh the module data directly
            this.refreshModuleData();
          },
          error: (error) => {
            console.error('Error creating test case:', error);
            this.loadingSubject.next(false); // Hide loading indicator
            alert('Failed to create test case. Please try again.');
          }
        });
      }
    });
  }

  private refreshModuleData(): void {
    const moduleId = this.route.snapshot.paramMap.get('id');
    if (moduleId) {
      this.tcmService.getModule(moduleId).pipe(
        catchError(error => {
          console.error('Error refreshing module:', error);
          this.errorSubject.next(true);
          return of(null);
        }),
        finalize(() => {
          this.loadingSubject.next(false);
        })
      ).subscribe(module => {
        if (module) {
          this.moduleSubject.next(module);
          this.errorSubject.next(false); // Clear error state when module loads successfully
        }
      });
    }
  }

  getPriorityClass(priority: string | undefined): string {
    if (!priority) return 'low';
    return priority.toLowerCase();
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'not_executed';
    return status.toLowerCase();
  }
}
