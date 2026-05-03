import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MyContractsComponent } from './pages/client/my-contracts/my-contracts.component';
import { AgentContractsComponent } from './pages/agent/agent-contracts/agent-contracts.component';

@Component({
  selector: 'app-insurance-router',
  standalone: true,
  imports: [CommonModule, MyContractsComponent, AgentContractsComponent],
  template: `
    <div *ngIf="isLoading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Chargement...</span>
      </div>
    </div>
    <ng-container *ngIf="!isLoading">
      <app-my-contracts *ngIf="isClient"></app-my-contracts>
      <app-agent-contracts *ngIf="isAgent"></app-agent-contracts>
    </ng-container>
  `
})
export class InsuranceRouterComponent implements OnInit {
  isClient: boolean = false;
  isAgent: boolean = false;
  isLoading: boolean = true;

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Vérifier si on est dans le navigateur
    if (isPlatformBrowser(this.platformId)) {
      const role = this.authService.getRole();
      this.isClient = role === 'CLIENT';
      this.isAgent = role === 'AGENT_ASSURANCE';
      this.isLoading = false;

      // Redirection Angular (plus propre que window.location)
      if (!this.isClient && !this.isAgent) {
        this.router.navigate(['/public/home']);
      }
    } else {
      // Environnement SSR - pas de redirection
      this.isLoading = false;
    }
  }
}