import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AggregatedStockAllocation,
  CountryAllocation,
  PortfolioAnalysis,
  SectorAllocation
} from '../../models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/dashboard';

  /**
   * Get complete portfolio analysis including top 20 stocks and country allocations
   */
  getPortfolioAnalysis(portfolioId: number): Observable<PortfolioAnalysis> {
    return this.http.get<PortfolioAnalysis>(`${this.baseUrl}/analysis/${portfolioId}`);
  }

  /**
   * Get country allocation for a portfolio
   */
  getCountryAllocation(portfolioId: number): Observable<CountryAllocation[]> {
    return this.http.get<CountryAllocation[]>(`${this.baseUrl}/country-allocation/${portfolioId}`);
  }

  /**
   * Get top N stocks for a portfolio
   */
  getTopStocks(portfolioId: number, limit = 20): Observable<AggregatedStockAllocation[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<AggregatedStockAllocation[]>(
      `${this.baseUrl}/top-stocks/${portfolioId}`,
      { params }
    );
  }

  /**
   * Get all aggregated stock allocations for a portfolio
   */
  getStockAllocations(portfolioId: number): Observable<AggregatedStockAllocation[]> {
    return this.http.get<AggregatedStockAllocation[]>(`${this.baseUrl}/stock-allocations/${portfolioId}`);
  }

  /**
   * Get sector allocation for a portfolio
   */
  getSectorAllocation(portfolioId: number): Observable<SectorAllocation[]> {
    return this.http.get<SectorAllocation[]>(`${this.baseUrl}/sector-allocation/${portfolioId}`);
  }
}
