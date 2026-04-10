import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './landing-page.component.html',
  styleUrls: ['./landing-page.component.css']
})
export class LandingPageComponent {
  isScrolled = false;

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

  constructor(private router: Router) {}

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }

  navigateToLogin() {
    console.log('Login clicked');
    this.router.navigate(['/public/login']);
  }
  scrollToServices() {
    document.getElementById('services')?.scrollIntoView({ behavior: 'smooth' });
  }

  toggleMobileMenu() {
    const navLinks = document.querySelector('.nav-links');
    navLinks?.classList.toggle('active');
  }

  submitContactForm() {
    console.log('Contact form submitted:', this.contactData);
    alert('Message sent successfully!');
    this.contactData = { name: '', email: '', message: '' };
  }

  subscribeNewsletter() {
    if (this.newsletterEmail) {
      console.log('Newsletter subscription:', this.newsletterEmail);
      alert('Subscription successful!');
      this.newsletterEmail = '';
    }
  }
}
