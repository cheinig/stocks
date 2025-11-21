import { Injectable, signal, computed, inject } from '@angular/core';
import {
  Portfolio,
  PortfolioRequest,
  PortfolioPosition,
  PortfolioPositionRequest,
  PortfolioWithPositions
} from '../../models/portfolio.model';
import { Page, PageRequest } from '../../models/page.model';
import { PortfolioApiService } from './portfolio-api.service';
import { tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PortfolioStateService {
  private readonly portfolioApi = inject(PortfolioApiService);

  private readonly _portfolios = signal<Portfolio[]>([]);
  private readonly _currentPortfolio = signal<PortfolioWithPositions | null>(null);
  private readonly _totalElements = signal<number>(0);
  private readonly _isLoading = signal<boolean>(false);
  private readonly _error = signal<string | null>(null);

  readonly portfolios = this._portfolios.asReadonly();
  readonly currentPortfolio = this._currentPortfolio.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly hasPortfolios = computed(() => this._portfolios().length > 0);
  readonly hasPositions = computed(() =>
    (this._currentPortfolio()?.positions?.length ?? 0) > 0
  );

  loadPortfolios(pageRequest?: PageRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.findAll(pageRequest).pipe(
      tap({
        next: (page: Page<Portfolio>) => {
          this._portfolios.set(page.content);
          this._totalElements.set(page.totalElements);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden der Portfolios');
          this._isLoading.set(false);
        }
      })
    );
  }

  loadPortfolioById(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.findByIdWithPositions(id).pipe(
      tap({
        next: (portfolio: PortfolioWithPositions) => {
          this._currentPortfolio.set(portfolio);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Laden des Portfolios');
          this._isLoading.set(false);
        }
      })
    );
  }

  createPortfolio(request: PortfolioRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.create(request).pipe(
      tap({
        next: (portfolio: Portfolio) => {
          this._portfolios.update(portfolios => [...portfolios, portfolio]);
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Erstellen des Portfolios');
          this._isLoading.set(false);
        }
      })
    );
  }

  updatePortfolio(id: number, request: PortfolioRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.update(id, request).pipe(
      tap({
        next: (updatedPortfolio: Portfolio) => {
          this._portfolios.update(portfolios =>
            portfolios.map(p => p.id === id ? updatedPortfolio : p)
          );
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Aktualisieren des Portfolios');
          this._isLoading.set(false);
        }
      })
    );
  }

  deletePortfolio(id: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.delete(id).pipe(
      tap({
        next: () => {
          this._portfolios.update(portfolios => portfolios.filter(p => p.id !== id));
          if (this._currentPortfolio()?.id === id) {
            this._currentPortfolio.set(null);
          }
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Löschen des Portfolios');
          this._isLoading.set(false);
        }
      })
    );
  }

  addPosition(portfolioId: number, request: PortfolioPositionRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.addPosition(portfolioId, request).pipe(
      tap({
        next: (position: PortfolioPosition) => {
          this._currentPortfolio.update(portfolio => {
            if (portfolio && portfolio.id === portfolioId) {
              return {
                ...portfolio,
                positions: [...portfolio.positions, position]
              };
            }
            return portfolio;
          });
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Hinzufügen der Position');
          this._isLoading.set(false);
        }
      })
    );
  }

  updatePosition(positionId: number, request: PortfolioPositionRequest) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.updatePosition(positionId, request).pipe(
      tap({
        next: (updatedPosition: PortfolioPosition) => {
          this._currentPortfolio.update(portfolio => {
            if (portfolio) {
              return {
                ...portfolio,
                positions: portfolio.positions.map(p =>
                  p.id === positionId ? updatedPosition : p
                )
              };
            }
            return portfolio;
          });
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Aktualisieren der Position');
          this._isLoading.set(false);
        }
      })
    );
  }

  deletePosition(positionId: number) {
    this._isLoading.set(true);
    this._error.set(null);

    return this.portfolioApi.deletePosition(positionId).pipe(
      tap({
        next: () => {
          this._currentPortfolio.update(portfolio => {
            if (portfolio) {
              return {
                ...portfolio,
                positions: portfolio.positions.filter(p => p.id !== positionId)
              };
            }
            return portfolio;
          });
          this._isLoading.set(false);
        },
        error: (error) => {
          this._error.set(error.message || 'Fehler beim Löschen der Position');
          this._isLoading.set(false);
        }
      })
    );
  }

  clearCurrentPortfolio() {
    this._currentPortfolio.set(null);
  }

  clearError() {
    this._error.set(null);
  }
}
