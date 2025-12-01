import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-executions',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, MatIconModule, MatTableModule, RouterModule],
  templateUrl: './executions.component.html',
  styleUrls: ['./executions.component.css']
})
export class ExecutionsComponent {}
