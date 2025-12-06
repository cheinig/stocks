export enum AssetType {
  STOCK = 'STOCK',
  ETF = 'ETF'
}

export enum ImporterType {
  FIDELITY = 'FIDELITY',
  XTRACKERS = 'XTRACKERS',
  VANECK = 'VANECK',
  AMUNDI = 'AMUNDI',
  ISHARES_WEB = 'ISHARES_WEB',
  XTRACKERS_WEB = 'XTRACKERS_WEB'
}

export function isWebImporter(importerType: ImporterType): boolean {
  return importerType === ImporterType.ISHARES_WEB || importerType === ImporterType.XTRACKERS_WEB;
}

export function requiresWebDataId(importerType: ImporterType): boolean {
  return importerType === ImporterType.ISHARES_WEB;
}
