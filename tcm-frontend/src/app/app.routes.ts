import { Routes } from '@angular/router';

export const routes: Routes = [
  // Default redirect to dashboard
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // Main dashboard
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },

  // Authentication routes
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },

  // Project routes
  {
    path: 'projects',
    loadComponent: () => import('./features/projects/projects/projects.component').then(m => m.ProjectsComponent)
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/projects/project-detail/project-detail.component').then(m => m.ProjectDetailComponent)
  },

  // Module routes
  {
    path: 'modules',
    loadComponent: () => import('./features/modules/modules/modules.component').then(m => m.ModulesComponent)
  },
  {
    path: 'modules/:id',
    loadComponent: () => import('./features/modules/module-detail/module-detail.component').then(m => m.ModuleDetailComponent)
  },

  // Test cases routes
  {
    path: 'test-cases',
    loadComponent: () => import('./features/test-cases/test-cases/test-cases.component').then(m => m.TestCasesComponent)
  },
  {
    path: 'test-cases/:id',
    loadComponent: () => import('./features/test-cases/test-case-detail/test-case-detail.component').then(m => m.TestCaseDetailComponent)
  },

  // Executions routes
  {
    path: 'executions',
    loadComponent: () => import('./features/executions/executions/executions.component').then(m => m.ExecutionsComponent)
  },

  // Wildcard route for 404
  { path: '**', redirectTo: '/dashboard' }
];
