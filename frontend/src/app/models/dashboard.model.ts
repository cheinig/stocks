export interface SectorAllocation {
  sector: string;
  percentage: number;
  value: number;
}

export interface CountryAllocation {
  country: string;
  percentage: number;
  value: number;
}

export interface PortfolioAnalysis {
  totalValue: number;
  sectorAllocations: SectorAllocation[];
  countryAllocations: CountryAllocation[];
}
