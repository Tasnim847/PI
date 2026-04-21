// components/webcam/webcam.component.ts
import { Component, ElementRef, ViewChild, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-webcam',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="webcam-container">
      <video #video [hidden]="!isCameraActive" autoplay playsinline class="webcam-video"></video>
      <canvas #canvas [hidden]="true" class="webcam-canvas"></canvas>

      <div *ngIf="!isCameraActive" class="camera-placeholder">
        <i class="fas fa-camera"></i>
        <p>Caméra inactive</p>
      </div>

      <div class="controls">
        <button *ngIf="!isCameraActive" (click)="startCamera()" type="button" class="btn-camera">
          <i class="fas fa-video"></i> Activer caméra
        </button>

        <button *ngIf="isCameraActive && !isCapturing" (click)="capture()" type="button" class="btn-capture">
          <i class="fas fa-camera"></i> Capturer
        </button>

        <button *ngIf="isCameraActive" (click)="stopCamera()" type="button" class="btn-stop">
          <i class="fas fa-stop"></i> Arrêter
        </button>

        <button *ngIf="previewImage" (click)="reset()" type="button" class="btn-reset">
          <i class="fas fa-redo"></i> Recommencer
        </button>
      </div>

      <div *ngIf="previewImage" class="preview">
        <img [src]="previewImage" alt="Preview" class="preview-img">
      </div>
    </div>
  `,
  styles: [`
    .webcam-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }
    .webcam-video {
      width: 100%;
      max-width: 400px;
      border-radius: 16px;
      background: #000;
    }
    .camera-placeholder {
      width: 400px;
      height: 300px;
      background: #f0f0f0;
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
    }
    .controls {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      justify-content: center;
    }
    button {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
    }
    .btn-camera, .btn-capture { background: #0e3d5c; color: white; }
    .btn-stop { background: #dc2626; color: white; }
    .btn-reset { background: #f59e0b; color: white; }
    .preview-img {
      width: 200px;
      height: 200px;
      object-fit: cover;
      border-radius: 16px;
      border: 3px solid #0e3d5c;
    }
  `]
})
export class WebcamComponent {
  @ViewChild('video') videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas') canvasElement!: ElementRef<HTMLCanvasElement>;

  @Output() onCapture = new EventEmitter<File>();
  @Input() autoCapture: boolean = false;

  isCameraActive = false;
  isCapturing = false;
  previewImage: string | null = null;
  private stream: MediaStream | null = null;

  async startCamera() {
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' }
      });
      this.videoElement.nativeElement.srcObject = this.stream;
      this.isCameraActive = true;
    } catch (err) {
      console.error('Erreur caméra:', err);
    }
  }

  capture(): File | null {
    const video = this.videoElement.nativeElement;
    const canvas = this.canvasElement.nativeElement;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);

    this.previewImage = canvas.toDataURL('image/jpeg');

    // Convertir en File
    return this.dataURLtoFile(this.previewImage, 'face-capture.jpg');
  }

  captureAndEmit() {
    const file = this.capture();
    if (file) {
      this.onCapture.emit(file);
    }
  }

  reset() {
    this.previewImage = null;
  }

  stopCamera() {
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }
    this.isCameraActive = false;
  }

  private dataURLtoFile(dataurl: string, filename: string): File {
    const arr = dataurl.split(',');
    const mime = arr[0].match(/:(.*?);/)?.[1] || 'image/jpeg';
    const bstr = atob(arr[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  }

  ngOnDestroy() {
    this.stopCamera();
  }
}
