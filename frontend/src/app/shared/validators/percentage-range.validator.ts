import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function percentageRangeValidator(min: number = 0, max: number = 100): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (value === null || value === undefined || value === '') {
      return null; // Don't validate empty values
    }

    const numValue = Number(value);

    if (isNaN(numValue)) {
      return { notANumber: { value } };
    }

    if (numValue < min || numValue > max) {
      return { percentageRange: { value, min, max } };
    }

    return null;
  };
}
