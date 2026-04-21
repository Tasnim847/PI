import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-dashboard-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-navbar.component.html',
  styleUrls: ['./dashboard-navbar.component.css']
})
export class DashboardNavbarComponent implements OnInit {
  @Input() sidebarCollapsed: boolean = false;

  userName: string = '';
  userRole: string = '';
  userEmail: string = '';
  userAvatar: string = '';
  firstName: string = '';
  lastName: string = '';
  profilePhoto: string = '';

  private isBrowser: boolean;

  constructor(
    private auth: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = typeof window !== 'undefined' && typeof localStorage !== 'undefined';
  }

  ngOnInit() {
    if (this.isBrowser) {
      this.loadUserData();
      this.loadProfilePhoto();
    }
  }

  loadUserData() {
    // ✅ Only access localStorage in browser
    if (!this.isBrowser) return;

    // Récupérer les données depuis localStorage
    const firstName = localStorage.getItem('firstName') || '';
    const lastName = localStorage.getItem('lastName') || '';
    const role = localStorage.getItem('role') || 'CLIENT';
    const email = localStorage.getItem('userEmail') || '';

    this.firstName = firstName;
    this.lastName = lastName;
    this.userName = `${firstName} ${lastName}`.trim() || 'Utilisateur';
    this.userRole = this.getRoleLabel(role);
    this.userEmail = email;

    // Avatar par défaut avec les initiales (si pas de photo)
    this.updateDefaultAvatar();
  }

  loadProfilePhoto() {
    // ✅ Only run in browser
    if (!this.isBrowser) return;

    const savedPhoto = localStorage.getItem('profilePhoto');
    if (savedPhoto && savedPhoto.startsWith('http')) {
      this.profilePhoto = savedPhoto;
      this.userAvatar = savedPhoto;
    } else {
      this.auth.getMe().subscribe({
        next: (user) => {
          if (user && user.photo) {
            this.profilePhoto = `http://localhost:8083/uploads/${user.photo}`;
            this.userAvatar = this.profilePhoto;
            if (this.isBrowser) {
              localStorage.setItem('profilePhoto', this.profilePhoto);
            }
          } else {
            this.updateDefaultAvatar();
          }
        },
        error: (err) => {
          console.error('Erreur chargement photo:', err);
          this.updateDefaultAvatar();
        }
      });
    }
  }

  updateDefaultAvatar() {
    const initials = `${this.firstName.charAt(0)}${this.lastName.charAt(0) || 'U'}`;
    this.userAvatar = `https://ui-avatars.com/api/?name=${initials}&background=1a5ac3&color=fff&rounded=true&bold=true&length=2`;
  }

  getRoleLabel(role: string): string {
    const roles: { [key: string]: string } = {
      'ADMIN': 'Administrateur',
      'CLIENT': 'Client',
      'AGENT_FINANCE': 'Agent Finance',
      'AGENT_ASSURANCE': 'Agent Assurance'
    };
    return roles[role] || role;
  }

  logout(event: Event) {
    event.stopPropagation();
    this.auth.logout();
    this.router.navigate(['/']);
    if (this.isBrowser) {
      setTimeout(() => {
        window.location.reload();
      }, 100);
    }
  }
}
