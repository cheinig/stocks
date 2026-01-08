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
  XTRACKERS_WEB = 'XTRACKERS_WEB',
  VANECK_WEB = 'VANECK_WEB',
  AMUNDI_WEB = 'AMUNDI_WEB',
  FIDELITY_WEB = 'FIDELITY_WEB'
}

export function isWebImporter(importerType: ImporterType): boolean {
  return importerType === ImporterType.ISHARES_WEB || importerType === ImporterType.XTRACKERS_WEB || importerType === ImporterType.VANECK_WEB || importerType === ImporterType.AMUNDI_WEB || importerType === ImporterType.FIDELITY_WEB;
}

export function requiresWebDataId(importerType: ImporterType): boolean {
  return importerType === ImporterType.ISHARES_WEB;
}

export function requiresTickerSymbol(importerType: ImporterType): boolean {
  return importerType === ImporterType.VANECK_WEB;
}
