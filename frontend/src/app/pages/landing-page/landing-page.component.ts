import { Component, HostListener, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, NavigationEnd } from '@angular/router';
import { RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { RegisterComponent } from '../../Features/auth/register/register.component';
import { LoginComponent } from '../../Features/auth/login/login.component';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterModule,
    LoginComponent,
    RegisterComponent
  ],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css']
})
export class LandingPageComponent implements OnDestroy {
  isScrolled = false;
  showLoginPopup = false;
  showRegisterPopup = false;
  private routerSubscription: Subscription | null = null; // Initialisé à null
  private isBrowser: boolean;

  services = [
    {
      icon: 'fas fa-credit-card',
      title: 'Credits',
      description: 'Get your credits quickly with competitive rates and a simplified process.'
    },
    {
      icon: 'fas fa-shield-alt',
      title: 'Insurance',
      description: 'Protect your assets with our personalized insurance solutions.'
    },
    {
      icon: 'fas fa-chart-line',
      title: 'Financial Management',
      description: 'Track and manage your finances in real-time from a single platform.'
    },
    {
      icon: 'fas fa-headset',
      title: '24/7 Support',
      description: 'A dedicated team available to assist you at any time.'
    }
  ];

  features = [
    {
      icon: 'fas fa-bolt',
      title: 'Fast & Efficient',
      description: 'Instant processing of your requests'
    },
    {
      icon: 'fas fa-lock',
      title: 'Secure',
      description: 'Your data is protected with the best technologies'
    },
    {
      icon: 'fas fa-mobile-alt',
      title: 'Mobile Friendly',
      description: 'Access your services from anywhere'
    }
  ];

  contactData = {
    name: '',
    email: '',
    message: ''
  };

  newsletterEmail = '';

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    
    // S'abonner aux changements de route uniquement dans le navigateur
    if (this.isBrowser) {
      this.routerSubscription = this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe((event: NavigationEnd) => {
        // Afficher le popup selon la route
        if (event.url === '/login') {
          this.showLoginPopup = true;
          this.showRegisterPopup = false;
          // Empêcher le scroll du body
          document.body.style.overflow = 'hidden';
        } else if (event.url === '/register') {
          this.showRegisterPopup = true;
          this.showLoginPopup = false;
          document.body.style.overflow = 'hidden';
        } else {
          this.showLoginPopup = false;
          this.showRegisterPopup = false;
          document.body.style.overflow = '';
        }
      });
    }
  }

  ngOnDestroy() {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    // Restaurer le scroll uniquement dans le navigateur
    if (this.isBrowser) {
      document.body.style.overflow = '';
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    if (this.isBrowser) {
      this.isScrolled = window.scrollY > 50;
    }
  }

  navigateToLogin() {
    if (this.isBrowser) {
      this.router.navigate(['/login']);
    }
  }

  navigateToRegister() {
    if (this.isBrowser) {
      this.router.navigate(['/register']);
    }
  }

  closePopup() {
    if (this.isBrowser) {
      this.router.navigate(['/']);
    }
  }

  scrollToServices() {
    if (this.isBrowser) {
      document.getElementById('services')?.scrollIntoView({ behavior: 'smooth' });
    }
  }

  toggleMobileMenu() {
    if (this.isBrowser) {
      const navLinks = document.querySelector('.nav-links');
      navLinks?.classList.toggle('active');
    }
  }

  submitContactForm() {
    console.log('Contact form submitted:', this.contactData);
    if (this.isBrowser) {
      alert('Message sent successfully!');
    }
    this.contactData = { name: '', email: '', message: '' };
  }

  subscribeNewsletter() {
    if (this.newsletterEmail) {
      console.log('Newsletter subscription:', this.newsletterEmail);
      if (this.isBrowser) {
        alert('Subscription successful!');
      }
      this.newsletterEmail = '';
    }
  }
}