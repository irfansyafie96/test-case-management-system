import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TcmService } from '../../../core/services/tcm.service';
import { TestCase } from '../../../core/models/project.model';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-test-case-detail',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule, 
    MatIconModule, 
    MatChipsModule, 
    MatDividerModule,
    RouterModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './test-case-detail.component.html',
  styleUrls: ['./test-case-detail.component.css']
})
export class TestCaseDetailComponent implements OnInit, OnDestroy {
  testCaseId: string | null = null;
  testCase: TestCase | null = null;
  isLoading = true;
  error: string | null = null;
  private routeSub: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private tcmService: TcmService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    console.log('TestCaseDetailComponent initialized');
    
    // Force change detection after a short delay to handle SSR hydration
    setTimeout(() => {
      this.cdr.detectChanges();
      console.log('Manual change detection triggered');
    }, 100);
    
    // Subscribe to route parameter changes instead of using snapshot
    this.routeSub = this.route.paramMap.subscribe(params => {
      this.testCaseId = params.get('id');
      console.log('Route param changed, testCaseId:', this.testCaseId);
      
      if (this.testCaseId) {
        this.loadTestCase(this.testCaseId);
      }
    });
  }

  ngOnDestroy() {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  loadTestCase(id: string) {
    console.log('Loading test case with ID:', id);
    this.isLoading = true;
    this.error = null;
    this.cdr.detectChanges(); // Force UI update immediately
    
    this.tcmService.getTestCase(id).subscribe({
      next: (data) => {
        console.log('Test case loaded successfully:', data);
        this.testCase = data;
        this.isLoading = false;
        this.cdr.detectChanges(); // Force UI update
      },
      error: (err) => {
        console.error('Error loading test case:', err);
        this.error = 'Failed to load test case details.';
        this.isLoading = false;
        this.cdr.detectChanges(); // Force UI update
      }
    });
  }
}
