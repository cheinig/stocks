import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Stock, StockRequest, StockResponse } from '../../models/stock.model';
import { Page, PageRequest } from '../../models/page.model';
import { ETFAllocation } from '../../models/allocation.model';

@Injectable({
  providedIn: 'root'
})
export class StockApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/stocks';

  findAll(pageRequest?: PageRequest): Observable<Page<Stock>> {
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
    return this.http.get<Page<Stock>>(this.baseUrl, { params });
  }

  findById(id: number): Observable<StockResponse> {
    return this.http.get<StockResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: StockRequest): Observable<StockResponse> {
    return this.http.post<StockResponse>(this.baseUrl, request);
  }

  update(id: number, request: StockRequest): Observable<StockResponse> {
    return this.http.put<StockResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  search(query: string, pageRequest?: PageRequest): Observable<Page<Stock>> {
    let params = new HttpParams().set('query', query);
    if (pageRequest?.page !== undefined) {
      params = params.set('page', pageRequest.page.toString());
    }
    if (pageRequest?.size !== undefined) {
      params = params.set('size', pageRequest.size.toString());
    }
    return this.http.get<Page<Stock>>(`${this.baseUrl}/search`, { params });
  }

  uploadLogo(id: number, file: Blob): Observable<void> {
    const formData = new FormData();
    formData.append('file', file, 'logo.png');
    return this.http.post<void>(`${this.baseUrl}/${id}/logo`, formData);
  }

  getLogo(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/logo`, { responseType: 'blob' });
  }

  fetchLogoFromElbstream(id: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/logo/fetch`, {});
  }

  getETFAllocations(id: number): Observable<ETFAllocation[]> {
    return this.http.get<ETFAllocation[]>(`${this.baseUrl}/${id}/etf-allocations`);
  }
}
