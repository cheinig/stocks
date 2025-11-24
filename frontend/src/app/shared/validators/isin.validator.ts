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

    // Note: ISIN check digit validation is complex and varies by implementation.
    // We rely on backend validation for check digit correctness.
    // Frontend only validates the format to provide immediate feedback.

    return null;
  };
}
