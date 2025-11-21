import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'stocks',
    loadComponent: () => import('./features/stocks/stock-list/stock-list.component').then(m => m.StockListComponent)
  },
  {
    path: 'stocks/create',
    loadComponent: () => import('./features/stocks/stock-form/stock-form.component').then(m => m.StockFormComponent)
  },
  {
    path: 'stocks/:id/edit',
    loadComponent: () => import('./features/stocks/stock-form/stock-form.component').then(m => m.StockFormComponent)
  },
  {
    path: 'etfs',
    loadComponent: () => import('./features/etfs/etf-list/etf-list.component').then(m => m.EtfListComponent)
  },
  {
    path: 'etfs/create',
    loadComponent: () => import('./features/etfs/etf-form/etf-form.component').then(m => m.EtfFormComponent)
  },
  {
    path: 'etfs/:id/edit',
    loadComponent: () => import('./features/etfs/etf-form/etf-form.component').then(m => m.EtfFormComponent)
  },
  {
    path: 'etfs/:id',
    loadComponent: () => import('./features/etfs/etf-details/etf-details.component').then(m => m.EtfDetailsComponent)
  },
  {
    path: 'portfolio',
    loadComponent: () => import('./features/portfolio/portfolio.component').then(m => m.PortfolioComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
