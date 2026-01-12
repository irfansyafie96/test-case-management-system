import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { Observable, BehaviorSubject, combineLatest, of } from 'rxjs';
import { map, catchError, finalize } from 'rxjs/operators';

interface TestAnalytics {
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
  passRate: number;
  failRate: number;
  byProject: ProjectAnalytics[];
  byModule: ModuleAnalytics[];
}

interface ProjectAnalytics {
  projectId: number;
  projectName: string;
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
}

interface ModuleAnalytics {
  moduleId: number;
  moduleName: string;
  projectId: number;
  projectName: string;
  totalTestCases: number;
  executedCount: number;
  passedCount: number;
  failedCount: number;
  notExecutedCount: number;
}

@Component({
  selector: 'app-test-cases',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule, 
    MatIconModule, 
    MatProgressBarModule, 
    MatProgressSpinnerModule,
    RouterModule
  ],
  templateUrl: './test-cases.component.html',
  styleUrls: ['./test-cases.component.css']
})
export class TestCasesComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private analyticsSubject = new BehaviorSubject<TestAnalytics>({
    totalTestCases: 0,
    executedCount: 0,
    passedCount: 0,
    failedCount: 0,
    notExecutedCount: 0,
    passRate: 0,
    failRate: 0,
    byProject: [],
    byModule: []
  });

  vm$: Observable<{ loading: boolean; error: boolean; analytics: TestAnalytics }>;

  constructor(private tcmService: TcmService) {
    this.vm$ = combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      analytics: this.analyticsSubject.asObservable()
    }).pipe(
      map(({ loading, error, analytics }) => ({ 
        loading, 
        error, 
        analytics
      }))
    );
  }

  ngOnInit() {
    this.loadAnalytics();
  }

  loadAnalytics() {
    this.loadingSubject.next(true);
    this.tcmService.getTestAnalytics().pipe(
      catchError(error => {
        console.error('Error loading analytics:', error);
        this.errorSubject.next(true);
        return of({
          totalTestCases: 0,
          executedCount: 0,
          passedCount: 0,
          failedCount: 0,
          notExecutedCount: 0,
          passRate: 0,
          failRate: 0,
          byProject: [],
          byModule: []
        });
      }),
      finalize(() => this.loadingSubject.next(false))
    ).subscribe(analytics => {
      console.log('Analytics from API:', analytics);
      console.log('Total Test Cases:', analytics.totalTestCases);
      console.log('Executed Count:', analytics.executedCount);
      console.log('Passed Count:', analytics.passedCount);
      console.log('Failed Count:', analytics.failedCount);
      console.log('By Project:', analytics.byProject);
      console.log('By Module:', analytics.byModule);
      this.analyticsSubject.next(analytics);
    });
  }

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((value / total) * 100);
  }
}
