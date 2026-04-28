import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompensationService } from '../../services/compensation.service';
import { Compensation } from '../../../../shared';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { environment } from '../../../../../environments/environment';
import { Router } from '@angular/router';

@Component({
  selector: 'app-list-my-compensations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './list-my-compensations.component.html',
  styleUrls: ['./list-my-compensations.component.css']
})
export class ListMyCompensationsComponent implements OnInit {
  compensations: Compensation[] = [];
  filteredCompensations: Compensation[] = [];
  loading = false;
  error = '';
  successMessage = '';

  // Payment properties
  selectedCompensationForPayment: Compensation | null = null;
  processingPayment = false;
  paymentMethod: string = 'CARD';
  
  // Stripe
  stripe: Stripe | null = null;
  
  // Card details
  cardDetails = {
    cardNumber: '',
    cardHolder: '',
    expiryDate: '',
    cvv: ''
  };

  // Filters
  filterStatus: string = 'ALL';
  statusOptions = ['ALL', 'CALCULATED', 'PAID', 'PENDING', 'CANCELLED'];

  // Pagination
  currentPage = 1;
  itemsPerPage = 6;
  totalPages = 1;

  constructor(private compensationService: CompensationService,
    private router: Router  
  ) {}

  async ngOnInit(): Promise<void> {
    await this.initStripe();
    this.loadCompensations();
  }

  async initStripe(): Promise<void> {
    if (!environment.stripePublicKey) {
      console.error('Stripe key not configured');
      return;
    }
    
    try {
      this.stripe = await loadStripe(environment.stripePublicKey);
    } catch (error) {
      console.error('Stripe error:', error);
    }
  }

  loadCompensations(): void {
    this.loading = true;
    this.error = '';
    this.compensationService.getMyCompensations().subscribe({
      next: (data) => {
        this.compensations = data;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error loading compensations: ' + err.message;
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    if (this.filterStatus === '' || this.filterStatus === 'ALL') {
      this.filteredCompensations = [...this.compensations];
    } else {
      this.filteredCompensations = this.compensations.filter(
        compensation => compensation.status === this.filterStatus
      );
    }
    
    this.totalPages = Math.ceil(this.filteredCompensations.length / this.itemsPerPage);
    this.currentPage = 1;
  }

  getPaginatedCompensations(): Compensation[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredCompensations.slice(startIndex, endIndex);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  viewCompensationDetails(compensationId: number): void {
    // Navigation avec le chemin complet incluant /public
    this.router.navigate([`/public/compensations/${compensationId}/details`]);
  }

  selectCompensationForPayment(compensation: Compensation): void {
    if (compensation.status !== 'CALCULATED') {
      this.error = 'This compensation cannot be paid';
      setTimeout(() => this.error = '', 3000);
      return;
    }
    
    this.selectedCompensationForPayment = compensation;
    this.paymentMethod = 'CARD';
    this.cardDetails = {
      cardNumber: '',
      cardHolder: '',
      expiryDate: '',
      cvv: ''
    };
  }

  cancelPayment(): void {
    this.selectedCompensationForPayment = null;
    this.processingPayment = false;
  }

  async processCardPayment(): Promise<void> {
    if (!this.selectedCompensationForPayment) return;
    
    this.processingPayment = true;
    
    this.compensationService.payCompensationByCard(this.selectedCompensationForPayment.compensationId).subscribe({
      next: async (response) => {
        if (response.success && response.clientSecret && this.stripe) {
          try {
            const result = await this.stripe.confirmCardPayment(response.clientSecret, {
              payment_method: {
                card: {
                  token: 'tok_visa',
                },
                billing_details: {
                  name: this.cardDetails.cardHolder || 'Client',
                },
              },
            });
            
            if (result.error) {
              this.processingPayment = false;
              this.error = 'Payment error: ' + result.error.message;
            } else if (result.paymentIntent?.status === 'succeeded') {
              this.compensationService.confirmCompensationPayment(result.paymentIntent.id).subscribe({
                next: (confirmResponse) => {
                  this.processingPayment = false;
                  if (confirmResponse.success) {
                    this.successMessage = 'Payment successful!';
                    this.loadCompensations();
                    this.cancelPayment();
                    setTimeout(() => this.successMessage = '', 5000);
                  } else {
                    this.error = confirmResponse.error || 'Confirmation failed';
                  }
                },
                error: (err) => {
                  this.processingPayment = false;
                  this.error = 'Confirmation error: ' + err.message;
                }
              });
            }
          } catch (error) {
            this.processingPayment = false;
            console.error('Stripe error:', error);
            this.error = 'Payment processing error';
          }
        } else {
          this.processingPayment = false;
          this.error = response.error || 'Failed to initialize payment';
        }
      },
      error: (err) => {
        this.processingPayment = false;
        console.error('Payment error:', err);
        this.error = err.error?.error || 'Payment initialization failed';
      }
    });
  }

  processCashPayment(): void {
    if (!this.selectedCompensationForPayment) return;
  
    const confirmMessage = `Confirm cash payment of ${this.formatAmount(this.selectedCompensationForPayment.clientOutOfPocket)} for compensation #${this.selectedCompensationForPayment.compensationId}?`;
  
    if (!confirm(confirmMessage)) {
      return;
    }
  
    this.processingPayment = true;
  
    this.compensationService.payCompensationByCash(this.selectedCompensationForPayment.compensationId).subscribe({
      next: (response) => {
        this.processingPayment = false;
      
        if (response.success) {
          this.successMessage = response.message || 'Cash payment recorded successfully!';
          this.loadCompensations();
          this.cancelPayment();
          setTimeout(() => this.successMessage = '', 5000);
        } else {
          this.error = response.error || 'Payment failed';
        }
      },
      error: (err) => {
        this.processingPayment = false;
        console.error('Payment error:', err);
        this.error = err.error?.error || 'Payment failed';
      }
    });
  }

  processPayment(): void {
    if (!this.selectedCompensationForPayment) {
      this.error = 'Please select a compensation to pay';
      return;
    }
    
    if (this.selectedCompensationForPayment.status !== 'CALCULATED') {
      this.error = 'This compensation has already been paid';
      this.cancelPayment();
      return;
    }
    
    if (this.paymentMethod === 'CARD') {
      this.processCardPayment();
    } else if (this.paymentMethod === 'CASH') {
      this.processCashPayment();
    }
  }

  

  // Statistics methods
  getCalculatedCount(): number {
    return this.compensations.filter(c => c.status === 'CALCULATED').length;
  }

  getPaidCount(): number {
    return this.compensations.filter(c => c.status === 'PAID').length;
  }

  getTotalOutOfPocket(): number {
    return this.compensations.reduce((sum, c) => sum + (c.clientOutOfPocket || 0), 0);
  }

  // UNE SEULE méthode formatAmount (celle-ci est gardée)
  formatAmount(amount: number): string {
    if (amount === undefined || amount === null) return '0,000 DT';
    return amount.toLocaleString('fr-TN', { 
      minimumFractionDigits: 3, 
      maximumFractionDigits: 3 
    }) + ' DT';
  }

  // Status styling methods
  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'CALCULATED': 'calculated',
      'PAID': 'paid',
      'PENDING': 'pending',
      'CANCELLED': 'cancelled'
    };
    return `compensation-status ${statusMap[status] || ''}`;
  }

  getRiskScoreClass(score: number): string {
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  }

  getRiskLevelClass(level: string): string {
    const levelMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'very-low',
      'FAIBLE': 'low',
      'MOYEN': 'medium',
      'ELEVE': 'high',
      'TRES_ELEVE': 'very-high'
    };
    return `risk-level ${levelMap[level] || ''}`;
  }

  getDecisionClass(decision: string): string {
    const decisionMap: { [key: string]: string } = {
      'AUTO_APPROVE': 'approve',
      'AUTO_REJECT': 'reject',
      'MANUAL_REVIEW': 'review'
    };
    return `decision ${decisionMap[decision] || ''}`;
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  
}