// insurance-router.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { MyContractsComponent } from './pages/client/my-contracts/my-contracts.component';
import { AgentContractsComponent } from './pages/agent/agent-contracts/agent-contracts.component';

@Component({
  selector: 'app-insurance-router',
  standalone: true,
  imports: [CommonModule, MyContractsComponent, AgentContractsComponent], // ✅ AJOUTÉ DANS IMPORTS
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

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    const role = this.authService.getRole();
    this.isClient = role === 'CLIENT';
    this.isAgent = role === 'AGENT_ASSURANCE';
    this.isLoading = false;

    // Redirection si rôle invalide
    if (!this.isClient && !this.isAgent) {
      // Rediriger vers home ou login
      window.location.href = '/public/home';
    }
  }
}