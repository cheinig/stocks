import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Ein unbekannter Fehler ist aufgetreten';

      if (error.error instanceof ErrorEvent) {
        // Client-seitiger Fehler
        errorMessage = `Fehler: ${error.error.message}`;
      } else {
        // Server-seitiger Fehler
        switch (error.status) {
          case 400:
            errorMessage = 'Ungültige Anfrage';
            break;
          case 401:
            errorMessage = 'Nicht autorisiert';
            break;
          case 403:
            errorMessage = 'Zugriff verweigert';
            break;
          case 404:
            errorMessage = 'Ressource nicht gefunden';
            break;
          case 500:
            errorMessage = 'Interner Serverfehler';
            break;
          default:
            errorMessage = `Fehler ${error.status}: ${error.message}`;
        }

        // Falls der Server eine detaillierte Fehlermeldung liefert
        if (error.error?.message) {
          errorMessage = error.error.message;
        }
      }

      console.error('HTTP Error:', errorMessage, error);

      return throwError(() => ({
        status: error.status,
        message: errorMessage,
        originalError: error
      }));
    })
  );
};
