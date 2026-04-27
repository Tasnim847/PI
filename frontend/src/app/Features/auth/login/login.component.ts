import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { FaceAuthService } from '../../../services/face-auth.service';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  email: string = '';
  password: string = '';
  isLoading: boolean = false;
  errorMessage: string = '';
  private isBrowser: boolean;

  // Coordonnées GPS
  private clientLat: number | null = null;
  private clientLon: number | null = null;
  private gpsAttempted: boolean = false;

  // Variables pour la reconnaissance faciale
  showFaceModal: boolean = false;
  faceImage: File | null = null;
  facePreview: string | null = null;
  faceLoading: boolean = false;
  faceErrorMessage: string = '';
  isCameraActive: boolean = false;
  private stream: MediaStream | null = null;

  constructor(
    private auth: AuthService,
    private faceAuth: FaceAuthService,
    private router: Router,
    private route: ActivatedRoute,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    // Capture automatique dès l'ouverture de la page
    this.captureGeoPosition();
  }

  // Capture automatique et silencieuse de la position GPS
  private captureGeoPosition() {
    if (!this.isBrowser || !navigator.geolocation) {
      console.log('📍 Géolocalisation non disponible');
      this.gpsAttempted = true;
      return;
    }

    console.log('📍 Tentative de capture GPS...');
    
    // Options optimisées pour une réponse rapide
    const options = {
      enableHighAccuracy: false,  // Plus rapide sans haute précision
      timeout: 5000,              // 5 secondes max
      maximumAge: 60000          // Accepte position récente de 1 minute
    };

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.clientLat = position.coords.latitude;
        this.clientLon = position.coords.longitude;
        this.gpsAttempted = true;
        console.log('✅ GPS capturé automatiquement:', this.clientLat, this.clientLon);
      },
      (error) => {
        this.gpsAttempted = true;
        console.warn('⚠️ Échec capture GPS automatique:', error.message);
        // Pas d'UI, on continue silencieusement
      },
      options
    );
  }

  // ========== LOGIN ==========
  onLogin() {
    if (!this.email || !this.password) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    // Attendre un peu si le GPS n'a pas encore répondu (max 1 seconde)
    const sendLogin = () => {
      const loginData: any = {
        email: this.email,
        password: this.password
      };

      // Envoyer les coordonnées GPS uniquement si on les a
      if (this.clientLat !== null && this.clientLon !== null) {
        loginData.clientLat = this.clientLat;
        loginData.clientLon = this.clientLon;
        console.log('📍 Envoi GPS au backend:', this.clientLat, this.clientLon);
      } else {
        console.log('📍 Pas de GPS disponible, backend utilisera IP');
      }

      this.auth.login(loginData).subscribe({
        next: (res) => {
          this.handleLoginResponse(res);
        },
        error: (err) => {
          this.isLoading = false;
          if (err.status === 401) {
            this.errorMessage = 'Email ou mot de passe incorrect';
          } else if (err.status === 403) {
            this.errorMessage = 'Compte bloqué. Réessayez dans 2 minutes.';
          } else {
            this.errorMessage = err.error?.message || 'Une erreur est survenue';
          }
        }
      });
    };

    // Si le GPS est déjà tenté, on envoie directement
    if (this.gpsAttempted) {
      sendLogin();
    } else {
      // Sinon on attend un peu que le GPS réponde (max 1.5 secondes)
      console.log('⏳ Attente réponse GPS...');
      setTimeout(() => {
        sendLogin();
      }, 1500);
    }
  }

  // ========== RECONNAISSANCE FACIALE ==========
  openFaceModal() {
    this.showFaceModal = true;
    this.faceErrorMessage = '';
    this.faceImage = null;
    this.facePreview = null;
    this.startCamera();
  }

  closeFaceModal() {
    this.showFaceModal = false;
    this.stopCamera();
  }

  async startCamera() {
    if (!this.isBrowser) return;

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user' }
      });
      this.isCameraActive = true;

      setTimeout(() => {
        const video = document.getElementById('faceVideo') as HTMLVideoElement;
        if (video && this.stream) {
          video.srcObject = this.stream;
        }
      }, 100);
    } catch (err) {
      console.error('Erreur caméra:', err);
      this.faceErrorMessage = "Impossible d'accéder à la caméra";
    }
  }

  stopCamera() {
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }
    this.isCameraActive = false;
  }

  captureFace() {
    const video = document.getElementById('faceVideo') as HTMLVideoElement;
    const canvas = document.getElementById('faceCanvas') as HTMLCanvasElement;

    if (!video || !canvas) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);

    this.facePreview = canvas.toDataURL('image/jpeg');
    this.faceImage = this.dataURLtoFile(this.facePreview, 'face-capture.jpg');
    this.stopCamera();
  }

  retakeFace() {
    this.facePreview = null;
    this.faceImage = null;
    this.faceErrorMessage = '';
    this.startCamera();
  }

  submitFaceLogin() {
    if (!this.faceImage) {
      this.faceErrorMessage = 'Veuillez capturer votre visage';
      return;
    }

    this.faceLoading = true;
    this.faceErrorMessage = '';

    this.faceAuth.loginWithFace(this.faceImage).subscribe({
      next: (res) => {
        this.faceLoading = false;
        this.closeFaceModal();
        this.handleLoginResponse(res);
      },
      error: (err) => {
        this.faceLoading = false;
        this.faceErrorMessage = err.error || 'Visage non reconnu. Veuillez réessayer.';
      }
    });
  }

  // ========== GESTION DE LA RÉPONSE ==========
  private handleLoginResponse(res: any) {
    this.isLoading = false;
    const token = res.token;

    if (!token) {
      this.errorMessage = 'Token non reçu du serveur';
      return;
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));

      const firstName = payload.firstName || payload.name || payload.firstname || '';
      const lastName = payload.lastName || payload.lastname || '';
      const role = payload.role || payload.authorities?.[0] || 'CLIENT';
      const userEmail = payload.sub || payload.email || this.email;
      let userId = payload.id || payload.userId || payload.user_id || null;

      if (this.isBrowser) {
        localStorage.setItem('token', token);
        localStorage.setItem('role', role);
        localStorage.setItem('firstName', firstName);
        localStorage.setItem('lastName', lastName);
        localStorage.setItem('userEmail', userEmail);

        if (userId) {
          localStorage.setItem('userId', userId.toString());
        }

        this.auth.saveSession(token, role, userId, firstName, lastName, userEmail);
      }

      if (!userId && this.isBrowser) {
        this.auth.getMe().subscribe({
          next: (user) => {
            if (user && user.id) {
              localStorage.setItem('userId', user.id.toString());
            }
            this.redirectBasedOnRole(role);
          },
          error: () => this.redirectBasedOnRole(role)
        });
      } else {
        this.redirectBasedOnRole(role);
      }
    } catch (error) {
      console.error('Error parsing token:', error);
      this.errorMessage = 'Erreur lors du traitement de la connexion';
      this.isLoading = false;
    }
  }

  private redirectBasedOnRole(role: string) {
    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/backoffice']);
        break;
      case 'AGENT_ASSURANCE':
        this.router.navigate(['/public/agent/home']);
        break;
      case 'AGENT_FINANCE':
        this.router.navigate(['/public/home']);
        break;
      case 'CLIENT':
        this.router.navigate(['/public/home']);
        break;
      default:
        this.router.navigate(['/public/home']);
        break;
    }
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

  goForgotPassword() {
    this.router.navigate(['/forgot-password']);
  }

  goRegister() {
    this.router.navigate(['/register']);
  }
}