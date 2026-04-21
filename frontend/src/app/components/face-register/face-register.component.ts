import { Component, ElementRef, ViewChild, Inject, PLATFORM_ID, OnInit } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { FaceAuthService } from '../../services/face-auth.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-face-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './face-register.component.html',
  styleUrls: ['./face-register.component.css']
})
export class FaceRegisterComponent implements OnInit {
  @ViewChild('video') videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas') canvasElement!: ElementRef<HTMLCanvasElement>;

  isCameraActive = false;
  previewImage: string | null = null;
  isLoading = false;
  statusMessage = '';
  isSuccess = false;
  isLoggedIn = false;
  private stream: MediaStream | null = null;

  constructor(
    private faceAuth: FaceAuthService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.isLoggedIn = true;

    // Démarrer la caméra automatiquement
    setTimeout(() => {
      this.startCamera();
    }, 500);
  }
  async startCamera() {
    if (!this.auth.isLoggedIn()) {
      this.statusMessage = '❌ Session expirée';
      this.router.navigate(['/login']);
      return;
    }

    try {
      this.statusMessage = '📷 Demande d\'accès à la caméra...';

      this.stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'user',
          width: { ideal: 640 },
          height: { ideal: 480 }
        }
      });

      // Attendre que le DOM soit prêt
      setTimeout(() => {
        if (this.videoElement?.nativeElement) {
          this.videoElement.nativeElement.srcObject = this.stream;
          this.videoElement.nativeElement.onloadedmetadata = () => {
            this.videoElement.nativeElement.play();
            this.isCameraActive = true;
            this.statusMessage = '✅ Caméra active - Regardez l\'objectif';
            // Forcer la détection de changement
            setTimeout(() => {}, 100);
          };
        }
      }, 100);

    } catch (err: any) {
      console.error('Erreur caméra:', err);
      this.statusMessage = `❌ Erreur: ${err.message || 'Impossible d\'accéder à la caméra'}`;
      this.isSuccess = false;
    }
  }

  capture() {
    if (!this.videoElement?.nativeElement) return;

    const video = this.videoElement.nativeElement;
    const canvas = this.canvasElement.nativeElement;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);

    this.previewImage = canvas.toDataURL('image/jpeg');
    this.stopCamera();
    this.statusMessage = '✅ Photo capturée ! Vérifiez et enregistrez.';
  }

  retake() {
    this.previewImage = null;
    this.startCamera();
  }

  registerFace() {
    if (!this.previewImage) return;

    if (!this.auth.isLoggedIn()) {
      this.statusMessage = '❌ Session expirée';
      this.router.navigate(['/login']);
      return;
    }

    this.isLoading = true;
    const file = this.dataURLtoFile(this.previewImage, 'face-registration.jpg');

    this.faceAuth.registerFace(file).subscribe({
      next: () => {
        this.isLoading = false;
        this.statusMessage = '✅ Visage enregistré avec succès !';
        this.isSuccess = true;
        setTimeout(() => {
          this.router.navigate(['/profile']);
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401) {
          this.statusMessage = '❌ Session expirée. Veuillez vous reconnecter.';
          setTimeout(() => this.router.navigate(['/login']), 2000);
        } else {
          this.statusMessage = err.error || '❌ Erreur lors de l\'enregistrement';
        }
        this.isSuccess = false;
      }
    });
  }

  goBack() {
    this.stopCamera();
    this.router.navigate(['/profile']);
  }

  private stopCamera() {
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
