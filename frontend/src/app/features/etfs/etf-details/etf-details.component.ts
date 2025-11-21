import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';

import { EtfStateService } from '../../../core/services/etf-state.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { FileUploadComponent } from '../../../shared/components/file-upload.component';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-etf-details',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTableModule,
    IconComponent,
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    FileUploadComponent
  ],
  template: `
    <div class="etf-details-container">
      <div class="header">
        <button mat-icon-button (click)="goBack()">
          <mat-icon fontIcon="arrow_back"></mat-icon>
        </button>
        <h1>ETF-Details</h1>
      </div>

      @if (etfState.isLoading()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (etfState.error()) {
        <app-error-message
          [title]="'Fehler beim Laden des ETFs'"
          [message]="etfState.error() || ''"
          [onRetry]="loadEtf.bind(this)">
        </app-error-message>
      } @else if (etfState.currentEtf()) {
        <!-- ETF Info Card -->
        <mat-card class="etf-info-card">
          <mat-card-header>
            <mat-card-title>{{ etfState.currentEtf()?.name }}</mat-card-title>
            <mat-card-subtitle>{{ etfState.currentEtf()?.isin }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">Importer-Typ:</span>
                <span class="value">{{ etfState.currentEtf()?.importerType }}</span>
              </div>
            </div>
            <div class="actions">
              <button mat-button color="primary" (click)="editEtf()">
                <mat-icon fontIcon="edit"></mat-icon>
                Bearbeiten
              </button>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- File Upload Card -->
        <mat-card class="upload-card">
          <mat-card-header>
            <mat-card-title>Allocation-Daten hochladen</mat-card-title>
            <mat-card-subtitle>
              Laden Sie eine Datei mit der Zusammensetzung des ETFs hoch (CSV oder Excel)
            </mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <app-file-upload
              [acceptedTypes]="'.csv,.xlsx,.xls'"
              [maxSizeInMB]="10"
              (fileSelected)="onFileSelected($event)"
              (uploadClicked)="onUpload()">
            </app-file-upload>

            @if (uploadProgress()) {
              <div class="upload-progress">
                <mat-icon fontIcon="hourglass_empty"></mat-icon>
                <span>Upload läuft...</span>
              </div>
            }

            @if (uploadSuccess()) {
              <div class="upload-success">
                <mat-icon fontIcon="check"></mat-icon>
                <span>{{ uploadSuccess() }}</span>
              </div>
            }
          </mat-card-content>
        </mat-card>

        <!-- Current Allocation Card -->
        @if (etfState.currentAllocations() && etfState.currentAllocations().length > 0) {
          <mat-card class="allocation-card">
            <mat-card-header>
              <mat-card-title>Aktuelle Allocation</mat-card-title>
              <mat-card-subtitle>
                {{ etfState.currentAllocations().length }} Positionen
              </mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="table-container">
                <table mat-table [dataSource]="etfState.currentAllocations()">
                  <ng-container matColumnDef="stockName">
                    <th mat-header-cell *matHeaderCellDef>Name</th>
                    <td mat-cell *matCellDef="let allocation">{{ allocation.stock?.name || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="stockIsin">
                    <th mat-header-cell *matHeaderCellDef>ISIN</th>
                    <td mat-cell *matCellDef="let allocation">{{ allocation.stock?.isin || 'N/A' }}</td>
                  </ng-container>

                  <ng-container matColumnDef="percentage">
                    <th mat-header-cell *matHeaderCellDef>Prozent</th>
                    <td mat-cell *matCellDef="let allocation">{{ allocation.percentage | number:'1.2-2' }}%</td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="allocationColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: allocationColumns;"></tr>
                </table>
              </div>

              <div class="allocation-actions">
                <button mat-button color="primary" (click)="showHistory()">
                  <mat-icon fontIcon="history"></mat-icon>
                  Historie anzeigen
                </button>
              </div>
            </mat-card-content>
          </mat-card>
        }
      }
    </div>
  `,
  styles: [`
    .etf-details-container {
      width: 100%;
      max-width: 1200px;
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
      margin-bottom: 2rem;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .label {
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.6);
    }

    .value {
      font-size: 1rem;
      font-weight: 500;
    }

    .actions {
      display: flex;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    .upload-progress,
    .upload-success {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 1rem;
      padding: 1rem;
      border-radius: 4px;
    }

    .upload-progress {
      background-color: rgba(33, 150, 243, 0.1);
      color: #2196f3;
    }

    .upload-success {
      background-color: rgba(76, 175, 80, 0.1);
      color: #4caf50;
    }

    .table-container {
      width: 100%;
      overflow-x: auto;
      margin-bottom: 1rem;
    }

    .mat-mdc-table {
      width: 100%;
    }

    .allocation-actions {
      display: flex;
      justify-content: flex-end;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    @media (max-width: 768px) {
      .info-grid {
        grid-template-columns: 1fr;
      }

      .actions {
        flex-direction: column;
      }

      .actions button {
        width: 100%;
      }
    }
  `]
})
export class EtfDetailsComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  etfState = inject(EtfStateService);

  etfId?: number;
  selectedFile = signal<File | null>(null);
  uploadProgress = signal(false);
  uploadSuccess = signal<string | null>(null);

  allocationColumns = ['stockName', 'stockIsin', 'percentage'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.etfId = parseInt(id, 10);
      this.loadEtf();
    }
  }

  loadEtf(): void {
    if (!this.etfId) return;

    this.etfState.loadEtfById(this.etfId).subscribe({
      next: () => {
        this.loadAllocations();
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden des ETFs', 'OK', { duration: 3000 });
      }
    });
  }

  loadAllocations(): void {
    if (!this.etfId) return;

    this.etfState.loadAllocation(this.etfId).subscribe({
      error: () => {
        this.snackBar.open('Fehler beim Laden der Allocations', 'OK', { duration: 3000 });
      }
    });
  }

  onFileSelected(file: File): void {
    this.selectedFile.set(file);
    this.uploadSuccess.set(null);
  }

  onUpload(): void {
    const file = this.selectedFile();
    if (!file || !this.etfId) return;

    this.uploadProgress.set(true);
    this.uploadSuccess.set(null);

    this.etfState.uploadAllocation(this.etfId, file).subscribe({
      next: () => {
        this.uploadProgress.set(false);
        this.uploadSuccess.set('Allocation erfolgreich importiert');
        this.selectedFile.set(null);
        this.loadAllocations();
        this.snackBar.open('Allocation erfolgreich hochgeladen', 'OK', { duration: 3000 });
      },
      error: (err: any) => {
        this.uploadProgress.set(false);
        const message = err?.error?.message || 'Fehler beim Hochladen der Datei';
        this.snackBar.open(message, 'OK', { duration: 5000 });
      }
    });
  }

  editEtf(): void {
    if (this.etfId) {
      this.router.navigate(['/etfs', this.etfId, 'edit']);
    }
  }

  showHistory(): void {
    // TODO: Implement history dialog
    this.snackBar.open('Historie-Ansicht wird in Phase 9.4 implementiert', 'OK', { duration: 3000 });
  }

  goBack(): void {
    this.router.navigate(['/etfs']);
  }
}
