import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { Observable, BehaviorSubject, combineLatest } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';

import { TcmService } from '../../../core/services/tcm.service';
import { AuthService } from '../../../core/services/auth.service';
import { Project, Ticket, TestCycle } from '../../../core/models/project.model';
import { TicketAuditDialogComponent } from './ticket-audit-dialog.component';
import { TicketEditDialogComponent } from './ticket-edit-dialog.component';

interface TicketsViewModel {
  tickets: Ticket[];
  projects: Project[];
  cycles: TestCycle[];
  loading: boolean;
  error: string | null;
  selectedProjectId: string;
  selectedCycleId: string;
  selectedStatus: string;
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule
  ],
  templateUrl: './tickets.component.html',
  styleUrls: ['./tickets.component.css']
})
export class TicketsComponent implements OnInit {
  private loadingSubject = new BehaviorSubject<boolean>(true);
  private errorSubject = new BehaviorSubject<string | null>(null);
  private ticketsSubject = new BehaviorSubject<Ticket[]>([]);
  private projectsSubject = new BehaviorSubject<Project[]>([]);
  private cyclesSubject = new BehaviorSubject<TestCycle[]>([]);
  private selectedProjectIdSubject = new BehaviorSubject<string>('all');
  private selectedCycleIdSubject = new BehaviorSubject<string>('all');
  private selectedStatusSubject = new BehaviorSubject<string>('all');

  loading$ = this.loadingSubject.asObservable();
  error$ = this.errorSubject.asObservable();
  tickets$ = this.ticketsSubject.asObservable();
  projects$ = this.projectsSubject.asObservable();
  cycles$ = this.cyclesSubject.asObservable();
  selectedProjectId$ = this.selectedProjectIdSubject.asObservable();
  selectedCycleId$ = this.selectedCycleIdSubject.asObservable();
  selectedStatus$ = this.selectedStatusSubject.asObservable();

  displayedColumns: string[] = ['status', 'subject', 'project', 'cycle', 'testCase', 'createdAt', 'actions'];

  vm$: Observable<TicketsViewModel>;

  constructor(
    private tcmService: TcmService,
    private authService: AuthService,
    private dialog: MatDialog
  ) {
    this.vm$ = combineLatest([
      this.loading$,
      this.error$,
      this.tickets$,
      this.projects$,
      this.cycles$,
      this.selectedProjectId$,
      this.selectedCycleId$,
      this.selectedStatus$
    ]).pipe(
      map(([loading, error, tickets, projects, cycles, selectedProjectId, selectedCycleId, selectedStatus]) => ({
        loading,
        error,
        tickets,
        projects,
        cycles,
        selectedProjectId,
        selectedCycleId,
        selectedStatus
      }))
    );
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loadingSubject.next(true);
    this.errorSubject.next(null);

    this.tcmService.getProjects().subscribe({
      next: (projects) => {
        this.projectsSubject.next(projects);
        this.loadTickets();
      },
      error: (err) => {
        this.errorSubject.next('Failed to load projects');
        this.loadingSubject.next(false);
      }
    });
  }

  loadTickets(): void {
    const projectId = this.selectedProjectIdSubject.value === 'all' ? undefined : this.selectedProjectIdSubject.value;
    const cycleId = this.selectedCycleIdSubject.value === 'all' ? undefined : this.selectedCycleIdSubject.value;
    const status = this.selectedStatusSubject.value === 'all' ? undefined : this.selectedStatusSubject.value;

    this.tcmService.getTickets(projectId, cycleId, status).subscribe({
      next: (tickets) => {
        this.ticketsSubject.next(tickets);
        this.loadingSubject.next(false);
      },
      error: (err) => {
        this.errorSubject.next('Failed to load tickets');
        this.loadingSubject.next(false);
      }
    });
  }

  onProjectChange(projectId: string): void {
    this.selectedProjectIdSubject.next(projectId);
    if (projectId && projectId !== 'all') {
      this.tcmService.getCyclesByProject(projectId).subscribe({
        next: (cycles) => {
          this.cyclesSubject.next(cycles);
          this.selectedCycleIdSubject.next('all');
          this.loadTickets();
        }
      });
    } else {
      this.cyclesSubject.next([]);
      this.selectedCycleIdSubject.next('all');
      this.loadTickets();
    }
  }

  onCycleChange(cycleId: string): void {
    this.selectedCycleIdSubject.next(cycleId);
    this.loadTickets();
  }

  onStatusChange(status: string): void {
    this.selectedStatusSubject.next(status);
    this.loadTickets();
  }

  toggleTicketStatus(ticket: Ticket): void {
    const newStatus = ticket.status === 'OPEN' ? 'CLOSED' : 'OPEN';
    this.tcmService.updateTicketStatus(ticket.id, newStatus).subscribe({
      next: () => {
        this.loadTickets();
      },
      error: (err) => {
        console.error('Failed to update ticket status', err);
      }
    });
  }

  openInRedmine(url: string): void {
    window.open(url, '_blank');
  }

  viewAuditTrail(ticket: Ticket): void {
    this.dialog.open(TicketAuditDialogComponent, {
      width: '600px',
      data: { ticket }
    });
  }

  editTicket(ticket: Ticket): void {
    const dialogRef = this.dialog.open(TicketEditDialogComponent, {
      width: '500px',
      data: { ticket }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.tcmService.updateTicket(ticket.id, result).subscribe({
          next: () => {
            this.loadTickets();
          },
          error: (err) => {
            console.error('Failed to update ticket', err);
          }
        });
      }
    });
  }

  getStatusClass(status: string): string {
    return status === 'OPEN' ? 'status-open' : 'status-closed';
  }

  isProjectManager(): boolean {
    return this.authService.hasRole('PROJECT_MANAGER') || this.authService.hasRole('ADMIN');
  }
}
