import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';;
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { from, of } from 'rxjs';
import { catchError, mergeMap, toArray } from 'rxjs/operators';

import { PortfolioStateService } from '../../core/services/portfolio-state.service';
import { StockApiService } from '../../core/services/stock-api.service';
import { EtfApiService } from '../../core/services/etf-api.service';
import { PortfolioPosition } from '../../models/portfolio.model';
import { AssetType } from '../../models/enums';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../shared/components/error-message.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/components/confirm-dialog.component';
import { IconComponent } from '../../shared/components/icon.component';
import { PositionFormComponent } from './position-form/position-form.component';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTableModule,
    MatTooltipModule,
    IconComponent,
    LoadingSpinnerComponent,
    ErrorMessageComponent
  ],
  template: `
    <div class="portfolio-container">
      <div class="header">
        <h1>Mein Portfolio</h1>
        <div class="header-actions">
          @if (hasPositionsWithoutLogo()) {
            <button mat-raised-button color="primary" (click)="refreshAllLogos()" [disabled]="refreshingLogos()">
              <mat-icon fontIcon="refresh" [class.spinning]="refreshingLogos()"></mat-icon>
              Logos aktualisieren
            </button>
          }
          <button mat-raised-button color="primary" (click)="addPosition()">
            <mat-icon fontIcon="add"></mat-icon>
            Position hinzufügen
          </button>
        </div>
      </div>

      @if (portfolioState.isLoading()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (portfolioState.error()) {
        <app-error-message
          [title]="'Fehler beim Laden des Portfolios'"
          [message]="portfolioState.error() || ''"
          [onRetry]="loadPortfolio.bind(this)">
        </app-error-message>
      } @else if (portfolioState.currentPortfolio()) {
        <mat-card class="portfolio-info-card">
          <mat-card-header>
            <mat-card-title>{{ portfolioState.currentPortfolio()?.name }}</mat-card-title>
            <mat-card-subtitle>
              {{ portfolioState.currentPortfolio()?.positions?.length || 0 }} Positionen
            </mat-card-subtitle>
          </mat-card-header>
        </mat-card>

        @if (portfolioState.currentPortfolio()?.positions && portfolioState.currentPortfolio()!.positions.length > 0) {
          <mat-card class="positions-card">
            <mat-card-header>
              <mat-card-title>Positionen</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="table-container">
                <table mat-table [dataSource]="sortedPositions()">
                  <ng-container matColumnDef="assetType">
                    <th mat-header-cell *matHeaderCellDef>Typ</th>
                    <td mat-cell *matCellDef="let position">
                      <span [class]="'asset-type-badge ' + position.assetType.toLowerCase()">
                        {{ position.assetType === 'STOCK' ? 'Aktie' : 'ETF' }}
                      </span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="logo">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let position">
                      @if (position.hasLogo) {
                        <img [src]="getLogoUrl(position)" alt="Logo" class="asset-logo-small" />
                      }
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="assetName">
                    <th mat-header-cell *matHeaderCellDef>Name</th>
                    <td mat-cell *matCellDef="let position">{{ position.assetName || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="assetIsin">
                    <th mat-header-cell *matHeaderCellDef>ISIN</th>
                    <td mat-cell *matCellDef="let position">{{ position.assetIsin || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="quantity">
                    <th mat-header-cell *matHeaderCellDef>Menge</th>
                    <td mat-cell *matCellDef="let position">{{ position.quantity | number:'1.0-4' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef>Aktionen</th>
                    <td mat-cell *matCellDef="let position">
                      <button mat-icon-button color="primary" (click)="editPosition(position)" matTooltip="Bearbeiten">
                        <mat-icon fontIcon="edit"></mat-icon>
                      </button>
                      <button mat-icon-button color="warn" (click)="deletePosition(position)" matTooltip="Löschen">
                        <mat-icon fontIcon="delete"></mat-icon>
                      </button>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
                </table>
              </div>
            </mat-card-content>
          </mat-card>
        } @else {
          <mat-card class="empty-state">
            <mat-card-content>
              <div class="empty-state-content">
                <mat-icon fontIcon="account_balance_wallet"></mat-icon>
                <h2>Keine Positionen vorhanden</h2>
                <p>Fügen Sie Ihre erste Position hinzu, um Ihr Portfolio zu starten.</p>
                <button mat-raised-button color="primary" (click)="addPosition()">
                  <mat-icon fontIcon="add"></mat-icon>
                  Position hinzufügen
                </button>
              </div>
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .portfolio-container {
      width: 100%;
      max-width: 1200px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    h1 {
      margin: 0;
      font-size: 2rem;
      font-weight: 500;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    .spinning {
      animation: spin 1s linear infinite;
    }

    mat-card {
      margin-bottom: 2rem;
    }

    .table-container {
      width: 100%;
      overflow-x: auto;
    }

    .mat-mdc-table {
      width: 100%;
    }

    .asset-type-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 500;
      text-transform: uppercase;
    }

    .asset-type-badge.stock {
      background-color: rgba(33, 150, 243, 0.2);
      color: #2196f3;
    }

    .asset-type-badge.etf {
      background-color: rgba(156, 39, 176, 0.2);
      color: #9c27b0;
    }

    .empty-state {
      margin-top: 2rem;
    }

    .empty-state-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 3rem 1rem;
      text-align: center;
    }

    .empty-state-content mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: rgba(255, 255, 255, 0.5);
    }

    .empty-state-content h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 400;
    }

    .empty-state-content p {
      margin: 0;
      color: rgba(255, 255, 255, 0.7);
    }

    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
      }

      .header button {
        width: 100%;
      }
    }

    .asset-logo-small {
      max-width: 24px;
      max-height: 24px;
      object-fit: contain;
      display: block;
    }

    :host ::ng-deep .mat-mdc-table .mat-column-logo {
      width: 32px;
      padding-right: 4px !important;
    }
  `]
})
export class PortfolioComponent implements OnInit {
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  portfolioState = inject(PortfolioStateService);
  private stockApi = inject(StockApiService);
  private etfApi = inject(EtfApiService);

  // Hardcoded portfolio ID as per backend setup
  private readonly PORTFOLIO_ID = 1;
  refreshingLogos = signal(false);

  displayedColumns = ['assetType', 'logo', 'assetName', 'assetIsin', 'quantity', 'actions'];

  getLogoUrl(position: PortfolioPosition): string {
    if (position.assetType === AssetType.STOCK) {
      return `/api/stocks/${position.assetId}/logo`;
    } else {
      return `/api/etfs/${position.assetId}/logo`;
    }
  }

  hasPositionsWithoutLogo(): boolean {
    const positions = this.portfolioState.currentPortfolio()?.positions || [];
    return positions.some(position => !position.hasLogo);
  }

  sortedPositions = computed(() => {
    const positions = this.portfolioState.currentPortfolio()?.positions || [];
    return [...positions].sort((a, b) => {
      // First sort by assetType (ETF before STOCK)
      // ETF comes before STOCK alphabetically (E before S)
      const typeComparison = a.assetType.localeCompare(b.assetType);
      if (typeComparison !== 0) {
        return typeComparison;
      }
      // Then sort by assetName (ascending)
      const nameA = a.assetName || '';
      const nameB = b.assetName || '';
      return nameA.localeCompare(nameB);
    });
  });

  ngOnInit(): void {
    this.loadPortfolio();
  }

  loadPortfolio(): void {
    this.portfolioState.loadPortfolioById(this.PORTFOLIO_ID).subscribe({
      error: () => {
        this.snackBar.open('Fehler beim Laden des Portfolios', 'OK', { duration: 3000 });
      }
    });
  }

  addPosition(): void {
    const dialogRef = this.dialog.open(PositionFormComponent, {
      width: '600px',
      data: { portfolioId: this.PORTFOLIO_ID }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.portfolioState.addPosition(this.PORTFOLIO_ID, result).subscribe({
          next: () => {
            this.snackBar.open('Position erfolgreich hinzugefügt', 'OK', { duration: 3000 });
            this.loadPortfolio();
          },
          error: () => {
            this.snackBar.open('Fehler beim Hinzufügen der Position', 'OK', { duration: 3000 });
          }
        });
      }
    });
  }

  editPosition(position: PortfolioPosition): void {
    const dialogRef = this.dialog.open(PositionFormComponent, {
      width: '600px',
      data: {
        portfolioId: this.PORTFOLIO_ID,
        position: position
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.portfolioState.updatePosition(position.id, result).subscribe({
          next: () => {
            this.snackBar.open('Position erfolgreich aktualisiert', 'OK', { duration: 3000 });
            this.loadPortfolio();
          },
          error: () => {
            this.snackBar.open('Fehler beim Aktualisieren der Position', 'OK', { duration: 3000 });
          }
        });
      }
    });
  }

  deletePosition(position: PortfolioPosition): void {
    const assetTypeLabel = position.assetType === AssetType.STOCK ? 'Aktie' : 'ETF';
    const dialogData: ConfirmDialogData = {
      title: 'Position löschen',
      message: `Möchten Sie die Position "${position.assetName}" (${assetTypeLabel}, ${position.quantity} Stück) wirklich löschen?`,
      confirmText: 'Löschen',
      cancelText: 'Abbrechen',
      confirmColor: 'warn'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.portfolioState.deletePosition(position.id).subscribe({
          next: () => {
            this.snackBar.open('Position erfolgreich gelöscht', 'OK', { duration: 3000 });
            this.loadPortfolio();
          },
          error: () => {
            this.snackBar.open('Fehler beim Löschen der Position', 'OK', { duration: 3000 });
          }
        });
      }
    });
  }

  refreshAllLogos(): void {
    const positions = this.portfolioState.currentPortfolio()?.positions || [];
    const positionsWithoutLogo = positions.filter(position => !position.hasLogo);

    if (positionsWithoutLogo.length === 0) {
      return;
    }

    this.refreshingLogos.set(true);

    from(positionsWithoutLogo).pipe(
      mergeMap(position => {
        if (position.assetType === AssetType.STOCK) {
          return this.stockApi.fetchLogoFromElbstream(position.assetId).pipe(
            catchError(error => {
              console.error(`Failed to fetch logo for stock ${position.assetId}:`, error);
              return of(null);
            })
          );
        } else {
          return this.etfApi.fetchLogoFromElbstream(position.assetId).pipe(
            catchError(error => {
              console.error(`Failed to fetch logo for ETF ${position.assetId}:`, error);
              return of(null);
            })
          );
        }
      }, 10), // Maximum 10 concurrent requests
      toArray()
    ).subscribe({
      next: () => {
        this.refreshingLogos.set(false);
        this.loadPortfolio();
        this.snackBar.open(`${positionsWithoutLogo.length} Logo(s) wurden aktualisiert`, 'OK', { duration: 3000 });
      },
      error: () => {
        this.refreshingLogos.set(false);
        this.snackBar.open('Fehler beim Aktualisieren der Logos', 'OK', { duration: 3000 });
      }
    });
  }
}
