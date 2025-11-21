import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'countryName',
  standalone: true
})
export class CountryNamePipe implements PipeTransform {
  private countryNames: Record<string, string> = {
    'US': 'USA',
    'DE': 'Deutschland',
    'GB': 'Großbritannien',
    'FR': 'Frankreich',
    'IT': 'Italien',
    'ES': 'Spanien',
    'NL': 'Niederlande',
    'BE': 'Belgien',
    'CH': 'Schweiz',
    'AT': 'Österreich',
    'SE': 'Schweden',
    'NO': 'Norwegen',
    'DK': 'Dänemark',
    'FI': 'Finnland',
    'IE': 'Irland',
    'PT': 'Portugal',
    'GR': 'Griechenland',
    'PL': 'Polen',
    'CZ': 'Tschechien',
    'HU': 'Ungarn',
    'JP': 'Japan',
    'CN': 'China',
    'KR': 'Südkorea',
    'IN': 'Indien',
    'AU': 'Australien',
    'CA': 'Kanada',
    'BR': 'Brasilien',
    'MX': 'Mexiko',
    'AR': 'Argentinien',
    'ZA': 'Südafrika',
    'RU': 'Russland',
    'TR': 'Türkei',
    'SA': 'Saudi-Arabien',
    'AE': 'Vereinigte Arabische Emirate',
    'SG': 'Singapur',
    'HK': 'Hongkong',
    'TW': 'Taiwan',
    'TH': 'Thailand',
    'ID': 'Indonesien',
    'MY': 'Malaysia',
    'PH': 'Philippinen',
    'VN': 'Vietnam'
  };

  transform(countryCode: string | null | undefined): string {
    if (!countryCode) {
      return '';
    }
    return this.countryNames[countryCode.toUpperCase()] || countryCode;
  }
}
