import { ImporterType } from './enums';

export { ImporterType };

export interface ETF {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ETFRequest {
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
}

export interface ETFResponse {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ImportStatistics {
  etfId: number;
  etfName: string;
  allocationVersion: number;
  totalEntries: number;
  newStocksCreated: number;
  newStockIsins: string[];
  newStockNames: string[];
  existingStocks: number;
  warnings: string[];
  success: boolean;
  errorMessage?: string;
}
