import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { FileUploadComponent } from '../../../shared/components/file-upload.component';
import { IconComponent } from '../../../shared/components/icon.component';
import { PortfolioApiService } from '../../../core/services/portfolio-api.service';
import { PortfolioImportResult } from '../../../models/portfolio.model';

export interface PortfolioImportDialogData {
  portfolioId: number;
}

@Component({
  selector: 'app-portfolio-import-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, FileUploadComponent, IconComponent],
  template: `
    <div class="import-dialog">
      <h2 mat-dialog-title>Werte importieren</h2>

      <div class="dialog-content">
        @if (!result()) {
          <p class="hint">
            Laden Sie den CSV-Export Ihres Depots hoch. Für jede ISIN wird der
            aktuelle Wert (Spalte „Wert") auf volle Euro gerundet in die passende
            Position übernommen.
          </p>

          <app-file-upload
            acceptedTypes=".csv"
            [maxSizeInMB]="10"
            (uploadClick)="onUpload($event)">
          </app-file-upload>
        } @else {
          <div class="result">
            @if (result()!.success) {
              <div class="result-summary success">
                <mat-icon fontIcon="check_circle"></mat-icon>
                <span>{{ result()!.updatedCount }} von {{ result()!.totalRows }} Position(en) aktualisiert</span>
              </div>
            } @else {
              <div class="result-summary error">
                <mat-icon fontIcon="error"></mat-icon>
                <span>{{ result()!.errorMessage || 'Import fehlgeschlagen' }}</span>
              </div>
            }

            @if (result()!.warnings.length) {
              <ul class="warnings">
                @for (warning of result()!.warnings; track warning) {
                  <li>{{ warning }}</li>
                }
              </ul>
            }

            @if (result()!.unmatchedIsins.length) {
              <div class="unmatched">
                <p class="unmatched-title">Nicht zugeordnete ISINs:</p>
                <p class="unmatched-list">{{ result()!.unmatchedIsins.join(', ') }}</p>
              </div>
            }
          </div>
        }
      </div>

      <div class="dialog-actions">
        <button mat-button (click)="close()">
          {{ result()?.success ? 'Schließen' : 'Abbrechen' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .import-dialog {
      padding: 1rem;
      min-width: 420px;
    }

    h2 {
      margin: 0 0 1rem 0;
      font-size: 1.5rem;
      font-weight: 500;
    }

    .dialog-content {
      margin-bottom: 1.5rem;
    }

    .hint {
      margin: 0 0 1rem 0;
      color: rgba(255, 255, 255, 0.7);
      font-size: 0.9rem;
    }

    .result-summary {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 500;
      margin-bottom: 1rem;
    }

    .result-summary.success {
      color: #4caf50;
    }

    .result-summary.error {
      color: #f44336;
    }

    .warnings {
      margin: 0 0 1rem 0;
      padding-left: 1.25rem;
      color: #ffb74d;
      font-size: 0.875rem;
    }

    .unmatched-title {
      margin: 0 0 0.25rem 0;
      font-weight: 500;
    }

    .unmatched-list {
      margin: 0;
      font-size: 0.85rem;
      color: rgba(255, 255, 255, 0.7);
      word-break: break-word;
    }

    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    @media (max-width: 600px) {
      .import-dialog {
        min-width: 280px;
      }
    }
  `]
})
export class PortfolioImportDialogComponent {
  private dialogRef = inject(MatDialogRef<PortfolioImportDialogComponent>);
  private data = inject<PortfolioImportDialogData>(MAT_DIALOG_DATA);
  private portfolioApi = inject(PortfolioApiService);

  result = signal<PortfolioImportResult | null>(null);
  uploading = signal(false);

  onUpload(file: File): void {
    this.uploading.set(true);
    this.portfolioApi.importValues(this.data.portfolioId, file).subscribe({
      next: (result) => {
        this.uploading.set(false);
        this.result.set(result);
      },
      error: (err) => {
        this.uploading.set(false);
        this.result.set({
          portfolioId: this.data.portfolioId,
          totalRows: 0,
          updatedCount: 0,
          unmatchedIsins: [],
          warnings: [],
          success: false,
          errorMessage: err?.error?.errorMessage || 'Import fehlgeschlagen'
        });
      }
    });
  }

  close(): void {
    // Signal success so the caller can reload the portfolio
    this.dialogRef.close(this.result()?.success ? this.result() : null);
  }
}
