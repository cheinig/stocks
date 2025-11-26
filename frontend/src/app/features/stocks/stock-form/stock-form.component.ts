import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { StockStateService } from '../../../core/services/stock-state.service';
import { StockRequest } from '../../../models/stock.model';
import { isinValidator } from '../../../shared/validators/isin.validator';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner.component';
import { ErrorMessageComponent } from '../../../shared/components/error-message.component';
import { IconComponent } from '../../../shared/components/icon.component';

@Component({
  selector: 'app-stock-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    IconComponent,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatCardModule,
    MatSnackBarModule,
    LoadingSpinnerComponent,
    ErrorMessageComponent
  ],
  template: `
    <div class="stock-form-container">
      <div class="header">
        <button mat-icon-button (click)="cancel()">
          <mat-icon fontIcon="arrow_back"></mat-icon>
        </button>
        <h1>{{ isEditMode() ? 'Aktie bearbeiten' : 'Neue Aktie erstellen' }}</h1>
      </div>

      @if (stockState.isLoading() && isEditMode()) {
        <app-loading-spinner></app-loading-spinner>
      } @else if (stockState.error() && isEditMode()) {
        <app-error-message
          [title]="'Fehler beim Laden der Aktie'"
          [message]="stockState.error() || ''"
          [onRetry]="loadStock.bind(this)">
        </app-error-message>
      } @else {
        <mat-card>
          <mat-card-content>
            <form [formGroup]="stockForm" (ngSubmit)="onSubmit()">
              <div class="form-grid">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Name</mat-label>
                  <input matInput formControlName="name" placeholder="z.B. Apple Inc.">
                  @if (stockForm.get('name')?.hasError('required') && stockForm.get('name')?.touched) {
                    <mat-error>Name ist erforderlich</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>ISIN</mat-label>
                  <input matInput formControlName="isin" placeholder="z.B. US0378331005">
                  @if (stockForm.get('isin')?.hasError('required') && stockForm.get('isin')?.touched) {
                    <mat-error>ISIN ist erforderlich</mat-error>
                  }
                  @if (stockForm.get('isin')?.hasError('isinFormat') && stockForm.get('isin')?.touched) {
                    <mat-error>Ungültiges ISIN-Format (2 Buchstaben + 9 Zeichen + 1 Prüfziffer)</mat-error>
                  }
                  @if (stockForm.get('isin')?.hasError('isinCheckDigit') && stockForm.get('isin')?.touched) {
                    <mat-error>Ungültige ISIN-Prüfziffer</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Land</mat-label>
                  <input matInput
                         formControlName="country"
                         [matAutocomplete]="countryAuto"
                         (input)="onCountryInput($event)"
                         placeholder="Land suchen...">
                  <mat-autocomplete #countryAuto="matAutocomplete" [displayWith]="displayCountry.bind(this)">
                    @for (country of filteredCountries(); track country.code) {
                      <mat-option [value]="country.code">
                        {{ country.name }} ({{ country.code }})
                      </mat-option>
                    }
                  </mat-autocomplete>
                  @if (stockForm.get('country')?.hasError('required') && stockForm.get('country')?.touched) {
                    <mat-error>Land ist erforderlich</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Branche</mat-label>
                  <mat-select formControlName="sector">
                    @for (sector of sectors; track sector) {
                      <mat-option [value]="sector">
                        {{ sector }}
                      </mat-option>
                    }
                  </mat-select>
                  @if (stockForm.get('sector')?.hasError('required') && stockForm.get('sector')?.touched) {
                    <mat-error>Branche ist erforderlich</mat-error>
                  }
                </mat-form-field>
              </div>

              <div class="form-actions">
                <button type="button" mat-button (click)="cancel()">
                  Abbrechen
                </button>
                <button type="submit" mat-raised-button color="primary" [disabled]="stockForm.invalid || saving()">
                  @if (saving()) {
                    Speichert...
                  } @else {
                    Speichern
                  }
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [`
    .stock-form-container {
      width: 100%;
      max-width: 800px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
    }

    h1 {
      margin: 0;
      font-size: 2rem;
      font-weight: 500;
    }

    mat-card {
      padding: 1rem;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    .full-width {
      grid-column: 1 / -1;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      padding-top: 1rem;
      border-top: 1px solid rgba(0, 0, 0, 0.12);
    }

    .form-actions button mat-icon {
      margin-right: 0.5rem;
    }

    @media (max-width: 768px) {
      .form-grid {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column-reverse;
      }

      .form-actions button {
        width: 100%;
      }
    }
  `]
})
export class StockFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);
  stockState = inject(StockStateService);

  isEditMode = signal(false);
  saving = signal(false);
  stockId?: number;

  stockForm: FormGroup;
  countryFilter = signal('');

  countries = [
    { code: 'AD', name: 'Andorra' },
    { code: 'AE', name: 'Vereinigte Arabische Emirate' },
    { code: 'AF', name: 'Afghanistan' },
    { code: 'AG', name: 'Antigua und Barbuda' },
    { code: 'AI', name: 'Anguilla' },
    { code: 'AL', name: 'Albanien' },
    { code: 'AM', name: 'Armenien' },
    { code: 'AO', name: 'Angola' },
    { code: 'AQ', name: 'Antarktis' },
    { code: 'AR', name: 'Argentinien' },
    { code: 'AS', name: 'Amerikanisch-Samoa' },
    { code: 'AT', name: 'Österreich' },
    { code: 'AU', name: 'Australien' },
    { code: 'AW', name: 'Aruba' },
    { code: 'AX', name: 'Åland' },
    { code: 'AZ', name: 'Aserbaidschan' },
    { code: 'BA', name: 'Bosnien und Herzegowina' },
    { code: 'BB', name: 'Barbados' },
    { code: 'BD', name: 'Bangladesch' },
    { code: 'BE', name: 'Belgien' },
    { code: 'BF', name: 'Burkina Faso' },
    { code: 'BG', name: 'Bulgarien' },
    { code: 'BH', name: 'Bahrain' },
    { code: 'BI', name: 'Burundi' },
    { code: 'BJ', name: 'Benin' },
    { code: 'BL', name: 'Saint-Barthélemy' },
    { code: 'BM', name: 'Bermuda' },
    { code: 'BN', name: 'Brunei' },
    { code: 'BO', name: 'Bolivien' },
    { code: 'BQ', name: 'Bonaire' },
    { code: 'BR', name: 'Brasilien' },
    { code: 'BS', name: 'Bahamas' },
    { code: 'BT', name: 'Bhutan' },
    { code: 'BV', name: 'Bouvetinsel' },
    { code: 'BW', name: 'Botswana' },
    { code: 'BY', name: 'Belarus' },
    { code: 'BZ', name: 'Belize' },
    { code: 'CA', name: 'Kanada' },
    { code: 'CC', name: 'Kokosinseln' },
    { code: 'CD', name: 'Kongo (Demokratische Republik)' },
    { code: 'CF', name: 'Zentralafrikanische Republik' },
    { code: 'CG', name: 'Kongo (Republik)' },
    { code: 'CH', name: 'Schweiz' },
    { code: 'CI', name: 'Elfenbeinküste' },
    { code: 'CK', name: 'Cookinseln' },
    { code: 'CL', name: 'Chile' },
    { code: 'CM', name: 'Kamerun' },
    { code: 'CN', name: 'China' },
    { code: 'CO', name: 'Kolumbien' },
    { code: 'CR', name: 'Costa Rica' },
    { code: 'CU', name: 'Kuba' },
    { code: 'CV', name: 'Kap Verde' },
    { code: 'CW', name: 'Curaçao' },
    { code: 'CX', name: 'Weihnachtsinsel' },
    { code: 'CY', name: 'Zypern' },
    { code: 'CZ', name: 'Tschechien' },
    { code: 'DE', name: 'Deutschland' },
    { code: 'DJ', name: 'Dschibuti' },
    { code: 'DK', name: 'Dänemark' },
    { code: 'DM', name: 'Dominica' },
    { code: 'DO', name: 'Dominikanische Republik' },
    { code: 'DZ', name: 'Algerien' },
    { code: 'EC', name: 'Ecuador' },
    { code: 'EE', name: 'Estland' },
    { code: 'EG', name: 'Ägypten' },
    { code: 'EH', name: 'Westsahara' },
    { code: 'ER', name: 'Eritrea' },
    { code: 'ES', name: 'Spanien' },
    { code: 'ET', name: 'Äthiopien' },
    { code: 'FI', name: 'Finnland' },
    { code: 'FJ', name: 'Fidschi' },
    { code: 'FK', name: 'Falklandinseln' },
    { code: 'FM', name: 'Mikronesien' },
    { code: 'FO', name: 'Färöer' },
    { code: 'FR', name: 'Frankreich' },
    { code: 'GA', name: 'Gabun' },
    { code: 'GB', name: 'Vereinigtes Königreich' },
    { code: 'GD', name: 'Grenada' },
    { code: 'GE', name: 'Georgien' },
    { code: 'GF', name: 'Französisch-Guayana' },
    { code: 'GG', name: 'Guernsey' },
    { code: 'GH', name: 'Ghana' },
    { code: 'GI', name: 'Gibraltar' },
    { code: 'GL', name: 'Grönland' },
    { code: 'GM', name: 'Gambia' },
    { code: 'GN', name: 'Guinea' },
    { code: 'GP', name: 'Guadeloupe' },
    { code: 'GQ', name: 'Äquatorialguinea' },
    { code: 'GR', name: 'Griechenland' },
    { code: 'GS', name: 'Südgeorgien und die Südlichen Sandwichinseln' },
    { code: 'GT', name: 'Guatemala' },
    { code: 'GU', name: 'Guam' },
    { code: 'GW', name: 'Guinea-Bissau' },
    { code: 'GY', name: 'Guyana' },
    { code: 'HK', name: 'Hongkong' },
    { code: 'HM', name: 'Heard und McDonaldinseln' },
    { code: 'HN', name: 'Honduras' },
    { code: 'HR', name: 'Kroatien' },
    { code: 'HT', name: 'Haiti' },
    { code: 'HU', name: 'Ungarn' },
    { code: 'ID', name: 'Indonesien' },
    { code: 'IE', name: 'Irland' },
    { code: 'IL', name: 'Israel' },
    { code: 'IM', name: 'Isle of Man' },
    { code: 'IN', name: 'Indien' },
    { code: 'IO', name: 'Britisches Territorium im Indischen Ozean' },
    { code: 'IQ', name: 'Irak' },
    { code: 'IR', name: 'Iran' },
    { code: 'IS', name: 'Island' },
    { code: 'IT', name: 'Italien' },
    { code: 'JE', name: 'Jersey' },
    { code: 'JM', name: 'Jamaika' },
    { code: 'JO', name: 'Jordanien' },
    { code: 'JP', name: 'Japan' },
    { code: 'KE', name: 'Kenia' },
    { code: 'KG', name: 'Kirgisistan' },
    { code: 'KH', name: 'Kambodscha' },
    { code: 'KI', name: 'Kiribati' },
    { code: 'KM', name: 'Komoren' },
    { code: 'KN', name: 'St. Kitts und Nevis' },
    { code: 'KP', name: 'Nordkorea' },
    { code: 'KR', name: 'Südkorea' },
    { code: 'KW', name: 'Kuwait' },
    { code: 'KY', name: 'Kaimaninseln' },
    { code: 'KZ', name: 'Kasachstan' },
    { code: 'LA', name: 'Laos' },
    { code: 'LB', name: 'Libanon' },
    { code: 'LC', name: 'St. Lucia' },
    { code: 'LI', name: 'Liechtenstein' },
    { code: 'LK', name: 'Sri Lanka' },
    { code: 'LR', name: 'Liberia' },
    { code: 'LS', name: 'Lesotho' },
    { code: 'LT', name: 'Litauen' },
    { code: 'LU', name: 'Luxemburg' },
    { code: 'LV', name: 'Lettland' },
    { code: 'LY', name: 'Libyen' },
    { code: 'MA', name: 'Marokko' },
    { code: 'MC', name: 'Monaco' },
    { code: 'MD', name: 'Moldau' },
    { code: 'ME', name: 'Montenegro' },
    { code: 'MF', name: 'Saint-Martin' },
    { code: 'MG', name: 'Madagaskar' },
    { code: 'MH', name: 'Marshallinseln' },
    { code: 'MK', name: 'Nordmazedonien' },
    { code: 'ML', name: 'Mali' },
    { code: 'MM', name: 'Myanmar' },
    { code: 'MN', name: 'Mongolei' },
    { code: 'MO', name: 'Macau' },
    { code: 'MP', name: 'Nördliche Marianen' },
    { code: 'MQ', name: 'Martinique' },
    { code: 'MR', name: 'Mauretanien' },
    { code: 'MS', name: 'Montserrat' },
    { code: 'MT', name: 'Malta' },
    { code: 'MU', name: 'Mauritius' },
    { code: 'MV', name: 'Malediven' },
    { code: 'MW', name: 'Malawi' },
    { code: 'MX', name: 'Mexiko' },
    { code: 'MY', name: 'Malaysia' },
    { code: 'MZ', name: 'Mosambik' },
    { code: 'NA', name: 'Namibia' },
    { code: 'NC', name: 'Neukaledonien' },
    { code: 'NE', name: 'Niger' },
    { code: 'NF', name: 'Norfolkinsel' },
    { code: 'NG', name: 'Nigeria' },
    { code: 'NI', name: 'Nicaragua' },
    { code: 'NL', name: 'Niederlande' },
    { code: 'NO', name: 'Norwegen' },
    { code: 'NP', name: 'Nepal' },
    { code: 'NR', name: 'Nauru' },
    { code: 'NU', name: 'Niue' },
    { code: 'NZ', name: 'Neuseeland' },
    { code: 'OM', name: 'Oman' },
    { code: 'PA', name: 'Panama' },
    { code: 'PE', name: 'Peru' },
    { code: 'PF', name: 'Französisch-Polynesien' },
    { code: 'PG', name: 'Papua-Neuguinea' },
    { code: 'PH', name: 'Philippinen' },
    { code: 'PK', name: 'Pakistan' },
    { code: 'PL', name: 'Polen' },
    { code: 'PM', name: 'Saint-Pierre und Miquelon' },
    { code: 'PN', name: 'Pitcairninseln' },
    { code: 'PR', name: 'Puerto Rico' },
    { code: 'PS', name: 'Palästina' },
    { code: 'PT', name: 'Portugal' },
    { code: 'PW', name: 'Palau' },
    { code: 'PY', name: 'Paraguay' },
    { code: 'QA', name: 'Katar' },
    { code: 'RE', name: 'Réunion' },
    { code: 'RO', name: 'Rumänien' },
    { code: 'RS', name: 'Serbien' },
    { code: 'RU', name: 'Russland' },
    { code: 'RW', name: 'Ruanda' },
    { code: 'SA', name: 'Saudi-Arabien' },
    { code: 'SB', name: 'Salomonen' },
    { code: 'SC', name: 'Seychellen' },
    { code: 'SD', name: 'Sudan' },
    { code: 'SE', name: 'Schweden' },
    { code: 'SG', name: 'Singapur' },
    { code: 'SH', name: 'St. Helena' },
    { code: 'SI', name: 'Slowenien' },
    { code: 'SJ', name: 'Svalbard und Jan Mayen' },
    { code: 'SK', name: 'Slowakei' },
    { code: 'SL', name: 'Sierra Leone' },
    { code: 'SM', name: 'San Marino' },
    { code: 'SN', name: 'Senegal' },
    { code: 'SO', name: 'Somalia' },
    { code: 'SR', name: 'Suriname' },
    { code: 'SS', name: 'Südsudan' },
    { code: 'ST', name: 'São Tomé und Príncipe' },
    { code: 'SV', name: 'El Salvador' },
    { code: 'SX', name: 'Sint Maarten' },
    { code: 'SY', name: 'Syrien' },
    { code: 'SZ', name: 'Eswatini' },
    { code: 'TC', name: 'Turks- und Caicosinseln' },
    { code: 'TD', name: 'Tschad' },
    { code: 'TF', name: 'Französische Süd- und Antarktisgebiete' },
    { code: 'TG', name: 'Togo' },
    { code: 'TH', name: 'Thailand' },
    { code: 'TJ', name: 'Tadschikistan' },
    { code: 'TK', name: 'Tokelau' },
    { code: 'TL', name: 'Osttimor' },
    { code: 'TM', name: 'Turkmenistan' },
    { code: 'TN', name: 'Tunesien' },
    { code: 'TO', name: 'Tonga' },
    { code: 'TR', name: 'Türkei' },
    { code: 'TT', name: 'Trinidad und Tobago' },
    { code: 'TV', name: 'Tuvalu' },
    { code: 'TW', name: 'Taiwan' },
    { code: 'TZ', name: 'Tansania' },
    { code: 'UA', name: 'Ukraine' },
    { code: 'UG', name: 'Uganda' },
    { code: 'UM', name: 'United States Minor Outlying Islands' },
    { code: 'US', name: 'USA' },
    { code: 'UY', name: 'Uruguay' },
    { code: 'UZ', name: 'Usbekistan' },
    { code: 'VA', name: 'Vatikanstadt' },
    { code: 'VC', name: 'St. Vincent und die Grenadinen' },
    { code: 'VE', name: 'Venezuela' },
    { code: 'VG', name: 'Britische Jungferninseln' },
    { code: 'VI', name: 'Amerikanische Jungferninseln' },
    { code: 'VN', name: 'Vietnam' },
    { code: 'VU', name: 'Vanuatu' },
    { code: 'WF', name: 'Wallis und Futuna' },
    { code: 'WS', name: 'Samoa' },
    { code: 'XX', name: 'Unbekannt' },
    { code: 'YE', name: 'Jemen' },
    { code: 'YT', name: 'Mayotte' },
    { code: 'ZA', name: 'Südafrika' },
    { code: 'ZM', name: 'Sambia' },
    { code: 'ZW', name: 'Simbabwe' }
  ];

  filteredCountries = computed(() => {
    const filter = this.countryFilter().toLowerCase();
    if (!filter) {
      return this.countries;
    }
    return this.countries.filter(country =>
      country.name.toLowerCase().includes(filter) ||
      country.code.toLowerCase().includes(filter)
    );
  });

  // GICS (Global Industry Classification Standard) Sektoren
  sectors = [
    'Communication Services',
    'Consumer Discretionary',
    'Consumer Staples',
    'Energy',
    'Financials',
    'Health Care',
    'Industrials',
    'Information Technology',
    'Materials',
    'Real Estate',
    'Utilities',
    'Unbekannt'
  ];

  constructor() {
    this.stockForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      isin: ['', [Validators.required, isinValidator()]],
      country: ['', Validators.required],
      sector: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.stockId = parseInt(id, 10);
      this.isEditMode.set(true);
      this.loadStock();
    }
  }

  loadStock(): void {
    if (!this.stockId) return;

    this.stockState.loadStockById(this.stockId).subscribe({
      next: () => {
        const stock = this.stockState.currentStock();
        if (stock) {
          this.stockForm.patchValue({
            name: stock.name,
            isin: stock.isin,
            country: stock.country,
            sector: stock.sector
          });
        }
      },
      error: () => {
        this.snackBar.open('Fehler beim Laden der Aktie', 'OK', { duration: 3000 });
      }
    });
  }

  onSubmit(): void {
    if (this.stockForm.invalid) {
      this.stockForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const request: StockRequest = this.stockForm.value;

    const operation = this.isEditMode() && this.stockId
      ? this.stockState.updateStock(this.stockId, request)
      : this.stockState.createStock(request);

    operation.subscribe({
      next: () => {
        const message = this.isEditMode()
          ? 'Aktie erfolgreich aktualisiert'
          : 'Aktie erfolgreich erstellt';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.router.navigate(['/stocks']);
      },
      error: () => {
        const message = this.isEditMode()
          ? 'Fehler beim Aktualisieren der Aktie'
          : 'Fehler beim Erstellen der Aktie';
        this.snackBar.open(message, 'OK', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/stocks']);
  }

  onCountryInput(event: Event): void {
    const input = (event.target as HTMLInputElement).value;
    this.countryFilter.set(input);
  }

  displayCountry(countryCode: string): string {
    const country = this.countries.find(c => c.code === countryCode);
    return country ? country.name : countryCode;
  }
}
