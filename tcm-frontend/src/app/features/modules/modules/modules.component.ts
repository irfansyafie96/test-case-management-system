import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { RouterModule } from '@angular/router';
import { TcmService } from '../../../core/services/tcm.service';
import { Project, TestModule, TestSubmodule, TestCase } from '../../../core/models/project.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-modules',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, MatChipsModule, RouterModule],
  templateUrl: './modules.component.html',
  styleUrls: ['./modules.component.css']
})
export class ModulesComponent implements OnInit {
  modules$: Observable<TestModule[]>;

  constructor(private tcmService: TcmService) {
    this.modules$ = this.tcmService.getTestModulesAssignedToCurrentUser();
  }

  ngOnInit(): void {
    // Load modules on initialization
    this.tcmService.getTestModulesAssignedToCurrentUser().subscribe();
  }
}
