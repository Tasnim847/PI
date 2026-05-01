import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent implements OnInit, OnDestroy {
  firstName: string = '';
  lastName: string = '';
  userEmail: string = '';
  userRole: string = '';
  isVisible: boolean = true;
  profilePhoto: string = '';
  private routerSubscription: Subscription;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.checkVisibility(event.url);
    });
  }

  ngOnInit() {
    this.checkVisibility(this.router.url);

    if (!this.auth.isLoggedIn()) {
      this.isVisible = false;
      return;
    }

    const role = this.auth.getRole();
    this.userRole = role ?? '';

    if (this.userRole !== 'CLIENT' && this.userRole !== 'AGENT_ASSURANCE' && this.userRole !== 'AGENT_FINANCE') {
      this.router.navigate(['/public/home']);
      return;
    }

    this.loadUserInfo();
    this.loadProfilePhoto();
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  checkVisibility(url: string): void {
    const publicRoutes = ['/public/login', '/public/register', '/', ''];
    const isPublicRoute = publicRoutes.includes(url) || url === '/';
    this.isVisible = this.auth.isLoggedIn() && !isPublicRoute;
  }

  loadUserInfo() {
    const firstName = localStorage.getItem('firstName');
    const lastName = localStorage.getItem('lastName');
    const email = localStorage.getItem('userEmail');

    this.firstName = firstName ?? '';
    this.lastName = lastName ?? '';
    this.userEmail = email ?? '';

    if (!this.firstName && !this.lastName) {
      const token = localStorage.getItem('token');
      if (token) {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          this.firstName = payload.firstName || '';
          this.lastName = payload.lastName || '';
          this.userEmail = payload.sub || '';
        } catch (e) {
          console.error('Token error', e);
        }
      }
    }
  }

  loadProfilePhoto() {
    const savedPhoto = localStorage.getItem('profilePhoto');
    if (savedPhoto) {
      this.profilePhoto = savedPhoto;
    } else {
      this.auth.getMe().subscribe({
        next: (user) => {
          if (user.photo) {
            this.profilePhoto = `http://localhost:8081/uploads/${user.photo}`;
            localStorage.setItem('profilePhoto', this.profilePhoto);
          }
        },
        error: (err) => {
          console.error('Erreur chargement photo:', err);
        }
      });
    }
  }

  // Méthodes de visibilité
  isCreditVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_FINANCE';
  }

  isInsuranceVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }

  isAccountVisible(): boolean {
  // ✅ Ajouter AGENT_FINANCE
  return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE' || this.userRole === 'AGENT_FINANCE';
}
  isComplaintVisible(): boolean {
    // Tous les rôles voient Complaint
    return this.userRole === 'CLIENT' || 
           this.userRole === 'AGENT_ASSURANCE' || 
           this.userRole === 'AGENT_FINANCE';
}

  isNewsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }

  isProductsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }

  isClaimsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }

  // Ajoutez cette méthode dans la classe NavbarComponent
  getClaimsLink(): string {
    if (this.userRole === 'AGENT_ASSURANCE') {
      return '/public/agent/claims';  // Route pour l'agent
    } else if (this.userRole === 'CLIENT') {
      return '/public/claims';         // Route pour le client
    }
    return '/public/claims'; // Fallback
  }

  isCompensationVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Méthode pour obtenir le bon lien selon le rôle
  getCompensationLink(): string {
    if (this.userRole === 'AGENT_ASSURANCE') {
      return '/public/agent/compensations';  // Page avec toutes les compensations
    } else if (this.userRole === 'CLIENT') {
      return '/public/compensations';         // Page avec ses propres compensations
    }
    return '/public/compensations';
  }

  logout(event: Event) {
    event.stopPropagation();
    this.auth.logout();
    this.router.navigate(['/']);
    setTimeout(() => {
      window.location.reload();
    }, 100);
  }
  // Ajoutez cette méthode

  getComplaintLink(): string {
    if (this.userRole === 'CLIENT') {
        return '/public/my-complaints';
    } else if (this.userRole === 'AGENT_ASSURANCE' || this.userRole === 'AGENT_FINANCE') {
        return '/public/agent/complaints';  // ← Changement ici : ajouter /public/
    }
    return '/public/my-complaints';
  }

   getHomeLink(): string {
    if (this.userRole === 'AGENT_ASSURANCE') {
      return '/public/agent/home';
    } else if (this.userRole === 'CLIENT') {
      return '/public/home';
    } else if (this.userRole === 'AGENT_FINANCE') {
      return '/public/home';
    } else if (this.userRole === 'ADMIN') {
      return '/backoffice';
    }
    return '/public/home';
  }

  // Cash Approvals - visible seulement pour AGENT_ASSURANCE
  isCashApprovalsVisible(): boolean {
    return this.userRole === 'AGENT_ASSURANCE';
  }
  getCreditLink(): string {
    if (this.userRole === 'AGENT_FINANCE') {
      return '/public/agent/credit-approvals';  // Page spécifique pour l'agent finance
    } else if (this.userRole === 'CLIENT') {
      return '/public/credit';  // Page pour le client
    }
    return '/public/credit';  // Fallback
  }
}