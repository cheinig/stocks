import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function positiveNumberValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (value === null || value === undefined || value === '') {
      return null; // Don't validate empty values
    }

    const numValue = Number(value);

    if (isNaN(numValue)) {
      return { notANumber: { value } };
    }

    if (numValue <= 0) {
      return { notPositive: { value } };
    }

    return null;
  };
}
