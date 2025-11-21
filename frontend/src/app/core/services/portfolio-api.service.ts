import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Portfolio,
  PortfolioRequest,
  PortfolioResponse,
  PortfolioPosition,
  PortfolioPositionRequest,
  PortfolioWithPositions
} from '../../models/portfolio.model';
import { Page, PageRequest } from '../../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class PortfolioApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/portfolios';

  findAll(pageRequest?: PageRequest): Observable<Page<Portfolio>> {
    let params = new HttpParams();
    if (pageRequest?.page !== undefined) {
      params = params.set('page', pageRequest.page.toString());
    }
    if (pageRequest?.size !== undefined) {
      params = params.set('size', pageRequest.size.toString());
    }
    if (pageRequest?.sort) {
      params = params.set('sort', pageRequest.sort);
    }
    return this.http.get<Page<Portfolio>>(this.baseUrl, { params });
  }

  findById(id: number): Observable<PortfolioResponse> {
    return this.http.get<PortfolioResponse>(`${this.baseUrl}/${id}`);
  }

  findByIdWithPositions(id: number): Observable<PortfolioWithPositions> {
    return this.http.get<PortfolioWithPositions>(`${this.baseUrl}/${id}/with-positions`);
  }

  create(request: PortfolioRequest): Observable<PortfolioResponse> {
    return this.http.post<PortfolioResponse>(this.baseUrl, request);
  }

  update(id: number, request: PortfolioRequest): Observable<PortfolioResponse> {
    return this.http.put<PortfolioResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // Position Management
  getPositions(portfolioId: number): Observable<PortfolioPosition[]> {
    return this.http.get<PortfolioPosition[]>(`${this.baseUrl}/${portfolioId}/positions`);
  }

  addPosition(portfolioId: number, request: PortfolioPositionRequest): Observable<PortfolioPosition> {
    return this.http.post<PortfolioPosition>(`${this.baseUrl}/${portfolioId}/positions`, request);
  }

  updatePosition(positionId: number, request: PortfolioPositionRequest): Observable<PortfolioPosition> {
    return this.http.put<PortfolioPosition>(`${this.baseUrl}/positions/${positionId}`, request);
  }

  deletePosition(positionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/positions/${positionId}`);
  }

  getStockPositions(portfolioId: number): Observable<PortfolioPosition[]> {
    return this.http.get<PortfolioPosition[]>(`${this.baseUrl}/${portfolioId}/positions/stocks`);
  }

  getEtfPositions(portfolioId: number): Observable<PortfolioPosition[]> {
    return this.http.get<PortfolioPosition[]>(`${this.baseUrl}/${portfolioId}/positions/etfs`);
  }
}
