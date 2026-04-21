import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OtpService } from '../../../services/otp.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {

  step = 1;
  email = '';
  otp = '';
  newPassword = '';
  isLoading = false;
  message = '';
  error = '';

  constructor(
    private otpService: OtpService,
    private router: Router
  ) {}

  sendOtp() {
    if (!this.email) {
      this.error = 'Veuillez saisir votre email';
      return;
    }

    this.isLoading = true;
    this.error = '';
    this.message = '';

    this.otpService.sendOtp(this.email).subscribe({
      next: (response) => {
        console.log("SUCCESS - Réponse brute:", response);

        // La réponse est une chaîne de texte comme "OTP sent to email"
        // On peut afficher un message personnalisé
        this.message = 'OTP envoyé à votre email avec succès !';
        this.step = 2;
        this.isLoading = false;
      },
      error: (err) => {
        console.log("ERROR détaillé:", err);

        // Gérer l'erreur plus précisément
        if (err.error && typeof err.error === 'string') {
          // Si l'erreur est une chaîne de texte
          this.error = err.error || 'Erreur lors de l\'envoi OTP';
        } else if (err.status === 200) {
          // Si le statut est 200 mais qu'il y a une erreur de parsing
          this.message = 'OTP envoyé avec succès !';
          this.step = 2;
        } else {
          this.error = err.error?.message || 'Erreur lors de l\'envoi OTP';
        }

        this.isLoading = false;
      }
    });
  }

  verifyOtp() {
    if (!this.otp || !this.newPassword) {
      this.error = 'Remplir tous les champs';
      return;
    }

    this.isLoading = true;
    this.error = '';
    this.message = '';

    this.otpService.verifyOtp({
      email: this.email,
      otp: this.otp,
      newPassword: this.newPassword
    }).subscribe({
      next: (response) => {
        console.log("Verification success:", response);
        this.message = 'Mot de passe modifié avec succès';
        this.step = 3;
        this.isLoading = false;

        // redirection vers login après 2 secondes
        setTimeout(() => {
          this.router.navigate(['/']);
          // Fermer le popup si nécessaire
          this.closePopup();
        }, 2000);
      },
      error: (err) => {
        console.log("Verification error:", err);

        if (err.error && typeof err.error === 'string') {
          this.error = err.error || 'OTP invalide ou expiré';
        } else {
          this.error = err.error?.message || 'OTP invalide ou expiré';
        }

        this.isLoading = false;
      }
    });
  }

  // Méthode pour revenir à la page de login (flèche retour)
  goBackToLogin() {

    this.router.navigate(['/login']);


  }

  // Méthode pour fermer le popup
  closePopup() {
    // Émettre un événement ou naviguer
    this.router.navigate(['/']);
  }
}