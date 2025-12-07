export interface Stock {
  id: number;
  name: string;
  isin: string;
  country: string;
  sector: string;
  hasLogo?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface StockRequest {
  name: string;
  isin: string;
  country: string;
  sector: string;
}

export interface StockResponse {
  id: number;
  name: string;
  isin: string;
  country: string;
  sector: string;
  hasLogo?: boolean;
  createdAt: string;
  updatedAt: string;
}
