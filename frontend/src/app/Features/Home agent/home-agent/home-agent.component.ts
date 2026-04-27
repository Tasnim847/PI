import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ContractService } from '../../Insurance/services/contract.service';
import { ClaimsService } from '../../Claims/services/claims.service';
import { ClientService } from '../../Insurance/services/client.service';

interface InsuranceContract {
  contractId: number;
  startDate: Date;
  endDate: Date;
  premium: number;
  status: string;
  client?: {
    firstName: string;
    lastName: string;
    email: string;
  };
}

interface Claim {
  claimId: number;
  claimDate: Date;
  claimedAmount: number;
  status: string;
  client?: {
    firstName: string;
    lastName: string;
    email: string;
  };
}

interface Client {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
  createdAt?: Date;
  currentRiskLevel?: string;
}

@Component({
  selector: 'app-home-agent',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './home-agent.component.html',
  styleUrls: ['./home-agent.component.css']
})
export class HomeAgentComponent implements OnInit {
  firstName: string = '';
  lastName: string = '';
  userEmail: string = '';
  userRole: string = '';
  currentDate: string = '';

  // Data collections
  contracts: InsuranceContract[] = [];
  recentContracts: InsuranceContract[] = [];
  clients: Client[] = [];
  recentClients: Client[] = [];
  allClaims: Claim[] = [];
  pendingClaimsList: Claim[] = [];

  // Stats
  pendingClaims: number = 0;
  approvedClaims: number = 0;

  constructor(
    private auth: AuthService,
    private router: Router,
    private contractService: ContractService,
    private claimsService: ClaimsService,
    private clientService: ClientService
  ) {}

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
      return;
    }

    this.loadUserInfo();
    this.setCurrentDate();
    this.loadAgentData();
  }

  setCurrentDate(): void {
    const now = new Date();
    this.currentDate = now.toLocaleDateString('en-US', { 
      weekday: 'long', 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  }

  loadUserInfo(): void {
    const user = this.auth.getCurrentUser();
    if (user) {
      this.firstName = user.firstName;
      this.lastName = user.lastName;
      this.userEmail = user.email;
      this.userRole = user.role;
    } else {
      const token = localStorage.getItem('token');
      if (token) {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          this.firstName = payload.firstName || 'Agent';
          this.lastName = payload.lastName || '';
          this.userEmail = payload.sub || '';
          this.userRole = payload.role || 'AGENT_ASSURANCE';
        } catch (e) {
          console.error('Token error', e);
        }
      }
    }
  }

  loadAgentData(): void {
    const agentId = this.auth.getUserId();
    if (!agentId) return;

    // Load all contracts (l'agent peut voir tous les contrats)
    this.contractService.getAllContracts().subscribe({
      next: (data) => {
        this.contracts = data;
        this.recentContracts = this.contracts.slice(0, 5);
      },
      error: (err) => console.error('Error loading contracts', err)
    });

    // Load all clients
    this.clientService.getAllClients().subscribe({
      next: (data) => {
        this.clients = data;
        this.recentClients = this.clients.slice(0, 5);
      },
      error: (err) => console.error('Error loading clients', err)
    });

    // Load all claims using getAllClaims()
    this.claimsService.getAllClaims().subscribe({
      next: (data) => {
        this.allClaims = data;
        // Filtrer les claims en fonction du statut
        this.pendingClaimsList = this.allClaims.filter(c => c.status === 'PENDING');
        this.pendingClaims = this.pendingClaimsList.length;
        this.approvedClaims = this.allClaims.filter(c => c.status === 'APPROVED' || c.status === 'PAID').length;
      },
      error: (err) => console.error('Error loading claims', err)
    });
  }

  // Navigation methods
  goToContracts(): void {
    this.router.navigate(['/public/agent/contracts']);
  }

  goToClaims(): void {
    this.router.navigate(['/public/agent/claims']);
  }

  goToCashApprovals(): void {
    this.router.navigate(['/public/agent/cash-approvals']);
  }

 

  viewContract(contractId: number): void {
    this.router.navigate(['/public/agent/contracts', contractId]);
  }

  // Helper methods
  getInitials(firstName: string, lastName: string): string {
    return (firstName?.charAt(0) || '') + (lastName?.charAt(0) || '');
  }
}