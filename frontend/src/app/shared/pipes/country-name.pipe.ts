import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'countryName',
  standalone: true
})
export class CountryNamePipe implements PipeTransform {
  private countryNames: Record<string, string> = {
    // North America
    'US': 'USA',
    'CA': 'Kanada',
    'MX': 'Mexiko',

    // Europe
    'DE': 'Deutschland',
    'FR': 'Frankreich',
    'GB': 'Großbritannien',
    'IT': 'Italien',
    'ES': 'Spanien',
    'NL': 'Niederlande',
    'CH': 'Schweiz',
    'BE': 'Belgien',
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
    'RO': 'Rumänien',
    'LU': 'Luxemburg',

    // Asia
    'JP': 'Japan',
    'CN': 'China',
    'KR': 'Südkorea',
    'IN': 'Indien',
    'SG': 'Singapur',
    'HK': 'Hongkong',
    'TW': 'Taiwan',
    'TH': 'Thailand',
    'ID': 'Indonesien',
    'MY': 'Malaysia',
    'PH': 'Philippinen',
    'VN': 'Vietnam',

    // Middle East
    'TR': 'Türkei',
    'SA': 'Saudi-Arabien',
    'AE': 'Vereinigte Arabische Emirate',
    'IL': 'Israel',
    'KW': 'Kuwait',
    'QA': 'Katar',
    'BH': 'Bahrain',
    'OM': 'Oman',

    // Oceania
    'AU': 'Australien',
    'NZ': 'Neuseeland',

    // South America
    'BR': 'Brasilien',
    'AR': 'Argentinien',
    'CL': 'Chile',
    'CO': 'Kolumbien',
    'PE': 'Peru',

    // Africa
    'ZA': 'Südafrika',
    'EG': 'Ägypten',
    'NG': 'Nigeria',
    'KE': 'Kenia',
    'MA': 'Marokko',

    // Caribbean & Other
    'KY': 'Kaimaninseln',
    'BM': 'Bermuda',
    'VG': 'Britische Jungferninseln',

    // Special
    'RU': 'Russland',
    'XX': 'Unbekannt'
  };

  transform(countryCode: string | null | undefined): string {
    if (!countryCode) {
      return '';
    }
    return this.countryNames[countryCode.toUpperCase()] || countryCode;
  }
}
