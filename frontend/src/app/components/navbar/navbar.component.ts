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
  private routerSubscription: Subscription;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {
    // S'abonner aux changements de route
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.checkVisibility(event.url);
    });
  }

  ngOnInit() {
    this.checkVisibility(this.router.url);
    
    // Si l'utilisateur n'est pas connecté, ne pas charger les infos
    if (!this.auth.isLoggedIn()) {
      this.isVisible = false;
      return;
    }

    // Récupérer le rôle avec gestion du null
    const role = this.auth.getRole();
    this.userRole = role ?? '';
    
    // Vérifier le rôle
    if (this.userRole !== 'CLIENT' && this.userRole !== 'AGENT_ASSURANCE' && this.userRole !== 'AGENT_FINANCE') {
      this.router.navigate(['/public/home']);
      return;
    }

    // Récupérer les infos de l'utilisateur
    this.loadUserInfo();
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  checkVisibility(url: string): void {
    // Masquer la navbar sur les pages publiques
    const publicRoutes = ['/public/login', '/public/register', '/', ''];
    const isPublicRoute = publicRoutes.includes(url) || url === '/';
    
    // Afficher seulement si l'utilisateur est connecté ET pas sur une page publique
    this.isVisible = this.auth.isLoggedIn() && !isPublicRoute;
    
    // Si la navbar devient invisible, ne pas essayer d'afficher les données
    if (!this.isVisible) {
      return;
    }
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
          this.firstName = payload.firstName || '';
          this.lastName = payload.lastName || '';
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

  // Méthodes de visibilité selon les rôles
  
  // Credit: visible pour CLIENT et AGENT_FINANCE seulement
  isCreditVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_FINANCE';
  }
  
  // Insurance: visible pour CLIENT et AGENT_ASSURANCE seulement
  isInsuranceVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Account: visible pour CLIENT et AGENT_ASSURANCE seulement (PAS pour AGENT_FINANCE)
  isAccountVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Complaint: visible pour CLIENT et AGENT_FINANCE seulement
  isComplaintVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_FINANCE';
  }
  
  // News: visible pour CLIENT et AGENT_ASSURANCE seulement
  isNewsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Products: visible pour CLIENT et AGENT_ASSURANCE seulement
  isProductsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Claims: visible pour CLIENT et AGENT_ASSURANCE seulement
  isClaimsVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }
  
  // Compensation: visible pour CLIENT et AGENT_ASSURANCE seulement
  isCompensationVisible(): boolean {
    return this.userRole === 'CLIENT' || this.userRole === 'AGENT_ASSURANCE';
  }

  logout(event: Event) {
    event.stopPropagation();
    
    // Vider le localStorage
    this.auth.logout();
    
    // Rediriger vers la page d'accueil
    this.router.navigate(['/']);
    
    // Forcer le rechargement de la page pour réinitialiser complètement l'état
    setTimeout(() => {
      window.location.reload();
    }, 100);
  }
}