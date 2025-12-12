import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

import { EtfStateService } from '../../../core/services/etf-state.service';
import { EtfApiService } from '../../../core/services/etf-api.service';
import { isWebImporter } from '../../../models/enums';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { FileUploadComponent } from '../../../shared/components/file-upload.component';
import { ETFStatistics } from '../../../models/etf.model';
import { CountryAllocation, SectorAllocation } from '../../../models/dashboard.model';
import { SectorNamePipe } from '../../../shared/pipes/sector-name.pipe';

@Component({
  selector: 'app-etf-details',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatSnackBarModule,
    MatTableModule,
    MatIconModule,
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    FileUploadComponent,
    BaseChartDirective,
    SectorNamePipe
  ],
  templateUrl: './etf-details.component.html',
  styleUrls: ['./etf-details.component.scss']
})
export class EtfDetailsComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  private etfApi = inject(EtfApiService);
  etfState = inject(EtfStateService);

  etfId?: number;
  selectedFile = signal<File | null>(null);
  uploadProgress = signal(false);
  uploadSuccess = signal<string | null>(null);
  statistics = signal<ETFStatistics | null>(null);
  loadingStatistics = signal(false);
  logoUrl = signal<string | null>(null);

  pieChartData = signal<ChartData<'pie'> | null>(null);
  sectorChartData = signal<ChartData<'bar'> | null>(null);

  allocationColumns = ['stockName', 'stockIsin', 'percentage'];

  // Sorted allocations by percentage descending
  sortedAllocations = computed(() => {
    const allocations = this.etfState.currentAllocations();
    return [...allocations].sort((a, b) => b.percentage - a.percentage);
  });

  pieChartType: ChartType = 'pie';
  pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const label = context.label || '';
            const value = context.parsed || 0;
            return `${label}: ${value.toFixed(2)}%`;
          }
        }
      }
    }
  };

  sectorChartType: ChartType = 'bar';
  sectorChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        callbacks: {
          label: (context) => {
            const value = context.parsed.y || 0;
            return `${value.toFixed(2)}%`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Prozent (%)'
        }
      },
      x: {
        title: {
          display: true,
          text: 'Branche'
        }
      }
    }
  };

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
        const etf = this.etfState.currentEtf();
        if (etf?.hasLogo) {
          this.loadLogoFromBackend();
        }
        this.loadAllocations();
        this.loadStatistics();
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden des ETFs', 'OK', { duration: 3000 });
      }
    });
  }

  loadLogoFromBackend(): void {
    if (!this.etfId) return;

    this.etfApi.getLogo(this.etfId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.logoUrl.set(url);
      },
      error: (err) => {
        console.error('Error loading logo from backend:', err);
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

  loadStatistics(): void {
    if (!this.etfId) return;

    this.loadingStatistics.set(true);
    this.etfApi.getStatistics(this.etfId).subscribe({
      next: (stats) => {
        this.statistics.set(stats);
        if (stats.countryAllocations && stats.countryAllocations.length > 0) {
          this.updateChartData(stats.countryAllocations);
        }
        if (stats.sectorAllocations && stats.sectorAllocations.length > 0) {
          this.updateSectorChartData(stats.sectorAllocations);
        }
        this.loadingStatistics.set(false);
      },
      error: (err) => {
        this.loadingStatistics.set(false);
        console.error('Error loading statistics:', err);
        // Don't show error if ETF has no allocations yet
        if (err.status !== 404) {
          this.snackBar.open('Fehler beim Laden der Statistiken', 'OK', { duration: 3000 });
        }
      }
    });
  }

  updateChartData(countryAllocations: CountryAllocation[]): void {
    if (!countryAllocations || countryAllocations.length === 0) {
      this.pieChartData.set(null);
      return;
    }

    const sortedAllocations = [...countryAllocations].sort((a, b) => Number(b.percentage) - Number(a.percentage));

    // Show countries with at least 0.3% weight individually, rest as "Others"
    const minPercentageThreshold = 0.3;

    const significantAllocations = sortedAllocations.filter(c => Number(c.percentage) >= minPercentageThreshold);
    const insignificantAllocations = sortedAllocations.filter(c => Number(c.percentage) < minPercentageThreshold);

    let labels: string[];
    let data: number[];

    if (insignificantAllocations.length === 0) {
      labels = significantAllocations.map(c => c.countryCode);
      data = significantAllocations.map(c => Number(c.percentage));
    } else {
      const othersPercentage = insignificantAllocations.reduce((sum, c) => sum + Number(c.percentage), 0);
      labels = [...significantAllocations.map(c => c.countryCode), 'Sonstige'];
      data = [...significantAllocations.map(c => Number(c.percentage)), othersPercentage];
    }

    const chartData: ChartData<'pie'> = {
      labels: labels,
      datasets: [{
        data: data,
        backgroundColor: this.generateColors(labels.length),
        borderWidth: 2,
        borderColor: '#1a1a1a'
      }]
    };

    this.pieChartData.set(chartData);
  }

  updateSectorChartData(sectorAllocations: SectorAllocation[]): void {
    if (!sectorAllocations || sectorAllocations.length === 0) {
      this.sectorChartData.set(null);
      return;
    }

    const sortedAllocations = [...sectorAllocations].sort((a, b) => b.percentage - a.percentage);
    const sectorPipe = new SectorNamePipe();

    const chartData: ChartData<'bar'> = {
      labels: sortedAllocations.map(s => sectorPipe.transform(s.sector)),
      datasets: [{
        label: 'Prozent',
        data: sortedAllocations.map(s => s.percentage),
        backgroundColor: this.generateColors(sortedAllocations.length),
        borderWidth: 1,
        borderColor: '#1a1a1a'
      }]
    };

    this.sectorChartData.set(chartData);
  }

  generateColors(count: number): string[] {
    const baseColors = [
      '#B4A7D6', '#A8D8B9', '#FFD6A5', '#FFADAD', '#D5AAFF',
      '#9DB4FF', '#FFF5BA', '#A2E4E8', '#FFB3D9', '#B2DFD5',
      '#FFCCB5', '#C5B3E6', '#C9E4C5', '#FFE5D0', '#E4C4D8',
      '#B5E7E7', '#F7D794', '#DCC6E0', '#AED9E0', '#FFD3B5',
    ];

    const colors: string[] = [];
    for (let i = 0; i < count; i++) {
      colors.push(baseColors[i % baseColors.length]);
    }
    return colors;
  }

  getCountryFlag(countryCode: string): string {
    if (countryCode === 'Sonstige') {
      return '🌍';
    }

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

  getTopCountries(): CountryAllocation[] {
    const countries = this.statistics()?.countryAllocations || [];
    return countries.slice(0, 10);
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
      next: (statistics: any) => {
        this.uploadProgress.set(false);

        let message = `Allocation erfolgreich importiert: ${statistics.totalEntries} Einträge`;
        if (statistics.warnings && statistics.warnings.length > 0) {
          message += '\n\nWarnungen:\n' + statistics.warnings.join('\n');
        }

        this.uploadSuccess.set(message);
        this.selectedFile.set(null);
        this.loadAllocations();
        this.loadStatistics();

        const snackBarMessage = statistics.warnings && statistics.warnings.length > 0
          ? `Import erfolgreich mit ${statistics.warnings.length} Warnung(en)`
          : 'Allocation erfolgreich hochgeladen';
        this.snackBar.open(snackBarMessage, 'OK', { duration: 5000 });
      },
      error: (err: any) => {
        this.uploadProgress.set(false);
        const message = err?.error?.message || 'Fehler beim Hochladen der Datei';
        this.snackBar.open(message, 'OK', { duration: 5000 });
      }
    });
  }

  onRefresh(): void {
    if (!this.etfId) return;

    this.uploadProgress.set(true);
    this.uploadSuccess.set(null);

    this.etfState.refreshWebHoldings(this.etfId).subscribe({
      next: (response) => {
        this.uploadProgress.set(false);

        let message = response.message;
        if (response.warnings && response.warnings.length > 0) {
          message += '\n\nWarnungen:\n' + response.warnings.join('\n');
        }

        this.uploadSuccess.set(message);
        this.loadAllocations();
        this.loadStatistics();

        const snackBarMessage = response.warnings && response.warnings.length > 0
          ? `Aktualisierung erfolgreich mit ${response.warnings.length} Warnung(en)`
          : response.message;
        this.snackBar.open(snackBarMessage, 'OK', { duration: 5000 });
      },
      error: (err: any) => {
        this.uploadProgress.set(false);
        const message = err?.error?.message || 'Fehler beim Aktualisieren der Holdings';
        this.snackBar.open(message, 'OK', { duration: 5000 });
      }
    });
  }

  isWebImporterType(): boolean {
    const etf = this.etfState.currentEtf();
    return etf?.importerType ? isWebImporter(etf.importerType) : false;
  }

  editEtf(): void {
    if (this.etfId) {
      this.router.navigate(['/etfs', this.etfId, 'edit']);
    }
  }

  showHistory(): void {
    this.snackBar.open('Historie-Ansicht wird in Phase 9.4 implementiert', 'OK', { duration: 3000 });
  }

  goBack(): void {
    this.router.navigate(['/etfs']);
  }

  viewStockDetails(allocation: any): void {
    if (allocation.stockId) {
      this.router.navigate(['/stocks', allocation.stockId]);
    }
  }
}
