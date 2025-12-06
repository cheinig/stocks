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

import { EtfStateService } from '../../../core/services/etf-state.service';
import { ETF } from '../../../models/etf.model';
import { DataTableComponent, TableColumn, TableAction } from '../../../shared/components/data-table.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog.component';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-etf-list',
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
    ErrorMessageComponent
  ],
  template: `
    <div class="etf-list-container">
      <div class="header">
        <h1>ETFs</h1>
        <button mat-raised-button color="primary" (click)="createEtf()">
          <mat-icon fontIcon="add"></mat-icon>
          Neuer ETF
        </button>
      </div>

      <div class="search-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Suche nach Name oder ISIN</mat-label>
          <input matInput [formControl]="searchControl" placeholder="z.B. MSCI World oder IE00B4L5Y983">
          <mat-icon matPrefix fontIcon="search"></mat-icon>
          @if (searchControl.value) {
            <button matSuffix mat-icon-button (click)="clearSearch()">
              <mat-icon fontIcon="close"></mat-icon>
            </button>
          }
        </mat-form-field>
      </div>

      @if (etfState.isLoading()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (etfState.error()) {
        <app-error-message
          [title]="'Fehler beim Laden der ETFs'"
          [message]="etfState.error() || ''"
          [onRetry]="loadEtfs.bind(this)">
        </app-error-message>
      } @else {
        <app-data-table
          [data]="etfState.etfs()"
          [columns]="columns"
          [actions]="actions"
          [totalElements]="etfState.totalElements()"
          [pageSize]="pageSize()"
          [pageIndex]="pageIndex()"
          (pageChange)="onPageChange($event)"
          (sortChange)="onSortChange($event)"
          (rowClick)="viewEtfDetails($event)">
        </app-data-table>
      }
    </div>
  `,
  styles: [`
    .etf-list-container {
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

    .search-bar {
      margin-bottom: 1.5rem;
    }

    .search-field {
      width: 100%;
      max-width: 600px;
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

      .search-field {
        max-width: 100%;
      }
    }
  `]
})
export class EtfListComponent implements OnInit {
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  etfState = inject(EtfStateService);

  searchControl = new FormControl('');
  pageSize = signal(20);
  pageIndex = signal(0);
  sortField = signal<string>('name');
  sortDirection = signal<'asc' | 'desc'>('asc');

  columns: TableColumn[] = [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'isin', label: 'ISIN', sortable: true },
    { key: 'importerType', label: 'Importer-Typ', sortable: true }
  ];

  actions: TableAction<ETF>[] = [
    {
      icon: 'edit',
      tooltip: 'Bearbeiten',
      color: 'primary',
      action: (etf) => this.editEtf(etf)
    },
    {
      icon: 'delete',
      tooltip: 'Löschen',
      color: 'warn',
      action: (etf) => this.deleteEtf(etf)
    }
  ];

  ngOnInit(): void {
    this.loadEtfs();
    this.setupSearch();
  }

  setupSearch(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.pageIndex.set(0);
        this.loadEtfs();
      });
  }

  loadEtfs(): void {
    const sort = `${this.sortField()},${this.sortDirection()}`;

    this.etfState.loadEtfs({
      page: this.pageIndex(),
      size: this.pageSize(),
      sort
    }).subscribe();
  }

  createEtf(): void {
    this.router.navigate(['/etfs/create']);
  }

  editEtf(etf: ETF): void {
    this.router.navigate(['/etfs', etf.id, 'edit']);
  }

  viewEtfDetails(etf: ETF): void {
    this.router.navigate(['/etfs', etf.id]);
  }

  deleteEtf(etf: ETF): void {
    const dialogData: ConfirmDialogData = {
      title: 'ETF löschen',
      message: `Möchten Sie den ETF "${etf.name}" (${etf.isin}) wirklich löschen? Alle zugehörigen Allocations werden ebenfalls gelöscht.`,
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
        this.etfState.deleteEtf(etf.id).subscribe({
          next: () => {
            this.snackBar.open('ETF erfolgreich gelöscht', 'OK', { duration: 3000 });
            this.loadEtfs();
          },
          error: () => {
            this.snackBar.open('Fehler beim Löschen des ETFs', 'OK', { duration: 3000 });
          }
        });
      }
    });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadEtfs();
  }

  onSortChange(event: Sort): void {
    this.sortField.set(event.active);
    this.sortDirection.set(event.direction as 'asc' | 'desc');
    this.loadEtfs();
  }
}
