import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { TestCase } from '../../../core/models/project.model';
import { Observable, BehaviorSubject, combineLatest, of } from 'rxjs';
import { map, catchError, finalize } from 'rxjs/operators';

@Component({
  selector: 'app-test-cases',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule, 
    MatIconModule, 
    MatTableModule, 
    MatChipsModule, 
    MatTooltipModule,
    MatProgressSpinnerModule,
    RouterModule
  ],
  templateUrl: './test-cases.component.html',
  styleUrls: ['./test-cases.component.css']
})
export class TestCasesComponent implements OnInit {
  displayedColumns: string[] = ['id', 'title', 'status', 'actions'];

  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<boolean>(false);
  private testCasesSubject = new BehaviorSubject<TestCase[]>([]);

  vm$: Observable<{ loading: boolean; error: boolean; testCases: TestCase[] }>;

  constructor(private tcmService: TcmService) {
    this.vm$ = combineLatest({
      loading: this.loadingSubject.asObservable(),
      error: this.errorSubject.asObservable(),
      testCases: this.testCasesSubject.asObservable()
    }).pipe(
      map(({ loading, error, testCases }) => ({ loading, error, testCases }))
    );
  }

  ngOnInit() {
    this.loadTestCases();
  }

  loadTestCases() {
    this.loadingSubject.next(true);
    this.tcmService.getAllTestCases().pipe(
      catchError(error => {
        console.error('Error loading test cases:', error);
        this.errorSubject.next(true);
        return of([]);
      }),
      finalize(() => this.loadingSubject.next(false))
    ).subscribe(testCases => {
      this.testCasesSubject.next(testCases);
    });
  }


  getStatusClass(status: string | undefined): string {
    if (!status) return 'draft';
    return status.toLowerCase();
  }
}
