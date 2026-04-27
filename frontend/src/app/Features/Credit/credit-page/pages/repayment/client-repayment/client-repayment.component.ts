import { Component, OnInit, PLATFORM_ID, Inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreditService, Credit } from '../../../../services/credit.service';
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

export interface PaymentSchedule {
  trancheNumber: number;
  dueDate: Date;
  amount: number;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
  isPaid: boolean;
}

export interface AutoRepaymentPlan {
  totalTranches: number;
  monthlyAmount: number;
  startDate: Date;
  endDate: Date;
  schedule: PaymentSchedule[];
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
  
  // ========== REMBOURSEMENT AUTOMATIQUE ==========
  autoRepaymentPlan: AutoRepaymentPlan | null = null;
  isAutoRepaymentEnabled: boolean = false;
  showPaymentSchedule: boolean = false;
  
  // ========== FORMULAIRE ==========
  repaymentAmount: number = 0;
  paymentMethod: string = 'CASH';
  repaymentType: 'MANUAL' | 'AUTO_MONTHLY' = 'MANUAL';
  
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
          this.generateAutoRepaymentPlan();
        }
      });
    }
  }

  // ========== LOAD CREDIT DETAILS (CLIENT) ==========
  loadCreditDetails(): void {
    if (!this.creditId) return;
    
    this.isLoading = true;
    this.cdr.markForCheck();
    console.log('Loading credit with ID:', this.creditId, 'Type:', typeof this.creditId);
    
    // Load all client credits and find the one with the ID
    this.creditService.getMyCredits().subscribe({
      next: (credits) => {
        console.log('All client credits:', credits);
        console.log('Searching for credit with ID:', this.creditId);
        
        // Display all IDs for debugging
        credits.forEach(c => {
          console.log('Credit ID:', c.creditId, 'Type:', typeof c.creditId, 'Equal?', c.creditId === this.creditId);
        });
        
        this.credit = credits.find(c => c.creditId === this.creditId) || null;
        if (this.credit) {
          this.repaymentAmount = this.credit.monthlyPayment;
          console.log('Credit found:', this.credit);
        } else {
          this.errorMessage = 'Credit not found';
          console.error('Credit with ID', this.creditId, 'not found in list');
        }
        this.isLoading = false;
        this.cdr.markForCheck();
        console.log('isLoading:', this.isLoading, 'credit:', this.credit);
        
        // Generate repayment plan after loading credit
        if (this.credit) {
          setTimeout(() => this.generateAutoRepaymentPlan(), 100);
        }
      },
      error: (err) => {
        console.error('Error loading credits:', err);
        this.errorMessage = 'Error loading credit';
        this.toastr.error(this.errorMessage);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== LOAD MY PAYMENTS (CLIENT) ==========
  loadMyPayments(): void {
    const headers = this.getHeaders();
    console.log('Calling /myPayments with headers:', headers);
    
    this.http.get<Repayment[]>(`${this.apiUrl}/myPayments`, {
      headers: headers
    }).subscribe({
      next: (data) => {
        console.log('Payments received:', data);
        this.myRepayments = data || [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading payments:', err);
        console.error('Status:', err.status);
        console.error('Message:', err.error?.message);
        // Don't display error if there are just no payments
        if (err.status !== 403 && err.status !== 401) {
          this.myRepayments = [];
        }
        this.cdr.markForCheck();
      }
    });
  }

  // ========== LOAD REMAINING AMOUNT ==========
  loadRemainingAmount(): void {
    if (!this.creditId) return;

    const headers = this.getHeaders();
    this.http.get<RemainingAmount>(`${this.apiUrl}/remaining/${this.creditId}`, {
      headers: headers
    }).subscribe({
      next: (data) => {
        console.log('Remaining amount:', data);
        this.remainingAmount = data.remainingAmount;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading remaining amount:', err);
        this.remainingAmount = 0;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== GENERATE AUTO REPAYMENT PLAN ==========
  generateAutoRepaymentPlan(): void {
    if (!this.credit) return;

    const startDate = new Date();
    const endDate = new Date(this.credit.endDate);
    const monthlyAmount = this.credit.monthlyPayment;
    const totalAmount = this.remainingAmount || this.credit.amount;
    
    // Calculate remaining tranches
    const monthsRemaining = this.calculateMonthsRemaining(startDate, endDate);
    const totalTranches = Math.ceil(totalAmount / monthlyAmount);
    
    const schedule: PaymentSchedule[] = [];
    
    for (let i = 0; i < totalTranches; i++) {
      const dueDate = new Date(startDate);
      dueDate.setMonth(dueDate.getMonth() + i + 1);
      
      // Check if this tranche is already paid
      const isPaid = this.isTrancheAlreadyPaid(i + 1);
      
      schedule.push({
        trancheNumber: i + 1,
        dueDate: dueDate,
        amount: i === totalTranches - 1 ? totalAmount - (monthlyAmount * i) : monthlyAmount,
        status: isPaid ? 'PAID' : (dueDate < new Date() ? 'OVERDUE' : 'PENDING'),
        isPaid: isPaid
      });
    }

    this.autoRepaymentPlan = {
      totalTranches: totalTranches,
      monthlyAmount: monthlyAmount,
      startDate: startDate,
      endDate: endDate,
      schedule: schedule
    };

    console.log('Repayment plan generated:', this.autoRepaymentPlan);
  }

  // ========== CALCULATE REMAINING MONTHS ==========
  calculateMonthsRemaining(startDate: Date, endDate: Date): number {
    const start = new Date(startDate.getFullYear(), startDate.getMonth());
    const end = new Date(endDate.getFullYear(), endDate.getMonth());
    
    let months = (end.getFullYear() - start.getFullYear()) * 12;
    months += end.getMonth() - start.getMonth();
    
    return Math.max(0, months);
  }

  // ========== CHECK IF TRANCHE IS ALREADY PAID ==========
  isTrancheAlreadyPaid(trancheNumber: number): boolean {
    // Enhanced logic to check if a specific tranche is already paid
    // Based on payment history and dates
    if (!this.myRepayments || this.myRepayments.length === 0) return false;
    
    // Calculate due date for this tranche
    const startDate = new Date();
    const trancheDueDate = new Date(startDate);
    trancheDueDate.setMonth(trancheDueDate.getMonth() + trancheNumber);
    
    // Check if there's a payment for this period
    const paymentsForTranche = this.myRepayments.filter(payment => {
      const paymentDate = new Date(payment.paymentDate);
      const monthDiff = Math.abs(paymentDate.getMonth() - trancheDueDate.getMonth()) + 
                       Math.abs(paymentDate.getFullYear() - trancheDueDate.getFullYear()) * 12;
      return monthDiff <= 1; // One month tolerance
    });
    
    return paymentsForTranche.length > 0;
  }

  // ========== ENABLE/DISABLE AUTO REPAYMENT ==========
  toggleAutoRepayment(): void {
    this.isAutoRepaymentEnabled = !this.isAutoRepaymentEnabled;
    
    if (this.isAutoRepaymentEnabled) {
      this.repaymentType = 'AUTO_MONTHLY';
      this.repaymentAmount = this.credit?.monthlyPayment || 0;
      this.showPaymentSchedule = true;
      this.successMessage = 'Auto repayment enabled. You will pay automatically each month.';
    } else {
      this.repaymentType = 'MANUAL';
      this.showPaymentSchedule = false;
      this.successMessage = 'Manual repayment enabled. You can choose the amount to pay.';
    }
    
    this.toastr.info(this.successMessage);
    setTimeout(() => this.successMessage = '', 3000);
  }

  // ========== PAY NEXT TRANCHE ==========
  payNextTranche(): void {
    if (!this.autoRepaymentPlan || !this.credit) return;

    const nextTranche = this.getNextUnpaidTranche();
    if (!nextTranche) {
      this.errorMessage = 'No pending tranche to pay';
      this.toastr.warning(this.errorMessage);
      return;
    }

    this.repaymentAmount = nextTranche.amount;
    this.submitRepayment();
  }

  // ========== GET NEXT UNPAID TRANCHE ==========
  getNextUnpaidTranche(): PaymentSchedule | null {
    if (!this.autoRepaymentPlan) return null;
    
    return this.autoRepaymentPlan.schedule.find(tranche => !tranche.isPaid) || null;
  }

  // ========== GET OVERDUE TRANCHES ==========
  getOverdueTranches(): PaymentSchedule[] {
    if (!this.autoRepaymentPlan) return [];
    
    const today = new Date();
    return this.autoRepaymentPlan.schedule.filter(tranche => 
      !tranche.isPaid && tranche.dueDate < today
    );
  }

  // ========== CALCULATE TOTAL OVERDUE AMOUNT ==========
  getTotalOverdueAmount(): number {
    return this.getOverdueTranches().reduce((total, tranche) => total + tranche.amount, 0);
  }

  // ========== UPDATE PAYMENT SCHEDULE AFTER PAYMENT ==========
  updatePaymentScheduleAfterPayment(): void {
    if (!this.autoRepaymentPlan) return;

    const nextTranche = this.getNextUnpaidTranche();
    if (nextTranche) {
      nextTranche.isPaid = true;
      nextTranche.status = 'PAID';
      
      // Check if all tranches are paid
      const allPaid = this.autoRepaymentPlan.schedule.every(tranche => tranche.isPaid);
      if (allPaid) {
        this.successMessage += ' - Credit fully repaid!';
        this.isAutoRepaymentEnabled = false;
      }
    }
  }

  // ========== GET REPAYMENT PROGRESS ==========
  getRepaymentProgress(): { paid: number; total: number; percentage: number } {
    if (!this.autoRepaymentPlan) return { paid: 0, total: 0, percentage: 0 };

    const paid = this.autoRepaymentPlan.schedule.filter(t => t.isPaid).length;
    const total = this.autoRepaymentPlan.schedule.length;
    const percentage = total > 0 ? (paid / total) * 100 : 0;

    return { paid, total, percentage };
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

    // Si c'est un paiement Stripe, utiliser l'endpoint spécial
    if (this.paymentMethod === 'STRIPE') {
      this.handleStripePayment();
      return;
    }

    // Paiement classique (CASH, CARD, BANK_TRANSFER)
    this.handleRegularPayment();
  }

  // ========== PAIEMENT CLASSIQUE ==========
  private handleRegularPayment(): void {
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
        
        // Mettre à jour le plan de remboursement si activé
        if (this.isAutoRepaymentEnabled) {
          this.updatePaymentScheduleAfterPayment();
        }
        
        // Recharger les données
        setTimeout(() => {
          this.loadMyPayments();
          this.loadRemainingAmount();
          this.loadCreditDetails();
          this.generateAutoRepaymentPlan(); // Régénérer le plan
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

  // ========== PAIEMENT STRIPE ==========
  private handleStripePayment(): void {
    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';
    this.cdr.markForCheck();

    const repaymentData = {
      amount: this.repaymentAmount,
      paymentMethod: 'STRIPE'
    };

    console.log('Envoi paiement Stripe:', repaymentData);

    this.http.post(`${this.apiUrl}/stripe-pay/${this.creditId}`, repaymentData, {
      headers: this.getHeaders()
    }).subscribe({
      next: (response: any) => {
        console.log('PaymentIntent créé:', response);
        
        // Afficher les informations du paiement Stripe
        this.successMessage = `Paiement Stripe initié. Client Secret: ${response.clientSecret}`;
        this.toastr.info('Paiement Stripe initié. Utilisez le client secret pour confirmer le paiement.');
        
        // Note: Dans une vraie application, vous utiliseriez le clientSecret 
        // avec Stripe Elements pour finaliser le paiement
        console.log('Client Secret:', response.clientSecret);
        console.log('Payment Intent ID:', response.paymentIntentId);
        
        this.isSubmitting = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Erreur paiement Stripe:', err);
        this.errorMessage = err.error?.message || 'Erreur lors du paiement Stripe';
        this.toastr.error(this.errorMessage);
        this.isSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ========== TÉLÉCHARGER LE PDF D'AMORTISSEMENT (CLIENT) ==========
  downloadAmortissementPdf(): void {
    if (!this.creditId) {
      this.errorMessage = 'ID de crédit invalide';
      this.toastr.error(this.errorMessage);
      return;
    }

    console.log(`Téléchargement PDF pour crédit ID: ${this.creditId}`);

    this.http.get(`${this.apiUrl}/credits/${this.creditId}/amortissement/pdf`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        // Créer un lien de téléchargement
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `amortissement_credit_${this.creditId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        
        this.successMessage = 'PDF téléchargé avec succès';
        this.toastr.success(this.successMessage);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Erreur téléchargement PDF:', err);
        this.errorMessage = err.error?.message || 'Erreur lors du téléchargement du PDF';
        this.toastr.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
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

  getMethodBadgeClass(method: string): string {
    switch(method?.toUpperCase()) {
      case 'CASH':
        return 'bg-info';
      case 'CARD':
        return 'bg-primary';
      case 'BANK_TRANSFER':
        return 'bg-secondary';
      case 'STRIPE':
        return 'bg-purple';
      default:
        return 'bg-secondary';
    }
  }

  getMethodLabel(method: string): string {
    switch(method?.toUpperCase()) {
      case 'CASH':
        return 'Cash';
      case 'CARD':
        return 'Card';
      case 'BANK_TRANSFER':
        return 'Bank Transfer';
      case 'STRIPE':
        return 'Stripe';
      default:
        return method || 'Unknown';
    }
  }

  // ========== NAVIGATION ==========
  goBack(): void {
    this.router.navigate(['/public/credit']);
  }
}
