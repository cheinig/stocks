import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'sectorName',
  standalone: true
})
export class SectorNamePipe implements PipeTransform {
  private sectorNames: Record<string, string> = {
    // GICS Sectors - English to German
    'Information Technology': 'Informationstechnologie',
    'Health Care': 'Gesundheitswesen',
    'Financials': 'Finanzwesen',
    'Consumer Discretionary': 'Zyklische Konsumgüter',
    'Consumer Staples': 'Basiskonsumgüter',
    'Industrials': 'Industrie',
    'Energy': 'Energie',
    'Materials': 'Grundstoffe',
    'Real Estate': 'Immobilien',
    'Communication Services': 'Kommunikationsdienste',
    'Utilities': 'Versorger',
    'Unbekannt': 'Unbekannt'
  };

  transform(sector: string | null | undefined): string {
    if (!sector) {
      return 'Unbekannt';
    }
    return this.sectorNames[sector] || this.sectorNames['Unbekannt'];
  }
}
