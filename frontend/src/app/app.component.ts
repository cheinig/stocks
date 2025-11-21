import { Component } from '@angular/core';
import { MainLayoutComponent } from './core/layout/main-layout.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MainLayoutComponent],
  template: '<app-main-layout></app-main-layout>'
})
export class AppComponent {
  title = 'Stock-Status';
}
