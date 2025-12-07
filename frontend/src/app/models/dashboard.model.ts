export interface AggregatedStockAllocation {
  stockId: number;
  hasLogo: boolean;
  isin: string;
  name: string;
  country: string;
  sector: string;
  totalPercentage: number;
  directPercentage: number;
  etfPercentage: number;
  etfCount: number;
}

export interface CountryAllocation {
  countryCode: string;
  percentage: number;
  stockCount: number;
}

export interface SectorAllocation {
  sector: string;
  percentage: number;
  stockCount: number;
}

export interface PortfolioAnalysis {
  portfolioId: number;
  portfolioName: string;
  allStocks: AggregatedStockAllocation[];
  top20Stocks: AggregatedStockAllocation[];
  countryAllocations: CountryAllocation[];
  totalUniqueStocks: number;
  totalCountries: number;
}
