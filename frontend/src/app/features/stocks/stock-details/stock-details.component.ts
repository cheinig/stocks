import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { StockStateService } from '../../../core/services/stock-state.service';
import { StockApiService } from '../../../core/services/stock-api.service';
import { ETFAllocation } from '../../../models/allocation.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { SectorNamePipe } from '../../../shared/pipes/sector-name.pipe';

@Component({
  selector: 'app-stock-details',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
    MatIconModule,
    MatTableModule,
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    SectorNamePipe
  ],
  templateUrl: './stock-details.component.html',
  styleUrls: ['./stock-details.component.scss']
})
export class StockDetailsComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private stockApi = inject(StockApiService);
  stockState = inject(StockStateService);

  stockId?: number;
  logoUrl = signal<string | null>(null);
  etfAllocations = signal<ETFAllocation[]>([]);
  loadingAllocations = signal(false);
  displayedColumns = ['etfName', 'percentage'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.stockId = parseInt(id, 10);
      this.loadStock();
    }
  }

  loadStock(): void {
    if (!this.stockId) return;

    this.stockState.loadStockById(this.stockId).subscribe({
      next: () => {
        const stock = this.stockState.currentStock();
        if (stock?.hasLogo) {
          this.loadLogoFromBackend();
        }
        this.loadETFAllocations();
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Aktie', 'OK', { duration: 3000 });
      }
    });
  }

  loadETFAllocations(): void {
    if (!this.stockId) return;

    this.loadingAllocations.set(true);
    this.stockApi.getETFAllocations(this.stockId).subscribe({
      next: (allocations) => {
        this.etfAllocations.set(allocations);
        this.loadingAllocations.set(false);
      },
      error: (err) => {
        console.error('Error loading ETF allocations:', err);
        this.loadingAllocations.set(false);
      }
    });
  }

  loadLogoFromBackend(): void {
    if (!this.stockId) return;

    this.stockApi.getLogo(this.stockId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.logoUrl.set(url);
      },
      error: (err) => {
        console.error('Error loading logo from backend:', err);
      }
    });
  }

  editStock(): void {
    if (this.stockId) {
      this.router.navigate(['/stocks', this.stockId, 'edit']);
    }
  }

  getCountryFlag(countryCode: string): string {
    const flagMap: { [key: string]: string } = {
      'US': '🇺🇸', 'CA': '🇨🇦', 'MX': '🇲🇽',
      'DE': '🇩🇪', 'FR': '🇫🇷', 'GB': '🇬🇧', 'IT': '🇮🇹', 'ES': '🇪🇸',
      'NL': '🇳🇱', 'CH': '🇨🇭', 'BE': '🇧🇪', 'AT': '🇦🇹', 'SE': '🇸🇪',
      'NO': '🇳🇴', 'DK': '🇩🇰', 'FI': '🇫🇮', 'IE': '🇮🇪', 'PL': '🇵🇱',
      'PT': '🇵🇹', 'GR': '🇬🇷', 'CZ': '🇨🇿', 'RO': '🇷🇴', 'HU': '🇭🇺',
      'LU': '🇱🇺',
      'JP': '🇯🇵', 'CN': '🇨🇳', 'HK': '🇭🇰', 'SG': '🇸🇬', 'KR': '🇰🇷',
      'IN': '🇮🇳', 'TW': '🇹🇼', 'TH': '🇹🇭', 'ID': '🇮🇩', 'MY': '🇲🇾',
      'PH': '🇵🇭', 'VN': '🇻🇳',
      'AE': '🇦🇪', 'SA': '🇸🇦', 'IL': '🇮🇱', 'TR': '🇹🇷', 'KW': '🇰🇼',
      'QA': '🇶🇦', 'BH': '🇧🇭', 'OM': '🇴🇲',
      'AU': '🇦🇺', 'NZ': '🇳🇿',
      'BR': '🇧🇷', 'AR': '🇦🇷', 'CL': '🇨🇱', 'CO': '🇨🇴', 'PE': '🇵🇪',
      'ZA': '🇿🇦', 'EG': '🇪🇬', 'NG': '🇳🇬', 'KE': '🇰🇪', 'XX': '🏳️'
    };

    return flagMap[countryCode] || '🏳️';
  }

  goBack(): void {
    this.router.navigate(['/stocks']);
  }

  viewEtfDetails(allocation: ETFAllocation): void {
    if (allocation.etfId) {
      this.router.navigate(['/etfs', allocation.etfId]);
    }
  }
}
