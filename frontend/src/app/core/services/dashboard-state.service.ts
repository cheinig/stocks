import { Injectable, signal, computed, inject } from '@angular/core';
import { AggregatedStockAllocation } from '../../models/allocation.model';
import { PortfolioAnalysis } from '../../models/dashboard.model';
import { DashboardApiService } from './dashboard-api.service';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardStateService {
  private readonly dashboardApi = inject(DashboardApiService);

  private readonly _aggregatedAllocations = signal<AggregatedStockAllocation[]>([]);
  private readonly _portfolioAnalysis = signal<PortfolioAnalysis | null>(null);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly aggregatedAllocations = this._aggregatedAllocations.asReadonly();
  readonly portfolioAnalysis = this._portfolioAnalysis.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly hasAllocations = computed(() => this._aggregatedAllocations().length > 0);
  readonly totalValue = computed(() => this._portfolioAnalysis()?.totalValue ?? 0);
  readonly sectorAllocations = computed(() => this._portfolioAnalysis()?.sectorAllocations ?? []);
  readonly countryAllocations = computed(() => this._portfolioAnalysis()?.countryAllocations ?? []);

  loadAggregatedAllocations(portfolioId: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.dashboardApi.getAggregatedStockAllocations(portfolioId).pipe(
      tap({
        next: (allocations: AggregatedStockAllocation[]) => {
          this._aggregatedAllocations.set(allocations);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Allokationen');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadPortfolioAnalysis(portfolioId: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.dashboardApi.getPortfolioAnalysis(portfolioId).pipe(
      tap({
        next: (analysis: PortfolioAnalysis) => {
          this._portfolioAnalysis.set(analysis);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Analyse');
          this._isLoading.set(false);
        }
      })
    );
  }

  clearData() {
    this._aggregatedAllocations.set([]);
    this._portfolioAnalysis.set(null);
  }

  clearError() {
    this._error.set(null);
  }
}
