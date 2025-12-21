import { Directive, ElementRef, Input, OnInit, Renderer2, inject } from '@angular/core';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: 'mat-icon',
  standalone: true
})
export class IconDirective implements OnInit {
  private el = inject(ElementRef);
  private renderer = inject(Renderer2);

  @Input() set matIcon(value: string) {
    this.icon = value;
    this.updateIcon();
  }

  private icon = '';

  ngOnInit(): void {
    this.updateIcon();
  }

  private updateIcon(): void {
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
      'insert_drive_file': 'file-earmark',
      'error': 'x-circle',
      'account_circle': 'person-circle',
      'logout': 'box-arrow-right'
    };

    const bootstrapIcon = iconMap[this.icon] || this.icon;

    // Clear existing content
    this.el.nativeElement.textContent = '';

    // Add Bootstrap Icon classes
    this.renderer.addClass(this.el.nativeElement, 'bi');
    this.renderer.addClass(this.el.nativeElement, `bi-${bootstrapIcon}`);

    // Style adjustments
    this.renderer.setStyle(this.el.nativeElement, 'display', 'inline-flex');
    this.renderer.setStyle(this.el.nativeElement, 'align-items', 'center');
    this.renderer.setStyle(this.el.nativeElement, 'justify-content', 'center');
  }
}
