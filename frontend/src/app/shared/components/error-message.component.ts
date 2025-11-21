import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-error-message',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  template: `
    <div class="error-container">
      <mat-icon class="error-icon">error_outline</mat-icon>
      <h3 class="error-title">{{ title }}</h3>
      <p class="error-message">{{ message }}</p>
      @if (showRetry) {
        <button mat-raised-button color="primary" (click)="onRetry()">
          <mat-icon>refresh</mat-icon>
          Erneut versuchen
        </button>
      }
    </div>
  `,
  styles: [`
    .error-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      text-align: center;
      min-height: 200px;
    }

    .error-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #f44336;
      margin-bottom: 1rem;
    }

    .error-title {
      margin: 0 0 0.5rem 0;
      color: rgba(0, 0, 0, 0.87);
      font-weight: 500;
    }

    .error-message {
      margin: 0 0 1.5rem 0;
      color: rgba(0, 0, 0, 0.6);
      max-width: 500px;
    }

    button mat-icon {
      margin-right: 0.5rem;
    }
  `]
})
export class ErrorMessageComponent {
  @Input() title = 'Fehler';
  @Input() message = 'Ein Fehler ist aufgetreten. Bitte versuchen Sie es erneut.';
  @Input() showRetry = true;
  @Input() onRetry: () => void = () => {};
}
