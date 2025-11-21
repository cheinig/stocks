import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnDestroy, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart-container">
      @if (title()) {
        <h3 class="chart-title">{{ title() }}</h3>
      }
      <canvas #chartCanvas></canvas>
    </div>
  `,
  styles: [`
    .chart-container {
      width: 100%;
      padding: 1rem;
      background: white;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .chart-title {
      margin: 0 0 1rem 0;
      font-size: 1.25rem;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.87);
    }

    canvas {
      max-height: 400px;
    }
  `]
})
export class ChartComponent implements AfterViewInit, OnDestroy {
  @ViewChild('chartCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  title = signal<string>('');
  chartType = signal<ChartType>('bar');
  chartData = signal<ChartConfiguration['data']>({ labels: [], datasets: [] });
  chartOptions = signal<ChartConfiguration['options']>({});

  private chart?: Chart;

  constructor() {
    effect(() => {
      if (this.chart) {
        this.updateChart();
      }
    });
  }

  @Input() set config(value: {
    type?: ChartType;
    data?: ChartConfiguration['data'];
    options?: ChartConfiguration['options'];
    title?: string;
  }) {
    if (value.type) this.chartType.set(value.type);
    if (value.data) this.chartData.set(value.data);
    if (value.options) this.chartOptions.set(value.options);
    if (value.title) this.title.set(value.title);
  }

  ngAfterViewInit(): void {
    this.createChart();
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  private createChart(): void {
    const ctx = this.canvasRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration = {
      type: this.chartType(),
      data: this.chartData(),
      options: {
        responsive: true,
        maintainAspectRatio: true,
        ...this.chartOptions()
      }
    };

    this.chart = new Chart(ctx, config);
  }

  private updateChart(): void {
    if (!this.chart) return;

    this.chart.data = this.chartData();
    this.chart.options = {
      responsive: true,
      maintainAspectRatio: true,
      ...this.chartOptions()
    };
    this.chart.update();
  }
}
