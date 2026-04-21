import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { FaceAuthService } from '../../services/face-auth.service';
import { WebcamComponent } from '../webcam/webcam.component';

@Component({
  selector: 'app-face-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, WebcamComponent],
  templateUrl: './face-login.component.html',
  styleUrls: ['./face-login.component.css']
})
export class FaceLoginComponent {
  loginMethod: 'classic' | 'face' = 'classic';
  email = '';
  password = '';
  isLoading = false;
  errorMessage = '';
  faceErrorMessage = '';
  faceRegistered = false;
  capturedFace: File | null = null;
  faceCheckEmail: string = '';

  private isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private faceAuthService: FaceAuthService,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  setMethod(method: 'classic' | 'face') {
    this.loginMethod = method;
    this.errorMessage = '';
    this.faceErrorMessage = '';
    if (method === 'face') {
      this.promptEmailForFace();
    }
  }

  promptEmailForFace() {
    const email = prompt('Veuillez entrer votre email pour la reconnaissance faciale:');
    if (email && email.includes('@')) {
      this.faceCheckEmail = email;
      this.checkFaceStatusByEmail(email);
    } else {
      this.faceErrorMessage = 'Email invalide. Veuillez réessayer.';
      setTimeout(() => {
        this.loginMethod = 'classic';
      }, 2000);
    }
  }

  checkFaceStatusByEmail(email: string) {
    this.faceAuthService.checkFaceExists(email).subscribe({
      next: (res) => {
        this.faceRegistered = res.hasFaceRegistered;
        if (!this.faceRegistered) {
          this.faceErrorMessage = '⚠️ Aucun visage enregistré pour ce compte. Veuillez d\'abord vous connecter avec email/mot de passe et enregistrer votre visage.';
        } else {
          this.faceErrorMessage = '';
        }
      },
      error: () => {
        this.faceRegistered = false;
        this.faceErrorMessage = '⚠️ Aucun visage enregistré pour ce compte.';
      }
    });
  }

  onClassicLogin() {
    if (!this.email || !this.password) {
      this.errorMessage = 'Veuillez remplir tous les champs';
      return;
    }

    this.isLoading = true;
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (res) => {
        this.handleLoginResponse(res);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Email ou mot de passe incorrect';
      }
    });
  }

  onFaceCapture(file: File) {
    this.capturedFace = file;
    this.faceErrorMessage = '';
  }

  submitFaceLogin() {
    if (!this.capturedFace) {
      this.faceErrorMessage = 'Veuillez capturer votre visage';
      return;
    }

    if (!this.faceRegistered) {
      this.faceErrorMessage = 'Aucun visage enregistré. Veuillez d\'abord vous connecter avec email/mot de passe.';
      return;
    }

    this.isLoading = true;
    this.faceAuthService.loginWithFace(this.capturedFace).subscribe({
      next: (res) => {
        this.handleLoginResponse(res);
      },
      error: (err) => {
        this.isLoading = false;
        this.faceErrorMessage = err.error || 'Visage non reconnu';
      }
    });
  }

  private handleLoginResponse(res: any) {
    this.isLoading = false;

    if (this.isBrowser) {
      localStorage.setItem('token', res.token);
      localStorage.setItem('role', res.role);
      localStorage.setItem('userId', res.userId?.toString() || '');
      localStorage.setItem('firstName', res.firstName || '');
      localStorage.setItem('lastName', res.lastName || '');
      localStorage.setItem('userEmail', res.email || this.email || this.faceCheckEmail);
    }

    this.authService.saveSession(
      res.token,
      res.role,
      res.userId,
      res.firstName,
      res.lastName,
      this.email || this.faceCheckEmail
    );

    this.redirectBasedOnRole(res.role);
  }

  private redirectBasedOnRole(role: string) {
    if (role === 'ADMIN') {
      this.router.navigate(['/backoffice']);
    } else {
      this.router.navigate(['/public/home']);
    }
  }

  goRegister() {
    this.router.navigate(['/register']);
  }
}
