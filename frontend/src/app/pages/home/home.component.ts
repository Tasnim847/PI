import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']  // Correction: styleUrls au lieu de styleUrl
})
export class HomeComponent implements OnInit {

  userName: string = '';
  userEmail: string = '';

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    // Vérifier si l'utilisateur est connecté
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
      return;
    }

    // Vérifier le rôle (optionnel)
    const role = this.auth.getRole();
    if (role !== 'CLIENT') {
      this.router.navigate(['/public/home']);
      return;
    }

    // Récupérer les infos de l'utilisateur depuis le token ou localStorage
    this.loadUserInfo();
  }

  loadUserInfo() {
    // Vous pouvez décoder le token JWT pour récupérer email et nom
    const token = localStorage.getItem('token');
    if (token) {
      // Option 1: Décoder le token (si vous voulez extraire les infos)
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.userEmail = payload.email || '';
        this.userName = payload.name || payload.email || '';
      } catch(e) {
        console.error('Error decoding token', e);
      }
    }
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/']);
  }
}
