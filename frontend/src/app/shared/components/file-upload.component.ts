import { Component, Output, EventEmitter, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule, MatButtonModule, IconComponent, MatProgressBarModule],
  template: `
    <div class="upload-container">
      <div
        class="drop-zone"
        [class.dragover]="isDragOver()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        (drop)="onDrop($event)"
        (click)="fileInput.click()">

        <input
          #fileInput
          type="file"
          [accept]="acceptedTypes"
          [multiple]="multiple"
          (change)="onFileSelected($event)"
          style="display: none;">

        <div class="upload-content">
          <mat-icon class="upload-icon" fontIcon="cloud_upload"></mat-icon>
          <p class="upload-text">
            Datei hierher ziehen oder klicken zum Auswählen
          </p>
          @if (acceptedTypes) {
            <p class="upload-hint">Akzeptierte Dateitypen: {{ acceptedTypes }}</p>
          }
          @if (maxSizeInMB) {
            <p class="upload-hint">Maximale Dateigröße: {{ maxSizeInMB }} MB</p>
          }
        </div>
      </div>

      @if (selectedFile()) {
        <div class="file-info">
          <div class="file-details">
            <mat-icon fontIcon="insert_drive_file"></mat-icon>
            <div class="file-text">
              <p class="file-name">{{ selectedFile()?.name }}</p>
              <p class="file-size">{{ formatFileSize(selectedFile()?.size || 0) }}</p>
            </div>
          </div>
          <button mat-icon-button color="warn" (click)="removeFile()">
            <mat-icon fontIcon="close"></mat-icon>
          </button>
        </div>
      }

      @if (uploading()) {
        <mat-progress-bar mode="indeterminate"></mat-progress-bar>
      }

      @if (uploadError()) {
        <div class="error-message">
          <mat-icon fontIcon="error"></mat-icon>
          <span>{{ uploadError() }}</span>
        </div>
      }

      @if (selectedFile() && !uploading()) {
        <div class="upload-actions">
          <button mat-raised-button color="primary" (click)="upload()">
            <mat-icon fontIcon="cloud_upload"></mat-icon>
            Hochladen
          </button>
          <button mat-button (click)="removeFile()">
            Abbrechen
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .upload-container {
      width: 100%;
    }

    .drop-zone {
      border: 2px dashed #ccc;
      border-radius: 8px;
      padding: 3rem 2rem;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s ease;
      background-color: #fafafa;
    }

    .drop-zone:hover {
      border-color: #1976d2;
      background-color: #f0f7ff;
    }

    .drop-zone.dragover {
      border-color: #1976d2;
      background-color: #e3f2fd;
      transform: scale(1.02);
    }

    .upload-content {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .upload-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #1976d2;
      margin-bottom: 1rem;
    }

    .upload-text {
      margin: 0;
      font-size: 1rem;
      color: rgba(0, 0, 0, 0.87);
      font-weight: 500;
    }

    .upload-hint {
      margin: 0.5rem 0 0 0;
      font-size: 0.875rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .file-info {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 1rem;
      padding: 1rem;
      background-color: #f5f5f5;
      border-radius: 4px;
    }

    .file-details {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .file-text {
      text-align: left;
    }

    .file-name {
      margin: 0;
      font-weight: 500;
      color: rgba(0, 0, 0, 0.87);
    }

    .file-size {
      margin: 0.25rem 0 0 0;
      font-size: 0.875rem;
      color: rgba(0, 0, 0, 0.6);
    }

    mat-progress-bar {
      margin-top: 1rem;
    }

    .error-message {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 1rem;
      padding: 0.75rem;
      background-color: #ffebee;
      color: #c62828;
      border-radius: 4px;
    }

    .upload-actions {
      display: flex;
      gap: 1rem;
      margin-top: 1rem;
      justify-content: flex-end;
    }

    .upload-actions button mat-icon {
      margin-right: 0.5rem;
    }
  `]
})
export class FileUploadComponent {
  @Input() acceptedTypes = '.csv,.xlsx';
  @Input() maxSizeInMB?: number;
  @Input() multiple = false;

  @Output() fileSelected = new EventEmitter<File>();
  @Output() uploadClick = new EventEmitter<File>();

  selectedFile = signal<File | null>(null);
  isDragOver = signal(false);
  uploading = signal(false);
  uploadError = signal<string | null>(null);

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver.set(false);

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  handleFile(file: File): void {
    this.uploadError.set(null);

    if (this.maxSizeInMB && file.size > this.maxSizeInMB * 1024 * 1024) {
      this.uploadError.set(`Datei ist zu groß. Maximale Größe: ${this.maxSizeInMB} MB`);
      return;
    }

    if (this.acceptedTypes) {
      const extension = '.' + file.name.split('.').pop()?.toLowerCase();
      const accepted = this.acceptedTypes.split(',').map(t => t.trim());
      if (!accepted.includes(extension)) {
        this.uploadError.set(`Dateityp nicht akzeptiert. Erlaubt: ${this.acceptedTypes}`);
        return;
      }
    }

    this.selectedFile.set(file);
    this.fileSelected.emit(file);
  }

  removeFile(): void {
    this.selectedFile.set(null);
    this.uploadError.set(null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (file) {
      this.uploadClick.emit(file);
    }
  }

  setUploading(uploading: boolean): void {
    this.uploading.set(uploading);
  }

  setError(error: string | null): void {
    this.uploadError.set(error);
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}
