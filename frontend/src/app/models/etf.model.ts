import { ImporterType } from './enums';
import { CountryAllocation, SectorAllocation } from './dashboard.model';

export { ImporterType };

export interface ETF {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
  tickerSymbol?: string;
  hasLogo?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ETFRequest {
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
  tickerSymbol?: string;
}

export interface ETFResponse {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  webUrl?: string;
  webDataId?: string;
  tickerSymbol?: string;
  hasLogo?: boolean;
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

export interface ETFStatistics {
  etfId: number;
  etfName: string;
  totalStocks: number;
  totalCountries: number;
  countryAllocations: CountryAllocation[];
  sectorAllocations: SectorAllocation[];
}
