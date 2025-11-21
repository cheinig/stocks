import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ETF, ETFRequest, ETFResponse } from '../../models/etf.model';
import { ETFAllocation } from '../../models/allocation.model';
import { Page, PageRequest } from '../../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class EtfApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/etfs';

  findAll(pageRequest?: PageRequest): Observable<Page<ETF>> {
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
    return this.http.get<Page<ETF>>(this.baseUrl, { params });
  }

  findById(id: number): Observable<ETFResponse> {
    return this.http.get<ETFResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: ETFRequest): Observable<ETFResponse> {
    return this.http.post<ETFResponse>(this.baseUrl, request);
  }

  update(id: number, request: ETFRequest): Observable<ETFResponse> {
    return this.http.put<ETFResponse>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  search(query: string, pageRequest?: PageRequest): Observable<Page<ETF>> {
    let params = new HttpParams().set('query', query);
    if (pageRequest?.page !== undefined) {
      params = params.set('page', pageRequest.page.toString());
    }
    if (pageRequest?.size !== undefined) {
      params = params.set('size', pageRequest.size.toString());
    }
    return this.http.get<Page<ETF>>(`${this.baseUrl}/search`, { params });
  }

  uploadAllocation(etfId: number, file: File): Observable<ETFAllocation[]> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ETFAllocation[]>(`${this.baseUrl}/${etfId}/allocations/upload`, formData);
  }

  getCurrentAllocation(etfId: number): Observable<ETFAllocation[]> {
    return this.http.get<ETFAllocation[]>(`${this.baseUrl}/${etfId}/allocations/current`);
  }

  getAllocationByVersion(etfId: number, version: number): Observable<ETFAllocation[]> {
    return this.http.get<ETFAllocation[]>(`${this.baseUrl}/${etfId}/allocations/version/${version}`);
  }

  getAllocationHistory(etfId: number): Observable<Record<number, ETFAllocation[]>> {
    return this.http.get<Record<number, ETFAllocation[]>>(`${this.baseUrl}/${etfId}/allocations/history`);
  }
}
