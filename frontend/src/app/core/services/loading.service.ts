import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingCount = 0;
  private readonly _isLoading = signal<boolean>(false);

  readonly isLoading = this._isLoading.asReadonly();

  show(): void {
    this.loadingCount++;
    if (this.loadingCount > 0) {
      this._isLoading.set(true);
    }
  }

  hide(): void {
    this.loadingCount--;
    if (this.loadingCount <= 0) {
      this.loadingCount = 0;
      this._isLoading.set(false);
    }
  }
}
