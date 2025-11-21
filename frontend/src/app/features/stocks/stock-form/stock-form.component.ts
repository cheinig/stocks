import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { StockStateService } from '../../../core/services/stock-state.service';
import { StockRequest } from '../../../models/stock.model';
import { isinValidator } from '../../../shared/validators/isin.validator';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-stock-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    IconComponent,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,
    MatSnackBarModule,
    LoadingSpinnerComponent,
    ErrorMessageComponent
  ],
  template: `
    <div class="stock-form-container">
      <div class="header">
        <button mat-icon-button (click)="cancel()">
          <mat-icon fontIcon="arrow_back"></mat-icon>
        </button>
        <h1>{{ isEditMode() ? 'Aktie bearbeiten' : 'Neue Aktie erstellen' }}</h1>
      </div>

      @if (stockState.isLoading() && isEditMode()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (stockState.error() && isEditMode()) {
        <app-error-message
          [title]="'Fehler beim Laden der Aktie'"
          [message]="stockState.error() || ''"
          [onRetry]="loadStock.bind(this)">
        </app-error-message>
      } @else {
        <mat-card>
          <mat-card-content>
            <form [formGroup]="stockForm" (ngSubmit)="onSubmit()">
              <div class="form-grid">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Name</mat-label>
                  <input matInput formControlName="name" placeholder="z.B. Apple Inc.">
                  @if (stockForm.get('name')?.hasError('required') && stockForm.get('name')?.touched) {
                    <mat-error>Name ist erforderlich</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>ISIN</mat-label>
                  <input matInput formControlName="isin" placeholder="z.B. US0378331005">
                  @if (stockForm.get('isin')?.hasError('required') && stockForm.get('isin')?.touched) {
                    <mat-error>ISIN ist erforderlich</mat-error>
                  }
                  @if (stockForm.get('isin')?.hasError('isinFormat') && stockForm.get('isin')?.touched) {
                    <mat-error>Ungültiges ISIN-Format (2 Buchstaben + 9 Zeichen + 1 Prüfziffer)</mat-error>
                  }
                  @if (stockForm.get('isin')?.hasError('isinCheckDigit') && stockForm.get('isin')?.touched) {
                    <mat-error>Ungültige ISIN-Prüfziffer</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Land</mat-label>
                  <mat-select formControlName="country">
                    @for (country of countries; track country.code) {
                      <mat-option [value]="country.code">
                        {{ country.name }}
                      </mat-option>
                    }
                  </mat-select>
                  @if (stockForm.get('country')?.hasError('required') && stockForm.get('country')?.touched) {
                    <mat-error>Land ist erforderlich</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Branche</mat-label>
                  <mat-select formControlName="sector">
                    @for (sector of sectors; track sector) {
                      <mat-option [value]="sector">
                        {{ sector }}
                      </mat-option>
                    }
                  </mat-select>
                  @if (stockForm.get('sector')?.hasError('required') && stockForm.get('sector')?.touched) {
                    <mat-error>Branche ist erforderlich</mat-error>
                  }
                </mat-form-field>
              </div>

              <div class="form-actions">
                <button type="button" mat-button (click)="cancel()">
                  Abbrechen
                </button>
                <button type="submit" mat-raised-button color="primary" [disabled]="stockForm.invalid || saving()">
                  @if (saving()) {
                    Speichert...
                  } @else {
                    Speichern
                  }
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .stock-form-container {
      width: 100%;
      max-width: 800px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
    }

    h1 {
      margin: 0;
      font-size: 2rem;
      font-weight: 500;
    }

    mat-card {
      padding: 1rem;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .full-width {
      grid-column: 1 / -1;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
    }

    .form-actions button mat-icon {
      margin-right: 0.5rem;
    }

    @media (max-width: 768px) {
      .form-grid {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column-reverse;
      }

      .form-actions button {
        width: 100%;
      }
    }
  `]
})
export class StockFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  stockState = inject(StockStateService);

  isEditMode = signal(false);
  saving = signal(false);
  stockId?: number;

  stockForm: FormGroup;

  countries = [
    { code: 'US', name: 'USA' },
    { code: 'DE', name: 'Deutschland' },
    { code: 'GB', name: 'Großbritannien' },
    { code: 'FR', name: 'Frankreich' },
    { code: 'IT', name: 'Italien' },
    { code: 'ES', name: 'Spanien' },
    { code: 'NL', name: 'Niederlande' },
    { code: 'BE', name: 'Belgien' },
    { code: 'CH', name: 'Schweiz' },
    { code: 'AT', name: 'Österreich' },
    { code: 'SE', name: 'Schweden' },
    { code: 'NO', name: 'Norwegen' },
    { code: 'DK', name: 'Dänemark' },
    { code: 'FI', name: 'Finnland' },
    { code: 'IE', name: 'Irland' },
    { code: 'JP', name: 'Japan' },
    { code: 'CN', name: 'China' },
    { code: 'KR', name: 'Südkorea' },
    { code: 'IN', name: 'Indien' },
    { code: 'AU', name: 'Australien' },
    { code: 'CA', name: 'Kanada' },
    { code: 'BR', name: 'Brasilien' }
  ];

  sectors = [
    'Technology',
    'Healthcare',
    'Financials',
    'Consumer Discretionary',
    'Consumer Staples',
    'Industrials',
    'Energy',
    'Materials',
    'Real Estate',
    'Utilities',
    'Communication Services'
  ];

  constructor() {
    this.stockForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      isin: ['', [Validators.required, isinValidator()]],
      country: ['', Validators.required],
      sector: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.stockId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadStock();
    }
  }

  loadStock(): void {
    if (!this.stockId) return;

    this.stockState.loadStockById(this.stockId).subscribe({
      next: () => {
        const stock = this.stockState.currentStock();
        if (stock) {
          this.stockForm.patchValue({
            name: stock.name,
            isin: stock.isin,
            country: stock.country,
            sector: stock.sector
          });
        }
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Aktie', 'OK', { duration: 3000 });
      }
    });
  }

  onSubmit(): void {
    if (this.stockForm.invalid) {
      this.stockForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const request: StockRequest = this.stockForm.value;

    const operation = this.isEditMode() && this.stockId
      ? this.stockState.updateStock(this.stockId, request)
      : this.stockState.createStock(request);

    operation.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Aktie erfolgreich aktualisiert'
          : 'Aktie erfolgreich erstellt';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.router.navigate(['/stocks']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Fehler beim Aktualisieren der Aktie'
          : 'Fehler beim Erstellen der Aktie';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/stocks']);
  }
}
