import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent  implements OnInit{
  firstName: string = '';
    lastName: string = '';
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
  
      // Récupérer les infos de l'utilisateur
      this.loadUserInfo();
    }
  
    loadUserInfo() {
      const firstName = localStorage.getItem('firstName');
      const lastName = localStorage.getItem('lastName');
  
      this.firstName = firstName ?? '';
      this.lastName = lastName ?? '';
  
      // Si localStorage vide → fallback token
      if (!this.firstName && !this.lastName) {
        const token = localStorage.getItem('token');
  
        if (token) {
          try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            console.log('localStorage firstName:', firstName);
            console.log('localStorage lastName:', lastName);
            console.log('JWT payload:', payload);
  
            this.firstName = payload.firstName;
            this.lastName = payload.lastName;
  
  
          } catch (e) {
            console.error('Token error', e);
            this.firstName = 'Client';
            this.lastName = '';
          }
        } else {
          this.firstName = 'Client';
          this.lastName = '';
        }
      }
    }
  
    logout() {
      this.auth.logout();
      this.router.navigate(['/']);
    }

}