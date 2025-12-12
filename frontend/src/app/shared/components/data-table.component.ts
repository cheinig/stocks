import { Component, Input, Output, EventEmitter, ContentChild, TemplateRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { IconComponent } from './icon.component';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
}

export interface TableAction<T> {
  icon: string;
  tooltip: string;
  color?: string;
  action: (item: T) => void;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    IconComponent,
    MatTooltipModule
  ],
  template: `
    <div class="table-container">
      <table mat-table [dataSource]="data" matSort (matSortChange)="onSortChange($event)">
        @for (column of columns; track column.key) {
          <ng-container [matColumnDef]="column.key">
            <th mat-header-cell *matHeaderCellDef [mat-sort-header]="column.sortable ? column.key : ''">
              {{ column.label }}
            </th>
            <td mat-cell *matCellDef="let row">
              @if (cellTemplate) {
                <ng-container *ngTemplateOutlet="cellTemplate; context: { $implicit: row, column: column.key }"></ng-container>
              } @else {
                {{ getNestedValue(row, column.key) }}
              }
            </td>
          </ng-container>
        }

        @if (actions && actions.length > 0) {
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Aktionen</th>
            <td mat-cell *matCellDef="let row">
              @for (action of actions; track action.icon) {
                <button
                  mat-icon-button
                  [color]="action.color || 'primary'"
                  [matTooltip]="action.tooltip"
                  (click)="onActionClick($event, action, row)">
                  <mat-icon [fontIcon]="action.icon"></mat-icon>
                </button>
              }
            </td>
          </ng-container>
        }

        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;" (click)="onRowClick(row)"></tr>
      </table>

      @if (showPaginator) {
        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="pageSizeOptions"
          [pageIndex]="pageIndex"
          (page)="onPageChange($event)">
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .table-container {
      width: 100%;
      overflow: auto;
      background: white;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    table {
      width: 100%;
    }

    th {
      font-weight: 600;
      background-color: #fafafa;
    }

    tr.mat-mdc-row:hover {
      background-color: #f5f5f5;
      cursor: pointer;
    }

    td, th {
      padding: 12px 16px;
    }
  `]
})
export class DataTableComponent<T> {
  @Input() data: T[] = [];
  @Input() columns: TableColumn[] = [];
  @Input() actions?: TableAction<T>[];
  @Input() totalElements = 0;
  @Input() pageSize = 20;
  @Input() pageIndex = 0;
  @Input() pageSizeOptions = [10, 20, 50, 100];
  @Input() showPaginator = true;

  @Output() pageChange = new EventEmitter<PageEvent>();
  @Output() sortChange = new EventEmitter<Sort>();
  @Output() rowClick = new EventEmitter<T>();

  @ContentChild('cellTemplate') cellTemplate?: TemplateRef<any>;

  get displayedColumns(): string[] {
    const cols = this.columns.map(c => c.key);
    if (this.actions && this.actions.length > 0) {
      cols.push('actions');
    }
    return cols;
  }

  getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((acc, part) => acc && acc[part], obj);
  }

  onPageChange(event: PageEvent): void {
    this.pageChange.emit(event);
  }

  onSortChange(event: Sort): void {
    this.sortChange.emit(event);
  }

  onRowClick(row: T): void {
    this.rowClick.emit(row);
  }

  onActionClick(event: Event, action: TableAction<T>, row: T): void {
    event.stopPropagation(); // Prevent row click event from firing
    action.action(row);
  }
}
