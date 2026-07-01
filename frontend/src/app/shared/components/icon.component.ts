import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'mat-icon',
  standalone: true,
  imports: [CommonModule],
  template: `<i [class]="'bi bi-' + bootstrapIcon"></i>`,
  styles: [`
    :host {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      font-size: 20px;
    }

    i {
      line-height: 1;
    }
  `]
})
export class IconComponent {
  @Input() set fontIcon(value: string) {
    this.icon = value;
    this.bootstrapIcon = this.getBootstrapIcon();
  }

  @Input() matListItemIcon: string | undefined;

  private icon = '';
  bootstrapIcon = '';

  private getBootstrapIcon(): string {
    // Map Material Icons to Bootstrap Icons
    const iconMap: Record<string, string> = {
      'menu': 'list',
      'dashboard': 'speedometer2',
      'show_chart': 'graph-up',
      'assessment': 'pie-chart',
      'account_balance_wallet': 'wallet2',
      'add': 'plus-lg',
      'edit': 'pencil',
      'delete': 'trash',
      'search': 'search',
      'close': 'x-lg',
      'arrow_back': 'arrow-left',
      'save': 'check-lg',
      'hourglass_empty': 'hourglass-split',
      'refresh': 'arrow-clockwise',
      'error_outline': 'exclamation-circle',
      'warning': 'exclamation-triangle',
      'help_outline': 'question-circle',
      'cloud_upload': 'cloud-upload',
      'upload_file': 'file-earmark-arrow-up',
      'insert_drive_file': 'file-earmark',
      'check_circle': 'check-circle',
      'error': 'x-circle',
      'account_circle': 'person-circle',
      'logout': 'box-arrow-right'
    };

    // Check if it's already a Bootstrap icon
    if (this.icon && this.icon.startsWith('bi-')) {
      return this.icon.replace('bi-', '');
    }

    return iconMap[this.icon] || this.icon;
  }
}
