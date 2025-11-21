import { Injectable, signal, computed, inject } from '@angular/core';
import { Stock, StockRequest } from '../../models/stock.model';
import { Page, PageRequest } from '../../models/page.model';
import { StockApiService } from './stock-api.service';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class StockStateService {
  private readonly stockApi = inject(StockApiService);

  private readonly _stocks = signal<Stock[]>([]);
  private readonly _currentStock = signal<Stock | null>(null);
  private readonly _totalElements = signal<number>(0);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly stocks = this._stocks.asReadonly();
  readonly currentStock = this._currentStock.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly hasStocks = computed(() => this._stocks().length > 0);

  loadStocks(pageRequest?: PageRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.findAll(pageRequest).pipe(
      tap({
        next: (page: Page<Stock>) => {
          this._stocks.set(page.content);
          this._totalElements.set(page.totalElements);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Stocks');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadStockById(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.findById(id).pipe(
      tap({
        next: (stock: Stock) => {
          this._currentStock.set(stock);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden des Stocks');
          this._isLoading.set(false);
        }
      })
    );
  }

  createStock(request: StockRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.create(request).pipe(
      tap({
        next: (stock: Stock) => {
          this._stocks.update(stocks => [...stocks, stock]);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Erstellen des Stocks');
          this._isLoading.set(false);
        }
      })
    );
  }

  updateStock(id: number, request: StockRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.update(id, request).pipe(
      tap({
        next: (updatedStock: Stock) => {
          this._stocks.update(stocks =>
            stocks.map(s => s.id === id ? updatedStock : s)
          );
          if (this._currentStock()?.id === id) {
            this._currentStock.set(updatedStock);
          }
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Aktualisieren des Stocks');
          this._isLoading.set(false);
        }
      })
    );
  }

  deleteStock(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.delete(id).pipe(
      tap({
        next: () => {
          this._stocks.update(stocks => stocks.filter(s => s.id !== id));
          if (this._currentStock()?.id === id) {
            this._currentStock.set(null);
          }
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Löschen des Stocks');
          this._isLoading.set(false);
        }
      })
    );
  }

  searchStocks(query: string, pageRequest?: PageRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.stockApi.search(query, pageRequest).pipe(
      tap({
        next: (page: Page<Stock>) => {
          this._stocks.set(page.content);
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

  clearCurrentStock() {
    this._currentStock.set(null);
  }

  clearError() {
    this._error.set(null);
  }
}
