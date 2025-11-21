import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AggregatedStockAllocation } from '../../models/allocation.model';
import { PortfolioAnalysis } from '../../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/portfolios';

  getAggregatedStockAllocations(portfolioId: number): Observable<AggregatedStockAllocation[]> {
    return this.http.get<AggregatedStockAllocation[]>(
      `${this.baseUrl}/${portfolioId}/aggregated-allocations`
    );
  }

  getPortfolioAnalysis(portfolioId: number): Observable<PortfolioAnalysis> {
    return this.http.get<PortfolioAnalysis>(
      `${this.baseUrl}/${portfolioId}/analysis`
    );
  }
}
