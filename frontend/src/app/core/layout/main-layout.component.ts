import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';
import { IconComponent } from '../../shared/components/icon.component';
import { KeycloakService } from '../services/keycloak.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    IconComponent,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule
  ],
  template: `
    <div class="layout-container">
      <mat-toolbar color="primary" class="toolbar">
        <button mat-icon-button (click)="toggleSidenav()">
          <mat-icon fontIcon="menu"></mat-icon>
        </button>
        <span class="title">Stock-Status</span>
        <span class="spacer"></span>

        <div class="user-menu">
          <span class="user-name-display">{{ getUserDisplayName() }}</span>
          <button mat-icon-button [matMenuTriggerFor]="userMenu" class="user-button">
            <mat-icon fontIcon="account_circle"></mat-icon>
          </button>
          <mat-menu #userMenu="matMenu">
            <button mat-menu-item (click)="logout()">
              <mat-icon fontIcon="logout"></mat-icon>
              <span>Abmelden</span>
            </button>
          </mat-menu>
        </div>
      </mat-toolbar>

      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav
          [opened]="sidenavOpened()"
          [mode]="sidenavMode()"
          class="sidenav">
          <mat-nav-list>
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
              <mat-icon matListItemIcon fontIcon="dashboard"></mat-icon>
              <span matListItemTitle>Dashboard</span>
            </a>
            <a mat-list-item routerLink="/stocks" routerLinkActive="active">
              <mat-icon matListItemIcon fontIcon="show_chart"></mat-icon>
              <span matListItemTitle>Aktien</span>
            </a>
            <a mat-list-item routerLink="/etfs" routerLinkActive="active">
              <mat-icon matListItemIcon fontIcon="assessment"></mat-icon>
              <span matListItemTitle>ETFs</span>
            </a>
            <a mat-list-item routerLink="/portfolio" routerLinkActive="active">
              <mat-icon matListItemIcon fontIcon="account_balance_wallet"></mat-icon>
              <span matListItemTitle>Portfolio</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>

        <mat-sidenav-content class="main-content">
          <div class="content-wrapper">
            <router-outlet></router-outlet>
          </div>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styles: [`
    .layout-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }

    .toolbar {
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .title {
      font-size: 1.5rem;
      font-weight: 500;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .user-menu {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .user-name-display {
      font-size: 1rem;
      font-weight: 400;
      color: rgba(255, 255, 255, 0.9);
    }

    .user-button {
      font-size: 1.5rem;
    }

    .user-button mat-icon {
      font-size: 2rem;
      width: 2rem;
      height: 2rem;
    }

    .sidenav-container {
      flex: 1;
      overflow: hidden;
    }

    .sidenav {
      width: 250px;
      padding-top: 1rem;
      background-color: #252525;
    }

    .main-content {
      background-color: #1e1e1e;
    }

    .content-wrapper {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .active {
      background-color: rgba(255, 255, 255, 0.1);
    }

    @media (max-width: 768px) {
      .content-wrapper {
        padding: 1rem;
      }
    }
  `]
})
export class MainLayoutComponent {
  keycloakService = inject(KeycloakService);
  sidenavOpened = signal(true);
  sidenavMode = signal<'side' | 'over'>('side');

  toggleSidenav() {
    this.sidenavOpened.update(opened => !opened);
  }

  getUserDisplayName(): string {
    return this.keycloakService.getFirstName() || this.keycloakService.getUsername() || 'Benutzer';
  }

  logout(): void {
    this.keycloakService.logout();
  }
}
