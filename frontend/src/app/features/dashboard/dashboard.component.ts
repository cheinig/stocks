import { Component, OnInit, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { PortfolioAnalysis, AggregatedStockAllocation, CountryAllocation, SectorAllocation } from '../../models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    BaseChartDirective
  ],
  template: `
    <div class="dashboard-container">
      <div class="dashboard-header">
        <h1>Portfolio Dashboard</h1>
        <button mat-raised-button color="primary" (click)="refresh()" [disabled]="loading()">
          <mat-icon>refresh</mat-icon>
          Aktualisieren
        </button>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <mat-progress-spinner mode="indeterminate"></mat-progress-spinner>
        </div>
      } @else if (analysis()) {
        <div class="dashboard-grid">
          <!-- Portfolio Summary Card -->
          <mat-card class="summary-card">
            <mat-card-header>
              <mat-card-title>Portfolio Übersicht</mat-card-title>
              <mat-card-subtitle>{{ analysis()?.portfolioName }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="summary-stats">
                <div class="stat-item">
                  <div class="stat-value">{{ analysis()?.totalUniqueStocks || 0 }}</div>
                  <div class="stat-label">Eindeutige Aktien</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ analysis()?.totalCountries || 0 }}</div>
                  <div class="stat-label">Länder</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ (analysis()?.top20Stocks?.length) || 0 }}</div>
                  <div class="stat-label">Top Positionen</div>
                </div>
                <div class="stat-item">
                  <div class="stat-value">{{ (analysis()?.allStocks?.length) || 0 }}</div>
                  <div class="stat-label">Gesamt Positionen</div>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Top 20 Stocks Card -->
          <mat-card class="stocks-card">
            <mat-card-header>
              <mat-card-title>Top 20 Aktien</mat-card-title>
              <mat-card-subtitle>Nach Gesamtallokation sortiert</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="table-container">
                <table mat-table [dataSource]="analysis()?.top20Stocks || []" class="stocks-table">
                  <ng-container matColumnDef="rank">
                    <th mat-header-cell *matHeaderCellDef>Rang</th>
                    <td mat-cell *matCellDef="let stock; let i = index">{{ i + 1 }}</td>
                  </ng-container>

                  <ng-container matColumnDef="name">
                    <th mat-header-cell *matHeaderCellDef>Name</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.name }}</td>
                  </ng-container>

                  <ng-container matColumnDef="isin">
                    <th mat-header-cell *matHeaderCellDef>ISIN</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.isin }}</td>
                  </ng-container>

                  <ng-container matColumnDef="country">
                    <th mat-header-cell *matHeaderCellDef>Land</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.country }}</td>
                  </ng-container>

                  <ng-container matColumnDef="sector">
                    <th mat-header-cell *matHeaderCellDef>Sektor</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.sector }}</td>
                  </ng-container>

                  <ng-container matColumnDef="totalPercentage">
                    <th mat-header-cell *matHeaderCellDef>Gesamt %</th>
                    <td mat-cell *matCellDef="let stock">
                      <span class="percentage">{{ stock.totalPercentage | number:'1.2-2' }}%</span>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="directPercentage">
                    <th mat-header-cell *matHeaderCellDef>Direkt %</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.directPercentage | number:'1.2-2' }}%</td>
                  </ng-container>

                  <ng-container matColumnDef="etfPercentage">
                    <th mat-header-cell *matHeaderCellDef>ETF %</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.etfPercentage | number:'1.2-2' }}%</td>
                  </ng-container>

                  <ng-container matColumnDef="etfCount">
                    <th mat-header-cell *matHeaderCellDef>ETF Anzahl</th>
                    <td mat-cell *matCellDef="let stock">{{ stock.etfCount }}</td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
                </table>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Country Allocation Card -->
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Länder-Allokation</mat-card-title>
              <mat-card-subtitle>Verteilung nach Ländern</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              @if (pieChartData()) {
                <div class="chart-wrapper">
                  <canvas
                    baseChart
                    [data]="pieChartData()!"
                    [type]="pieChartType"
                    [options]="pieChartOptions">
                  </canvas>
                </div>
                <div class="country-stats">
                  @for (country of analysis()?.countryAllocations || []; track country.countryCode) {
                    <div class="country-item">
                      <span class="country-code">{{ country.countryCode }}</span>
                      <span class="country-percentage">{{ country.percentage | number:'1.2-2' }}%</span>
                      <span class="country-stocks">({{ country.stockCount }} Aktien)</span>
                    </div>
                  }
                </div>
              }
            </mat-card-content>
          </mat-card>

          <!-- Sector Allocation Card -->
          <mat-card class="chart-card">
            <mat-card-header>
              <mat-card-title>Branchen-Allokation</mat-card-title>
              <mat-card-subtitle>Verteilung nach Branchen</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              @if (sectorChartData()) {
                <div class="chart-wrapper">
                  <canvas
                    baseChart
                    [data]="sectorChartData()!"
                    [type]="sectorChartType"
                    [options]="sectorChartOptions">
                  </canvas>
                </div>
                <div class="country-stats">
                  @for (sector of sectorAllocations() || []; track sector.sector) {
                    <div class="country-item">
                      <span class="country-code">{{ sector.sector }}</span>
                      <span class="country-percentage">{{ sector.percentage | number:'1.2-2' }}%</span>
                      <span class="country-stocks">({{ sector.stockCount }} Aktien)</span>
                    </div>
                  }
                </div>
              }
            </mat-card-content>
          </mat-card>
        </div>
      } @else if (error()) {
        <mat-card class="error-card">
          <mat-card-content>
            <div class="error-message">
              <mat-icon color="warn">error</mat-icon>
              <p>{{ error() }}</p>
              <button mat-raised-button color="primary" (click)="refresh()">
                Erneut versuchen
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 1.5rem;
      max-width: 1600px;
      margin: 0 auto;
    }

    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .dashboard-header h1 {
      margin: 0;
      font-size: 2rem;
      font-weight: 500;
    }

    .dashboard-header button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .loading-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 400px;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
      gap: 1.5rem;
    }

    .summary-card {
      grid-column: 1 / -1;
    }

    .stocks-card {
      grid-column: 1 / -1;
    }

    .chart-card {
      grid-column: 1 / -1;
    }

    @media (min-width: 1200px) {
      .chart-card {
        grid-column: span 1;
      }
    }

    .summary-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1.5rem;
      margin-top: 1rem;
    }

    .stat-item {
      text-align: center;
      padding: 1rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 8px;
    }

    .stat-value {
      font-size: 2rem;
      font-weight: 600;
      color: #3f51b5;
      margin-bottom: 0.5rem;
    }

    .stat-label {
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.7);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .table-container {
      overflow-x: auto;
      margin-top: 1rem;
    }

    .stocks-table {
      width: 100%;
      min-width: 800px;
    }

    .stocks-table th {
      font-weight: 600;
      background: rgba(255, 255, 255, 0.05);
    }

    .stocks-table td, .stocks-table th {
      padding: 0.75rem 1rem;
    }

    .percentage {
      font-weight: 600;
      color: #4caf50;
    }

    .chart-wrapper {
      max-width: 500px;
      margin: 1.5rem auto;
      position: relative;
      height: 300px;
    }

    .country-stats {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 0.75rem;
      margin-top: 1.5rem;
    }

    .country-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
    }

    .country-code {
      font-weight: 600;
      min-width: 40px;
    }

    .country-percentage {
      font-weight: 500;
      color: #3f51b5;
      margin-left: auto;
    }

    .country-stocks {
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.6);
    }

    .error-card {
      margin-top: 2rem;
    }

    .error-message {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 2rem;
      text-align: center;
    }

    .error-message mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }

    @media (max-width: 768px) {
      .dashboard-container {
        padding: 1rem;
      }

      .dashboard-header {
        flex-direction: column;
        gap: 1rem;
        align-items: stretch;
      }

      .dashboard-header button {
        width: 100%;
      }

      .dashboard-grid {
        grid-template-columns: 1fr;
      }

      .summary-stats {
        grid-template-columns: repeat(2, 1fr);
      }

      .stocks-table {
        font-size: 0.875rem;
      }

      .stocks-table td, .stocks-table th {
        padding: 0.5rem;
      }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private dashboardApi = inject(DashboardApiService);

  private readonly PORTFOLIO_ID = 1; // Hardcoded portfolio ID

  loading = signal(false);
  error = signal<string | null>(null);
  analysis = signal<PortfolioAnalysis | null>(null);
  pieChartData = signal<ChartData<'pie'> | null>(null);
  sectorAllocations = signal<SectorAllocation[] | null>(null);
  sectorChartData = signal<ChartData<'bar'> | null>(null);

  displayedColumns = ['rank', 'name', 'isin', 'country', 'sector', 'totalPercentage', 'directPercentage', 'etfPercentage', 'etfCount'];

  pieChartType: ChartType = 'pie';
  pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: true,
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
    maintainAspectRatio: true,
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
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardApi.getPortfolioAnalysis(this.PORTFOLIO_ID).subscribe({
      next: (data) => {
        this.analysis.set(data);
        this.updateChartData(data.countryAllocations);
        this.loadSectorAllocations();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading dashboard:', err);
        this.error.set('Fehler beim Laden des Dashboards. Bitte versuchen Sie es erneut.');
        this.loading.set(false);
      }
    });
  }

  loadSectorAllocations(): void {
    this.dashboardApi.getSectorAllocation(this.PORTFOLIO_ID).subscribe({
      next: (data) => {
        this.sectorAllocations.set(data);
        this.updateSectorChartData(data);
      },
      error: (err) => {
        console.error('Error loading sector allocations:', err);
      }
    });
  }

  updateChartData(countryAllocations: CountryAllocation[]): void {
    if (!countryAllocations || countryAllocations.length === 0) {
      this.pieChartData.set(null);
      return;
    }

    const sortedAllocations = [...countryAllocations].sort((a, b) => b.percentage - a.percentage);

    const chartData: ChartData<'pie'> = {
      labels: sortedAllocations.map(c => c.countryCode),
      datasets: [{
        data: sortedAllocations.map(c => c.percentage),
        backgroundColor: this.generateColors(sortedAllocations.length),
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

    const chartData: ChartData<'bar'> = {
      labels: sortedAllocations.map(s => s.sector),
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
      '#3f51b5', // Indigo
      '#4caf50', // Green
      '#ff9800', // Orange
      '#f44336', // Red
      '#9c27b0', // Purple
      '#2196f3', // Blue
      '#ffeb3b', // Yellow
      '#00bcd4', // Cyan
      '#e91e63', // Pink
      '#009688', // Teal
      '#ff5722', // Deep Orange
      '#673ab7', // Deep Purple
    ];

    const colors: string[] = [];
    for (let i = 0; i < count; i++) {
      colors.push(baseColors[i % baseColors.length]);
    }
    return colors;
  }

  refresh(): void {
    this.loadDashboard();
  }
}
