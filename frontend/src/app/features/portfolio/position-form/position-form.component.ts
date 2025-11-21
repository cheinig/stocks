import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

import { AssetType } from '../../../models/enums';
import { PortfolioPosition, PortfolioPositionRequest } from '../../../models/portfolio.model';
import { Stock } from '../../../models/stock.model';
import { ETF } from '../../../models/etf.model';
import { StockApiService } from '../../../core/services/stock-api.service';
import { EtfApiService } from '../../../core/services/etf-api.service';

export interface PositionFormData {
  portfolioId: number;
  position?: PortfolioPosition;
}

@Component({
  selector: 'app-position-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatAutocompleteModule
  ],
  template: `
    <div class="position-form-dialog">
      <h2 mat-dialog-title>{{ isEditMode() ? 'Position bearbeiten' : 'Position hinzufügen' }}</h2>

      <form [formGroup]="positionForm" (ngSubmit)="onSubmit()">
        <div class="form-content">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Asset-Typ</mat-label>
            <mat-select formControlName="assetType" (selectionChange)="onAssetTypeChange()">
              <mat-option value="STOCK">Aktie</mat-option>
              <mat-option value="ETF">ETF</mat-option>
            </mat-select>
            @if (positionForm.get('assetType')?.hasError('required') && positionForm.get('assetType')?.touched) {
              <mat-error>Asset-Typ ist erforderlich</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ assetTypeLabel() }}</mat-label>
            <input
              matInput
              formControlName="assetSearch"
              [matAutocomplete]="auto"
              placeholder="Name oder ISIN suchen">
            <mat-autocomplete
              #auto="matAutocomplete"
              [displayWith]="displayAsset"
              (optionSelected)="onAssetSelected($event.option.value)">
              @for (asset of filteredAssets(); track asset.id) {
                <mat-option [value]="asset">
                  {{ asset.name }} ({{ asset.isin }})
                </mat-option>
              }
            </mat-autocomplete>
            @if (positionForm.get('assetId')?.hasError('required') && positionForm.get('assetSearch')?.touched) {
              <mat-error>Bitte wählen Sie ein Asset aus</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Menge</mat-label>
            <input matInput type="number" formControlName="quantity" placeholder="z.B. 10" step="0.0001">
            @if (positionForm.get('quantity')?.hasError('required') && positionForm.get('quantity')?.touched) {
              <mat-error>Menge ist erforderlich</mat-error>
            }
            @if (positionForm.get('quantity')?.hasError('min') && positionForm.get('quantity')?.touched) {
              <mat-error>Menge muss größer als 0 sein</mat-error>
            }
          </mat-form-field>
        </div>

        <div class="dialog-actions">
          <button type="button" mat-button (click)="cancel()">
            Abbrechen
          </button>
          <button type="submit" mat-raised-button color="primary" [disabled]="positionForm.invalid || saving()">
            @if (saving()) {
              Speichert...
            } @else {
              {{ isEditMode() ? 'Aktualisieren' : 'Hinzufügen' }}
            }
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .position-form-dialog {
      padding: 1rem;
    }

    h2 {
      margin: 0 0 1.5rem 0;
      font-size: 1.5rem;
      font-weight: 500;
    }

    .form-content {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-bottom: 1.5rem;
      min-width: 400px;
    }

    .full-width {
      width: 100%;
    }

    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    @media (max-width: 600px) {
      .form-content {
        min-width: 300px;
      }

      .dialog-actions {
        flex-direction: column-reverse;
      }

      .dialog-actions button {
        width: 100%;
      }
    }
  `]
})
export class PositionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<PositionFormComponent>);
  private data = inject<PositionFormData>(MAT_DIALOG_DATA);
  private stockApi = inject(StockApiService);
  private etfApi = inject(EtfApiService);

  positionForm: FormGroup;
  isEditMode = signal(false);
  saving = signal(false);
  filteredAssets = signal<(Stock | ETF)[]>([]);
  selectedAsset = signal<Stock | ETF | null>(null);

  assetTypeLabel = signal('Asset');

  constructor() {
    this.positionForm = this.fb.group({
      assetType: ['', Validators.required],
      assetSearch: [''],
      assetId: [null, Validators.required],
      quantity: [null, [Validators.required, Validators.min(0.0001)]]
    });
  }

  ngOnInit(): void {
    if (this.data.position) {
      this.isEditMode.set(true);
      this.positionForm.patchValue({
        assetType: this.data.position.assetType,
        assetId: this.data.position.assetId,
        quantity: this.data.position.quantity
      });

      // For edit mode, pre-populate the asset search field
      if (this.data.position.assetName) {
        this.positionForm.patchValue({
          assetSearch: `${this.data.position.assetName} (${this.data.position.assetIsin})`
        });
      }

      // Disable asset type change in edit mode
      this.positionForm.get('assetType')?.disable();
      this.positionForm.get('assetSearch')?.disable();
    }

    // Setup autocomplete
    this.positionForm.get('assetSearch')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(value => {
          if (typeof value === 'string' && value.length >= 2) {
            return this.searchAssets(value);
          }
          return of([]);
        })
      )
      .subscribe(assets => {
        this.filteredAssets.set(assets);
      });

    // Update label when asset type changes
    this.updateAssetTypeLabel();
  }

  onAssetTypeChange(): void {
    this.positionForm.patchValue({
      assetSearch: '',
      assetId: null
    });
    this.filteredAssets.set([]);
    this.selectedAsset.set(null);
    this.updateAssetTypeLabel();
  }

  updateAssetTypeLabel(): void {
    const assetType = this.positionForm.get('assetType')?.value;
    this.assetTypeLabel.set(assetType === AssetType.STOCK ? 'Aktie' : assetType === AssetType.ETF ? 'ETF' : 'Asset');
  }

  searchAssets(query: string): Observable<(Stock | ETF)[]> {
    const assetType = this.positionForm.get('assetType')?.value;

    if (assetType === AssetType.STOCK) {
      return this.stockApi.search(query).pipe(
        switchMap(page => of(page.content))
      );
    } else if (assetType === AssetType.ETF) {
      // For ETF, we load all and filter client-side
      // This is acceptable for a limited number of ETFs
      return this.etfApi.findAll({ page: 0, size: 100 }).pipe(
        switchMap(page => {
          const filtered = page.content.filter(etf =>
            etf.name.toLowerCase().includes(query.toLowerCase()) ||
            etf.isin.toLowerCase().includes(query.toLowerCase())
          );
          return of(filtered);
        })
      );
    }

    return of([]);
  }

  displayAsset = (asset: Stock | ETF | null): string => {
    return asset ? `${asset.name} (${asset.isin})` : '';
  };

  onAssetSelected(asset: Stock | ETF): void {
    this.selectedAsset.set(asset);
    this.positionForm.patchValue({
      assetId: asset.id
    });
  }

  onSubmit(): void {
    if (this.positionForm.invalid) {
      this.positionForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);

    const assetType = this.isEditMode()
      ? this.data.position!.assetType
      : this.positionForm.get('assetType')?.value;

    const request: PortfolioPositionRequest = {
      assetType: assetType,
      assetId: this.positionForm.get('assetId')?.value,
      quantity: this.positionForm.get('quantity')?.value
    };

    this.dialogRef.close(request);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
