import { Component, OnInit, PLATFORM_ID, Inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreditService, Credit } from '../../../services/credit.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

export interface Repayment {
  id: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  status: string;
}

export interface RemainingAmount {
  creditId: number;
  remainingAmount: number;
  currency: string;
}

@Component({
  selector: 'app-client-repayment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client-repayment.component.html',
  styleUrl: './client-repayment.component.css'
})
export class ClientRepaymentComponent implements OnInit {
  // ========== DONNÉES ==========
  creditId: number | null = null;
  credit: Credit | null = null;
  myRepayments: Repayment[] = [];
  remainingAmount: number = 0;
  
  // ========== FORMULAIRE ==========
  repaymentAmount: number = 0;
  paymentMethod: string = 'CASH';
  
  // ========== ÉTATS ==========
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  private apiUrl = 'http://localhost:8081/Repayment';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private creditService: CreditService,
    private http: HttpClient,
    private toastr: ToastrService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.route.params.subscribe(params => {
        this.creditId = parseInt(params['id'], 10); // Convertir en nombre
        console.log('Credit ID from route:', this.creditId, 'Type:', typeof this.creditId);
        if (this.creditId) {
          this.loadCreditDetails();
          this.loadMyPayments();
          this.loadRemainingAmount();
        }
      });
    }
  }

  // ========== CHARGEMENT DES DÉTAILS DU CRÉDIT (CLIENT) ==========
  loadCreditDetails(): void {
    if (!this.creditId) return;
    
    this.isLoading = true;
    this.cdr.markForCheck();
    console.log('Début chargement crédit avec ID:', this.creditId, 'Type:', typeof this.creditId);
    
    // Charger tous les crédits du client et trouver celui avec l'ID
    this.creditService.getMyCredits().subscribe({
      next: (credits) => {
        console.log('Tous les crédits du client:', credits);
        console.log('Recherche du crédit avec ID:', this.creditId);
        
        // Afficher tous les IDs pour déboguer
        credits.forEach(c => {
          console.log('Crédit ID:', c.creditId, 'Type:', typeof c.creditId, 'Égal?', c.creditId === this.creditId);
        });
        
        this.credit = credits.find(c => c.creditId === this.creditId) || null;
        if (this.credit) {
          this.repaymentAmount = this.credit.monthlyPayment;
          console.log('Crédit trouvé:', this.credit);
        } else {
          this.errorMessage = 'Crédit non trouvé';
          console.error('Crédit avec ID', this.creditId, 'non trouvé dans la liste');
        }
        this.isLoading = false;
        this.cdr.markForCheck();
        console.log('isLoading:', this.isLoading, 'credit:', this.credit);
      },
      error: (err) => {
        console.error('Erreur chargement crédits:', err);
        this.errorMessage = 'Erreur lors du chargement du crédit';
        this.toastr.error(this.errorMessage);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== CHARGER MES PAIEMENTS (CLIENT) ==========
  loadMyPayments(): void {
    const headers = this.getHeaders();
    console.log('Appel à /myPayments avec headers:', headers);
    
    this.http.get<Repayment[]>(`${this.apiUrl}/myPayments`, {
      headers: headers
    }).subscribe({
      next: (data) => {
        console.log('Paiements reçus:', data);
        this.myRepayments = data || [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Erreur chargement paiements:', err);
        console.error('Status:', err.status);
        console.error('Message:', err.error?.message);
        // Ne pas afficher d'erreur si c'est juste qu'il n'y a pas de paiements
        if (err.status !== 403 && err.status !== 401) {
          this.myRepayments = [];
        }
        this.cdr.markForCheck();
      }
    });
  }

  // ========== CHARGER LE MONTANT RESTANT ==========
  loadRemainingAmount(): void {
    if (!this.creditId) return;

    const headers = this.getHeaders();
    this.http.get<RemainingAmount>(`${this.apiUrl}/remaining/${this.creditId}`, {
      headers: headers
    }).subscribe({
      next: (data) => {
        console.log('Montant restant:', data);
        this.remainingAmount = data.remainingAmount;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Erreur montant restant:', err);
        this.remainingAmount = 0;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== PAYER UN CRÉDIT (CLIENT) ==========
  submitRepayment(): void {
    if (!this.creditId || !this.credit) {
      this.errorMessage = 'Crédit non trouvé';
      this.toastr.error(this.errorMessage);
      return;
    }

    if (this.repaymentAmount <= 0) {
      this.errorMessage = 'Le montant doit être supérieur à 0';
      this.toastr.error(this.errorMessage);
      return;
    }

    if (this.repaymentAmount > this.credit.monthlyPayment * 1.5) {
      this.errorMessage = 'Le montant dépasse le maximum autorisé';
      this.toastr.error(this.errorMessage);
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.markForCheck();

    const repaymentData = {
      amount: this.repaymentAmount,
      paymentMethod: this.paymentMethod
    };

    console.log('Envoi remboursement:', repaymentData);

    this.http.post(`${this.apiUrl}/pay-credit/${this.creditId}`, repaymentData, {
      headers: this.getHeaders()
    }).subscribe({
      next: (response) => {
        console.log('Remboursement réussi:', response);
        this.successMessage = 'Remboursement effectué avec succès';
        this.toastr.success(this.successMessage);
        this.repaymentAmount = this.credit!.monthlyPayment;
        this.isSubmitting = false;
        this.cdr.markForCheck();
        // Recharger les données
        setTimeout(() => {
          this.loadMyPayments();
          this.loadRemainingAmount();
          this.loadCreditDetails();
        }, 500);
      },
      error: (err) => {
        console.error('Erreur remboursement:', err);
        this.errorMessage = err.error?.message || 'Erreur lors du remboursement';
        this.toastr.error(this.errorMessage);
        this.isSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== NAVIGATION ==========
  goBack(): void {
    this.router.navigate(['/public/credit']);
  }

  // ========== UTILITAIRES ==========
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    console.log('Token:', token ? 'Présent' : 'Absent');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }

  formatDate(date: string | Date): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR');
  }

  getRemainingAmount(): number {
    if (!this.credit) return 0;
    return this.credit.monthlyPayment - this.repaymentAmount;
  }

  isRepaymentValid(): boolean {
    return this.repaymentAmount > 0 && this.repaymentAmount <= (this.credit?.monthlyPayment || 0) * 1.5;
  }

  getStatusBadgeClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED':
      case 'SUCCESS':
        return 'bg-success';
      case 'PENDING':
        return 'bg-warning';
      case 'FAILED':
      case 'REJECTED':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }
}
