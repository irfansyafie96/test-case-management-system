import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { TestCase } from '../../../core/models/project.model';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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
    RouterModule
  ],
  templateUrl: './test-cases.component.html',
  styleUrls: ['./test-cases.component.css']
})
export class TestCasesComponent implements OnInit {
  testCases$: Observable<TestCase[]>;
  displayedColumns: string[] = ['id', 'title', 'priority', 'actions'];

  constructor(private tcmService: TcmService) {
    // We need to aggregate test cases from projects -> modules -> suites -> cases
    // This is inefficient but works for the prototype without a dedicated "getAllTestCases" endpoint
    this.testCases$ = this.tcmService.projects$.pipe(
      map(projects => {
        const allCases: TestCase[] = [];
        projects.forEach(p => {
          p.modules?.forEach(m => {
            m.testSuites?.forEach(s => {
              s.testCases?.forEach(c => {
                allCases.push(c);
              });
            });
          });
        });
        return allCases;
      })
    );
  }

  ngOnInit() {
    this.tcmService.getProjects().subscribe();
  }
}
