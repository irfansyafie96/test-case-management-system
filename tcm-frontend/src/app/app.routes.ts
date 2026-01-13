import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Default redirect to dashboard
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // Main dashboard
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },

  // Authentication routes
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register-org',
    loadComponent: () => import('./features/auth/register-org/register-org.component').then(m => m.RegisterOrgComponent)
  },
  {
    path: 'join',
    loadComponent: () => import('./features/auth/join/join.component').then(m => m.JoinComponent)
  },

  // Project routes
  {
    path: 'projects',
    loadComponent: () => import('./features/projects/projects/projects.component').then(m => m.ProjectsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/projects/project-detail/project-detail.component').then(m => m.ProjectDetailComponent),
    canActivate: [authGuard]
  },

  // Module routes
  {
    path: 'modules',
    loadComponent: () => import('./features/modules/modules/modules.component').then(m => m.ModulesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'modules/:id',
    loadComponent: () => import('./features/modules/module-detail/module-detail.component').then(m => m.ModuleDetailComponent),
    canActivate: [authGuard]
  },

  // Test cases routes
  {
    path: 'test-cases',
    loadComponent: () => import('./features/test-cases/test-cases/test-cases.component').then(m => m.TestCasesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'test-cases/:id',
    loadComponent: () => import('./features/test-cases/test-case-detail/test-case-detail.component').then(m => m.TestCaseDetailComponent),
    canActivate: [authGuard]
  },

  // Executions routes
  {
    path: 'executions',
    loadComponent: () => import('./features/executions/executions/executions.component').then(m => m.ExecutionsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'executions/workbench/:id',
    loadComponent: () => import('./features/executions/execution-workbench/execution-workbench.component').then(m => m.ExecutionWorkbenchComponent),
    canActivate: [authGuard]
  },

  // Team routes
  {
    path: 'team',
    loadComponent: () => import('./features/team/team/team.component').then(m => m.TeamComponent),
    canActivate: [authGuard]
  },

  // Wildcard route for 404
  { path: '**', redirectTo: '/dashboard' }
];
