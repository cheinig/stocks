export interface ETFAllocation {
  id: number;
  etfId: number;
  stockId: number;
  percentage: number;
  version: number;
  uploadedAt: string;
  stockName?: string;
  stockIsin?: string;
}

export interface AggregatedStockAllocation {
  stockId: number;
  stockName: string;
  isin: string;
  country: string;
  sector: string;
  totalQuantity: number;
  totalValue: number;
  percentage: number;
  sources: AllocationSource[];
}

export interface AllocationSource {
  type: 'DIRECT' | 'ETF';
  quantity: number;
  value: number;
  etfName?: string;
}
