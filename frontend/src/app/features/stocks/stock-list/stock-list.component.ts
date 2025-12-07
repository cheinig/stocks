import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { StockStateService } from '../../../core/services/stock-state.service';
import { Stock } from '../../../models/stock.model';
import { DataTableComponent, TableColumn, TableAction } from '../../../shared/components/data-table.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog.component';
import { CountryNamePipe } from '../../../shared/pipes/country-name.pipe';
import { SectorNamePipe } from '../../../shared/pipes/sector-name.pipe';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-stock-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    IconComponent,
    MatInputModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatDialogModule,
    DataTableComponent,
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    CountryNamePipe,
    SectorNamePipe
  ],
  template: `
    <div class="stock-list-container">
      <div class="header">
        <h1>Aktien</h1>
        <button mat-raised-button color="primary" (click)="createStock()">
          <mat-icon fontIcon="add"></mat-icon>
          Neue Aktie
        </button>
      </div>

      <div class="search-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Suche nach Name, ISIN, Sektor oder Land</mat-label>
          <input matInput [formControl]="searchControl" placeholder="z.B. Apple, US0378331005, Technology oder DE">
          <mat-icon matPrefix fontIcon="search"></mat-icon>
          @if (searchControl.value) {
            <button matSuffix mat-icon-button (click)="clearSearch()">
              <mat-icon fontIcon="close"></mat-icon>
            </button>
          }
        </mat-form-field>
      </div>

      @if (stockState.isLoading()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (stockState.error()) {
        <app-error-message
          [title]="'Fehler beim Laden der Aktien'"
          [message]="stockState.error() || ''"
          [onRetry]="loadStocks.bind(this)">
        </app-error-message>
      } @else {
        <app-data-table
          [data]="stockState.stocks()"
          [columns]="columns"
          [actions]="actions"
          [totalElements]="stockState.totalElements()"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          (pageChange)="onPageChange($event)"
          (sortChange)="onSortChange($event)"
          (rowClick)="viewStock($event)">
          <ng-template #cellTemplate let-row let-column="column">
            @if (column === 'logo') {
              @if (row.hasLogo) {
                <img [src]="getLogoUrl(row.id)" alt="Logo" class="stock-logo-small" />
              }
            } @else if (column === 'country') {
              {{ row.country | countryName }}
            } @else if (column === 'sector') {
              {{ row.sector | sectorName }}
            } @else {
              {{ row[column] }}
            }
          </ng-template>
        </app-data-table>
      }
    </div>
  `,
  styles: [`
    .stock-list-container {
      width: 100%;
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

    .header button mat-icon {
      margin-right: 0.5rem;
    }

    .search-bar {
      margin-bottom: 1.5rem;
    }

    .search-field {
      width: 100%;
      max-width: 500px;
    }

    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
      }

      .search-field {
        max-width: 100%;
      }
    }

    .stock-logo-small {
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
export class StockListComponent implements OnInit {
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  stockState = inject(StockStateService);

  getLogoUrl(stockId: number): string {
    return `/api/stocks/${stockId}/logo`;
  }

  searchControl = new FormControl('');
  pageSize = signal(20);
  pageIndex = signal(0);
  sortField = signal<string>('name');
  sortDirection = signal<'asc' | 'desc'>('asc');

  columns: TableColumn[] = [
    { key: 'logo', label: '', sortable: false },
    { key: 'name', label: 'Name', sortable: true },
    { key: 'isin', label: 'ISIN', sortable: true },
    { key: 'country', label: 'Land', sortable: true },
    { key: 'sector', label: 'Branche', sortable: true }
  ];

  actions: TableAction<Stock>[] = [
    {
      icon: 'edit',
      tooltip: 'Bearbeiten',
      color: 'primary',
      action: (stock: Stock) => this.editStock(stock)
    },
    {
      icon: 'delete',
      tooltip: 'Löschen',
      color: 'warn',
      action: (stock: Stock) => this.deleteStock(stock)
    }
  ];

  ngOnInit(): void {
    this.loadStocks();
    this.setupSearch();
  }

  setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(query => {
      this.pageIndex.set(0);
      if (query && query.trim()) {
        this.searchStocks(query.trim());
      } else {
        this.loadStocks();
      }
    });
  }

  loadStocks(): void {
    const sort = `${this.sortField()},${this.sortDirection()}`;
    this.stockState.loadStocks({
      page: this.pageIndex(),
      size: this.pageSize(),
      sort
    }).subscribe();
  }

  searchStocks(query: string): void {
    const sort = `${this.sortField()},${this.sortDirection()}`;
    this.stockState.searchStocks(query, {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort
    }).subscribe();
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  onPageChange(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.pageIndex.set(event.pageIndex);

    const query = this.searchControl.value?.trim();
    if (query) {
      this.searchStocks(query);
    } else {
      this.loadStocks();
    }
  }

  onSortChange(event: Sort): void {
    if (event.direction) {
      this.sortField.set(event.active);
      this.sortDirection.set(event.direction);

      const query = this.searchControl.value?.trim();
      if (query) {
        this.searchStocks(query);
      } else {
        this.loadStocks();
      }
    }
  }

  createStock(): void {
    this.router.navigate(['/stocks/create']);
  }

  viewStock(stock: Stock): void {
    this.router.navigate(['/stocks', stock.id]);
  }

  editStock(stock: Stock): void {
    this.router.navigate(['/stocks', stock.id, 'edit']);
  }

  deleteStock(stock: Stock): void {
    const dialogData: ConfirmDialogData = {
      title: 'Aktie löschen',
      message: `Möchten Sie die Aktie "${stock.name}" (${stock.isin}) wirklich löschen?`,
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
        this.stockState.deleteStock(stock.id).subscribe({
          next: () => {
            this.snackBar.open('Aktie erfolgreich gelöscht', 'OK', { duration: 3000 });
            this.loadStocks();
          },
          error: () => {
            this.snackBar.open('Fehler beim Löschen der Aktie', 'OK', { duration: 3000 });
          }
        });
      }
    });
  }
}
