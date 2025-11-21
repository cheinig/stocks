import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function isinValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (!value) {
      return null; // Don't validate empty values (use Validators.required for that)
    }

    // ISIN format: 2 letters (country code) + 9 alphanumeric + 1 check digit
    const isinPattern = /^[A-Z]{2}[A-Z0-9]{9}[0-9]$/;

    if (!isinPattern.test(value)) {
      return { isinFormat: { value } };
    }

    // Validate check digit using Luhn algorithm
    if (!validateIsinCheckDigit(value)) {
      return { isinCheckDigit: { value } };
    }

    return null;
  };
}

function validateIsinCheckDigit(isin: string): boolean {
  // Convert letters to numbers (A=10, B=11, ..., Z=35)
  let digits = '';
  for (const char of isin.substring(0, 11)) {
    if (char >= '0' && char <= '9') {
      digits += char;
    } else {
      digits += (char.charCodeAt(0) - 55).toString();
    }
  }

  // Apply Luhn algorithm
  let sum = 0;
  let doubleDigit = digits.length % 2 === 0;

  for (const char of digits) {
    let digit = parseInt(char, 10);

    if (doubleDigit) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }

    sum += digit;
    doubleDigit = !doubleDigit;
  }

  const checkDigit = (10 - (sum % 10)) % 10;
  return checkDigit === parseInt(isin[11], 10);
}
