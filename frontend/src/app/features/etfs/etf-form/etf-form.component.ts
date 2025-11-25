import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { EtfStateService } from '../../../core/services/etf-state.service';
import { ETFRequest, ImporterType } from '../../../models/etf.model';
import { isinValidator } from '../../../shared/validators/isin.validator';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-etf-form',
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
    <div class="etf-form-container">
      <div class="header">
        <button mat-icon-button (click)="cancel()">
          <mat-icon fontIcon="arrow_back"></mat-icon>
        </button>
        <h1>{{ isEditMode() ? 'ETF bearbeiten' : 'Neuen ETF erstellen' }}</h1>
      </div>

      @if (etfState.isLoading() && isEditMode()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (etfState.error() && isEditMode()) {
        <app-error-message
          [title]="'Fehler beim Laden des ETFs'"
          [message]="etfState.error() || ''"
          [onRetry]="loadEtf.bind(this)">
        </app-error-message>
      } @else {
        <mat-card>
          <mat-card-content>
            <form [formGroup]="etfForm" (ngSubmit)="onSubmit()">
              <div class="form-grid">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Name</mat-label>
                  <input matInput formControlName="name" placeholder="z.B. MSCI World">
                  @if (etfForm.get('name')?.hasError('required') && etfForm.get('name')?.touched) {
                    <mat-error>Name ist erforderlich</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>ISIN</mat-label>
                  <input matInput formControlName="isin" placeholder="z.B. IE00B4L5Y983">
                  @if (etfForm.get('isin')?.hasError('required') && etfForm.get('isin')?.touched) {
                    <mat-error>ISIN ist erforderlich</mat-error>
                  }
                  @if (etfForm.get('isin')?.hasError('isinFormat') && etfForm.get('isin')?.touched) {
                    <mat-error>Ungültiges ISIN-Format (2 Buchstaben + 9 alphanumerische Zeichen + 1 Ziffer)</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Importer-Typ</mat-label>
                  <mat-select formControlName="importerType">
                    @for (type of importerTypes; track type.value) {
                      <mat-option [value]="type.value">
                        {{ type.label }}
                      </mat-option>
                    }
                  </mat-select>
                  @if (etfForm.get('importerType')?.hasError('required') && etfForm.get('importerType')?.touched) {
                    <mat-error>Importer-Typ ist erforderlich</mat-error>
                  }
                  <mat-hint>Wählen Sie das Format für den Import der Allocation-Daten</mat-hint>
                </mat-form-field>
              </div>

              <div class="form-actions">
                <button type="button" mat-button (click)="cancel()">
                  Abbrechen
                </button>
                <button type="submit" mat-raised-button color="primary" [disabled]="etfForm.invalid || saving()">
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
    .etf-form-container {
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
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .full-width {
      width: 100%;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
    }

    @media (max-width: 768px) {
      .form-actions {
        flex-direction: column-reverse;
      }

      .form-actions button {
        width: 100%;
      }
    }
  `]
})
export class EtfFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  etfState = inject(EtfStateService);

  isEditMode = signal(false);
  saving = signal(false);
  etfId?: number;

  etfForm: FormGroup;

  importerTypes = [
    { value: ImporterType.GENERIC_CSV, label: 'Generic CSV' },
    { value: ImporterType.GENERIC_EXCEL, label: 'Generic Excel' },
    { value: ImporterType.ISHARES_CSV, label: 'iShares CSV' },
    { value: ImporterType.VANGUARD_CSV, label: 'Vanguard CSV' },
    { value: ImporterType.SPDR_CSV, label: 'SPDR CSV' },
    { value: ImporterType.FIDELITY, label: 'Fidelity Excel' },
    { value: ImporterType.XTRACKERS, label: 'Xtrackers Excel' },
    { value: ImporterType.VANECK, label: 'VanEck Excel' }
  ];

  constructor() {
    this.etfForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      isin: ['', [Validators.required, isinValidator()]],
      importerType: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.etfId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadEtf();
    }
  }

  loadEtf(): void {
    if (!this.etfId) return;

    this.etfState.loadEtfById(this.etfId).subscribe({
      next: () => {
        const etf = this.etfState.currentEtf();
        if (etf) {
          this.etfForm.patchValue({
            name: etf.name,
            isin: etf.isin,
            importerType: etf.importerType
          });
        }
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden des ETFs', 'OK', { duration: 3000 });
      }
    });
  }

  onSubmit(): void {
    if (this.etfForm.invalid) {
      this.etfForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const request: ETFRequest = this.etfForm.value;

    const operation = this.isEditMode() && this.etfId
      ? this.etfState.updateEtf(this.etfId, request)
      : this.etfState.createEtf(request);

    operation.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'ETF erfolgreich aktualisiert'
          : 'ETF erfolgreich erstellt';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.router.navigate(['/etfs']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Fehler beim Aktualisieren des ETFs'
          : 'Fehler beim Erstellen des ETFs';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/etfs']);
  }
}
