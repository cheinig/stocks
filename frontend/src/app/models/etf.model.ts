import { ImporterType } from './enums';

export interface ETF {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  createdAt?: string;
  updatedAt?: string;
}

export interface ETFRequest {
  name: string;
  isin: string;
  importerType: ImporterType;
}

export interface ETFResponse {
  id: number;
  name: string;
  isin: string;
  importerType: ImporterType;
  createdAt: string;
  updatedAt: string;
}
