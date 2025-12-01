import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';

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
    MatTooltipModule
  ],
  templateUrl: './test-case-detail.component.html',
  styleUrls: ['./test-case-detail.component.css']
})
export class TestCaseDetailComponent implements OnInit {
  testCaseId: string | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.testCaseId = this.route.snapshot.paramMap.get('id');
  }
}
