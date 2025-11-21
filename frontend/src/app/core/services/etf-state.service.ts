import { Injectable, signal, computed, inject } from '@angular/core';
import { ETF, ETFRequest } from '../../models/etf.model';
import { ETFAllocation } from '../../models/allocation.model';
import { Page, PageRequest } from '../../models/page.model';
import { EtfApiService } from './etf-api.service';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EtfStateService {
  private readonly etfApi = inject(EtfApiService);

  private readonly _etfs = signal<ETF[]>([]);
  private readonly _currentEtf = signal<ETF | null>(null);
  private readonly _currentAllocations = signal<ETFAllocation[]>([]);
  private readonly _allocationHistory = signal<Record<number, ETFAllocation[]>>({});
  private readonly _totalElements = signal<number>(0);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly etfs = this._etfs.asReadonly();
  readonly currentEtf = this._currentEtf.asReadonly();
  readonly currentAllocations = this._currentAllocations.asReadonly();
  readonly allocationHistory = this._allocationHistory.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly hasEtfs = computed(() => this._etfs().length > 0);
  readonly hasAllocations = computed(() => this._currentAllocations().length > 0);

  loadEtfs(pageRequest?: PageRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.findAll(pageRequest).pipe(
      tap({
        next: (page: Page<ETF>) => {
          this._etfs.set(page.content);
          this._totalElements.set(page.totalElements);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der ETFs');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadEtfById(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.findById(id).pipe(
      tap({
        next: (etf: ETF) => {
          this._currentEtf.set(etf);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden des ETFs');
          this._isLoading.set(false);
        }
      })
    );
  }

  createEtf(request: ETFRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.create(request).pipe(
      tap({
        next: (etf: ETF) => {
          this._etfs.update(etfs => [...etfs, etf]);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Erstellen des ETFs');
          this._isLoading.set(false);
        }
      })
    );
  }

  updateEtf(id: number, request: ETFRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.update(id, request).pipe(
      tap({
        next: (updatedEtf: ETF) => {
          this._etfs.update(etfs =>
            etfs.map(e => e.id === id ? updatedEtf : e)
          );
          if (this._currentEtf()?.id === id) {
            this._currentEtf.set(updatedEtf);
          }
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Aktualisieren des ETFs');
          this._isLoading.set(false);
        }
      })
    );
  }

  deleteEtf(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.delete(id).pipe(
      tap({
        next: () => {
          this._etfs.update(etfs => etfs.filter(e => e.id !== id));
          if (this._currentEtf()?.id === id) {
            this._currentEtf.set(null);
          }
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Löschen des ETFs');
          this._isLoading.set(false);
        }
      })
    );
  }

  uploadAllocation(etfId: number, file: File) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.uploadAllocation(etfId, file).pipe(
      tap({
        next: (allocations: ETFAllocation[]) => {
          this._currentAllocations.set(allocations);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Hochladen der Allokation');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadCurrentAllocation(etfId: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.getCurrentAllocation(etfId).pipe(
      tap({
        next: (allocations: ETFAllocation[]) => {
          this._currentAllocations.set(allocations);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Allokation');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadAllocationHistory(etfId: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.getAllocationHistory(etfId).pipe(
      tap({
        next: (history: Record<number, ETFAllocation[]>) => {
          this._allocationHistory.set(history);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Historie');
          this._isLoading.set(false);
        }
      })
    );
  }

  searchEtfs(query: string, pageRequest?: PageRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.etfApi.search(query, pageRequest).pipe(
      tap({
        next: (page: Page<ETF>) => {
          this._etfs.set(page.content);
          this._totalElements.set(page.totalElements);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler bei der Suche');
          this._isLoading.set(false);
        }
      })
    );
  }

  clearCurrentEtf() {
    this._currentEtf.set(null);
    this._currentAllocations.set([]);
  }

  clearError() {
    this._error.set(null);
  }
}
