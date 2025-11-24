export interface ETFAllocation {
  id: number;
  etfId: number;
  etfName?: string;
  stockId: number;
  stockIsin?: string;
  stockName?: string;
  percentage: number;
  allocationVersion: number;
  uploadDate: string;
  // Legacy support
  stock?: {
    id: number;
    isin: string;
    name: string;
  };
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
