import { AssetType } from './enums';

export interface Portfolio {
  id: number;
  name: string;
  userId: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PortfolioRequest {
  name: string;
  userId: string;
}

export interface PortfolioResponse {
  id: number;
  name: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export interface PortfolioPosition {
  id: number;
  portfolioId: number;
  assetType: AssetType;
  assetId: number;
  quantity: number;
  assetName?: string;
  assetIsin?: string;
  hasLogo?: boolean;
}

export interface PortfolioPositionRequest {
  assetType: AssetType;
  assetId: number;
  quantity: number;
}

export interface PortfolioWithPositions {
  id: number;
  name: string;
  userId: string;
  positions: PortfolioPosition[];
  createdAt: string;
  updatedAt: string;
}
