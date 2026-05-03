import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ContractService } from '../../../services/contract.service';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { loadStripe, Stripe } from '@stripe/stripe-js';
import { environment } from '../../../../../../environments/environment';

@Component({
  selector: 'app-payment-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment-page.component.html',
  styleUrls: ['./payment-page.component.css']
})
export class PaymentPageComponent implements OnInit, OnDestroy {
  contract: any = null;
  allPayments: any[] = [];
  paginatedPayments: any[] = [];
  isLoading = false;
  selectedPayments: Set<number> = new Set();
  totalSelectedAmount = 0;
  selectedPaymentId: number = 0;
  processingPayment = false;
  contractId: number = 0;
  clientEmail: string = '';
  clientId: number = 0;
  
  // Pagination
  currentPage: number = 1;
  pageSize: number = 6;
  totalPages: number = 1;
  
  paymentMethod: string = 'CARD';
  cardDetails = {
    cardNumber: '',
    cardHolder: '',
    expiryDate: '',
    cvv: ''
  };
  
  // Bank Transfer Properties
  bankTransferDetails = {
    rip: ''
  };

  bankBalanceInfo: {
    balance: number;
    sufficient: boolean;
    error?: string;
  } | null = null;

  checkingBalance = false;
  
  // Cash Payment Properties
  pendingCashPayment: any = null;
  recentlyRejectedPaymentId: number | null = null;
  
  // Stripe
  stripe: Stripe | null = null;
  
  Math = Math;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private toastr: ToastrService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}
  
  get pendingCount(): number {
    return this.allPayments.filter(p => p.status === 'PENDING').length;
  }
  
  async ngOnInit(): Promise<void> {
    this.contractId = Number(this.route.snapshot.paramMap.get('id'));
    
    await this.initStripe();
    this.getUserInfo();
    
    if (this.contractId) {
      this.loadContractDetails();
      this.loadPayments();
    } else {
      this.toastr.error('Contract not found');
      this.router.navigate(['/public/insurance/my-contracts']);
    }
    
    // Listen for CASH request updates
    this.notificationService.listenForRequestUpdate().subscribe((request) => {
      if (request.paymentId && this.selectedPaymentId === request.paymentId) {
        if (request.status === 'approved') {
          this.handleAgentApprovalFromNotification(request);
        } else if (request.status === 'rejected') {
          this.handleAgentRejectionFromNotification(request);
        }
      }
    });
  }
  
  getUserInfo(): void {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.clientEmail = payload.email || payload.sub || '';
        this.clientId = payload.id || payload.userId || 0;
      } catch (e) {
        console.error('Token decode error', e);
      }
    }
  }
  
  async initStripe(): Promise<void> {
    if (!environment.stripePublicKey) {
      console.error('❌ Stripe key not configured');
      return;
    }
    
    try {
      this.stripe = await loadStripe(environment.stripePublicKey);
      if (this.stripe) {
        console.log('✅ Stripe initialized successfully');
      }
    } catch (error) {
      console.error('❌ Stripe error:', error);
    }
  }
  
  loadContractDetails(): void {
    this.contractService.getContractById(this.contractId).subscribe({
      next: (contract) => {
        this.contract = contract;
        console.log('📄 Contract loaded:', contract);
        console.log('👤 Agent ID (agentAssuranceId):', contract?.agentAssuranceId);
        console.log('👤 Client:', contract?.client);
      },
      error: (err) => {
        console.error('Error loading contract:', err);
        this.toastr.error('Error loading contract');
      }
    });
  }
  
  loadPayments(): void {
    this.isLoading = true;
    this.contractService.getPaymentsByContract(this.contractId).subscribe({
      next: (payments) => {
        this.allPayments = payments.sort((a, b) => 
          new Date(a.paymentDate).getTime() - new Date(b.paymentDate).getTime()
        );
        this.isLoading = false;
        this.currentPage = 1;
        this.updatePagination();
        
        if (this.recentlyRejectedPaymentId) {
          const rejectedPayment = this.allPayments.find(p => p.paymentId === this.recentlyRejectedPaymentId);
          if (rejectedPayment && rejectedPayment.status === 'PENDING') {
            this.toastr.info('Rejected payment can be reselected', 'Information');
          }
        }
        
        if (this.pendingCount === 0) {
          this.toastr.info('No pending payments for this contract');
        }
      },
      error: (err) => {
        console.error('Error loading payments:', err);
        this.toastr.error('Error loading payments');
        this.isLoading = false;
      }
    });
  }
  
  updatePagination(): void {
    this.totalPages = Math.ceil(this.allPayments.length / this.pageSize);
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedPayments = this.allPayments.slice(start, end);
  }
  
  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
      document.querySelector('.card-body')?.scrollIntoView({ behavior: 'smooth' });
    }
  }
  
  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
      document.querySelector('.card-body')?.scrollIntoView({ behavior: 'smooth' });
    }
  }
  
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
      document.querySelector('.card-body')?.scrollIntoView({ behavior: 'smooth' });
    }
  }
  
  togglePayment(paymentId: number, amount: number, status: string): void {
    if (status !== 'PENDING') {
      if (status === 'PAID') {
        this.toastr.warning('This payment has already been made');
      }
      return;
    }
    
    // Clear recent rejection indicator if reselecting
    if (this.recentlyRejectedPaymentId === paymentId) {
      this.recentlyRejectedPaymentId = null;
    }
    
    if (this.selectedPayments.has(paymentId)) {
      this.selectedPayments.delete(paymentId);
      this.totalSelectedAmount = 0;
      this.selectedPaymentId = 0;
    } else {
      this.selectedPayments.clear();
      this.selectedPayments.add(paymentId);
      this.totalSelectedAmount = amount;
      this.selectedPaymentId = paymentId;
    }
  }
  
  selectFirstPending(): void {
    const firstPending = this.allPayments.find(p => p.status === 'PENDING');
    if (firstPending) {
      this.selectedPayments.clear();
      this.selectedPayments.add(firstPending.paymentId);
      this.totalSelectedAmount = firstPending.amount;
      this.selectedPaymentId = firstPending.paymentId;
      this.toastr.info(`Installment #${firstPending.paymentId} selected`);
      
      const element = document.querySelector(`#payment-${firstPending.paymentId}`);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    } else {
      this.toastr.warning('No pending installments');
    }
  }
  
  clearSelection(): void {
    this.selectedPayments.clear();
    this.totalSelectedAmount = 0;
    this.selectedPaymentId = 0;
    this.recentlyRejectedPaymentId = null;
  }
  
  checkBankBalance(): void {
    const ripRegex = /^\d{21}$/;
    if (!this.bankTransferDetails.rip || !ripRegex.test(this.bankTransferDetails.rip)) {
      this.bankBalanceInfo = null;
      if (this.bankTransferDetails.rip && this.bankTransferDetails.rip.length > 0) {
        this.bankBalanceInfo = {
          balance: 0,
          sufficient: false,
          error: 'RIP must contain exactly 21 digits'
        };
      }
      return;
    }
  
    this.checkingBalance = true;
  
    this.contractService.checkBankBalance(
      this.bankTransferDetails.rip, 
      this.totalSelectedAmount
    ).subscribe({
      next: (response) => {
        this.checkingBalance = false;
        this.bankBalanceInfo = {
          balance: response.balance,
          sufficient: response.sufficient
        };
      },
      error: (err) => {
        this.checkingBalance = false;
        this.bankBalanceInfo = {
          balance: 0,
          sufficient: false,
          error: err.error?.message || 'Error checking balance'
        };
      }
    });
  }

  processBankTransferPayment(selectedPayment: any): void {
    const ripRegex = /^\d{21}$/;
    if (!this.bankTransferDetails.rip || !ripRegex.test(this.bankTransferDetails.rip)) {
      this.toastr.error('Invalid RIP. Please enter exactly 21 digits');
      this.processingPayment = false;
      return;
    }
  
    if (!this.bankBalanceInfo?.sufficient) {
      this.toastr.error('Insufficient balance to make this payment');
      this.processingPayment = false;
      return;
    }
  
    const paymentData = {
      clientEmail: this.clientEmail,
      contractId: this.contractId,
      installmentAmount: selectedPayment.amount,
      paymentType: 'BANK_TRANSFER',
      remainingAmount: this.contract?.remainingAmount || 0,
      sourceRip: this.bankTransferDetails.rip
    };
  
    console.log('📤 Processing Bank Transfer payment:', paymentData);
  
    this.contractService.payByBankTransfer(paymentData).subscribe({
      next: (response) => {
        this.processingPayment = false;
        this.toastr.success(`✅ Payment of ${selectedPayment.amount} DT completed successfully!`);
        console.log('✅ Response:', response);
      
        setTimeout(() => {
          this.loadPayments();
          this.clearSelection();
          this.bankTransferDetails.rip = '';
          this.bankBalanceInfo = null;
        }, 1000);
      
        setTimeout(() => {
          this.router.navigate(['/public/insurance/my-contracts']);
        }, 3000);
      },
      error: (err) => {
        this.processingPayment = false;
        console.error('❌ Error:', err);
        const errorMsg = err.error?.error || err.message || 'Error processing payment';
        this.toastr.error(errorMsg);
      }
    });
  }

  async processPayment(): Promise<void> {
    if (this.selectedPayments.size === 0) {
      this.toastr.warning('Please select an installment to pay');
      return;
    }
  
    const selectedPaymentId = Array.from(this.selectedPayments)[0];
    const selectedPayment = this.allPayments.find(p => p.paymentId === selectedPaymentId);
  
    if (!selectedPayment) {
      this.toastr.error('Error selecting payment');
      return;
    }
  
    if (selectedPayment.status !== 'PENDING') {
      this.toastr.error('This installment has already been paid');
      this.clearSelection();
      return;
    }
  
    if (!this.clientEmail) {
      this.toastr.error('Client email not found. Please reconnect.');
      return;
    }
  
    this.processingPayment = true;
  
    if (this.paymentMethod === 'CASH') {
      this.requestAgentApproval(selectedPayment);
    } 
    else if (this.paymentMethod === 'BANK_TRANSFER') {
      if (!this.bankTransferDetails.rip || this.bankTransferDetails.rip.length !== 21) {
        this.toastr.error('Please enter a valid 21-digit RIP');
        this.processingPayment = false;
        return;
      }
    
      if (!this.bankBalanceInfo?.sufficient) {
        this.toastr.error('Insufficient balance to make this payment');
        this.processingPayment = false;
        return;
      }
    
      this.processBankTransferPayment(selectedPayment);
    }
    else if (this.paymentMethod === 'CARD') {
      await this.processStripePayment(selectedPayment);
    } 
    else {
      this.processManualPayment(selectedPayment);
    }
  }
  
  async processStripePayment(selectedPayment: any): Promise<void> {
    try {
      this.processingPayment = true;
      
      this.contractService.createPaymentIntent(this.contractId, selectedPayment.amount).subscribe({
        next: async (response) => {
          if (response && response.clientSecret && this.stripe) {
            try {
              const result = await this.stripe.confirmCardPayment(response.clientSecret, {
                payment_method: {
                  card: {
                    token: 'tok_visa',
                  },
                  billing_details: {
                    name: this.cardDetails.cardHolder || 'Client Test',
                    email: this.clientEmail,
                  },
                },
              });
              
              if (result.error) {
                this.processingPayment = false;
                this.toastr.error('Error: ' + result.error.message);
              } else if (result.paymentIntent?.status === 'succeeded') {
                this.contractService.confirmPayment(result.paymentIntent.id).subscribe({
                  next: () => {
                    this.processingPayment = false;
                    this.toastr.success('✅ Payment completed successfully!');
                    setTimeout(() => {
                      this.router.navigate(['/public/insurance/my-contracts']);
                    }, 2000);
                  },
                  error: (err) => {
                    this.processingPayment = false;
                    console.error('Confirmation error:', err);
                    this.toastr.success('Payment completed successfully!');
                    setTimeout(() => {
                      this.router.navigate(['/public/insurance/my-contracts']);
                    }, 2000);
                  }
                });
              }
            } catch (error) {
              this.processingPayment = false;
              console.error('Error:', error);
              this.toastr.error('Error processing payment');
            }
          } else {
            this.processingPayment = false;
            this.toastr.error('Error creating payment');
          }
        },
        error: (err) => {
          this.processingPayment = false;
          console.error('Error:', err);
          this.toastr.error('Error initializing payment');
        }
      });
    } catch (error) {
      this.processingPayment = false;
      console.error(error);
      this.toastr.error('Processing error');
    }
  }
  
  requestAgentApproval(selectedPayment: any): void {
    const agentId = this.contract?.agentAssuranceId || 0;
    
    console.log('🔍 Verification before sending:');
    console.log('  - agentAssuranceId:', agentId);
    console.log('  - contractId:', this.contractId);
    console.log('  - clientId:', this.clientId);
    console.log('  - clientEmail:', this.clientEmail);
    console.log('  - paymentId:', selectedPayment.paymentId);
    console.log('  - amount:', selectedPayment.amount);
    
    if (!agentId || agentId === 0) {
      this.toastr.error('Unable to find your insurance agent. Please contact support.', 'Error');
      this.processingPayment = false;
      return;
    }
    
    if (!this.clientId || this.clientId === 0) {
      this.toastr.error('Missing client information. Please reconnect.', 'Error');
      this.processingPayment = false;
      return;
    }
    
    this.pendingCashPayment = {
      paymentId: selectedPayment.paymentId,
      contractId: this.contractId,
      clientId: this.clientId,
      agentId: agentId,
      amount: selectedPayment.amount,
      clientName: `${this.contract?.client?.firstName || ''} ${this.contract?.client?.lastName || ''}`.trim(),
      clientEmail: this.clientEmail,
      requestedAt: new Date().toISOString(),
      status: 'PENDING'
    };
    
    console.log('📝 CASH request prepared:', JSON.stringify(this.pendingCashPayment, null, 2));
    this.saveApprovalRequestToBackend(selectedPayment);
  }
  
  saveApprovalRequestToBackend(selectedPayment: any): void {
    const requestData = {
      paymentId: Number(selectedPayment.paymentId),
      contractId: Number(this.contractId),
      clientId: Number(this.clientId),
      agentId: Number(this.contract?.agentAssuranceId || 0),
      amount: Number(selectedPayment.amount),
      clientName: `${this.contract?.client?.firstName || ''} ${this.contract?.client?.lastName || ''}`.trim(),
      clientEmail: this.clientEmail,
      status: 'PENDING'
    };
    
    console.log('📤 Sending to backend:', JSON.stringify(requestData, null, 2));
    
    this.contractService.createCashRequest(requestData).subscribe({
      next: (response) => {
        console.log('✅ Request saved to database:', response);
        this.showConfirmationPopup(selectedPayment);
      },
      error: (err) => {
        console.error('❌ Error saving request:', err);
        console.error('Error details:', err.error);
        this.toastr.error(`Error: ${err.error?.message || 'Server error'}`, 'Error');
        this.processingPayment = false;
      }
    });
  }
  
  getPendingApprovalRequests(): any[] {
    const stored = localStorage.getItem('pendingCashApprovals');
    return stored ? JSON.parse(stored) : [];
  }
  
  showConfirmationPopup(selectedPayment: any): void {
    this.toastr.success(
      `✅ Payment request of ${selectedPayment.amount} DT sent to your agent.`, 
      'Request Sent',
      { timeOut: 3000 }
    );
    
    this.clearSelection();
    this.processingPayment = false;
  }
  
  startApprovalPolling(paymentId: number): void {
    const interval = setInterval(() => {
      this.contractService.getCashRequestStatus(paymentId).subscribe({
        next: (requests) => {
          if (requests && requests.length > 0) {
            const request = requests[0];
            if (request.status === 'APPROVED') {
              clearInterval(interval);
              this.handleAgentApproval(paymentId);
            } else if (request.status === 'REJECTED') {
              clearInterval(interval);
              this.handleAgentRejection(paymentId, request.rejectionReason);
            }
          }
        },
        error: (err) => {
          console.error('Error checking status:', err);
        }
      });
    }, 5000);
    
    setTimeout(() => {
      clearInterval(interval);
    }, 5 * 60 * 1000);
  }
  
  handleAgentApproval(paymentId: number): void {
    this.toastr.success('✅ Your payment has been approved by the agent!', 'Approved');
    
    const selectedPayment = this.allPayments.find(p => p.paymentId === paymentId);
    if (selectedPayment) {
      this.processCashPaymentAfterApproval(selectedPayment);
    }
    
    this.loadPayments();
  }
  
  handleAgentRejection(paymentId: number, reason?: string): void {
    this.toastr.error(`❌ Payment rejected: ${reason || 'Contact your agent'}`, 'Rejected');
    this.recentlyRejectedPaymentId = paymentId;
    this.loadPayments();
    
    setTimeout(() => {
      this.toastr.info('You can select this installment again', 'Information');
    }, 3000);
  }
  
  handleAgentApprovalFromNotification(request: any): void {
    if (this.pendingCashPayment && this.pendingCashPayment.paymentId === request.paymentId) {
      this.handleAgentApproval(request.paymentId);
    } else {
      this.loadPayments();
      this.toastr.success('Your payment has been approved by the agent', 'Approved');
    }
  }
  
  handleAgentRejectionFromNotification(request: any): void {
    if (this.pendingCashPayment && this.pendingCashPayment.paymentId === request.paymentId) {
      this.handleAgentRejection(request.paymentId, request.rejectionReason || 'Rejected by agent');
    } else {
      this.toastr.warning(`Your payment request has been rejected`, 'Rejected');
      this.loadPayments();
    }
  }
  
  processCashPaymentAfterApproval(selectedPayment: any): void {
    if (!this.clientEmail) {
      this.toastr.error('Client email not found. Please reconnect.');
      return;
    }
    
    this.processingPayment = true;
    
    const paymentData = {
      clientEmail: this.clientEmail,
      contractId: this.contractId,
      installmentAmount: selectedPayment.amount,
      paymentType: 'CASH',
      remainingAmount: this.contract?.remainingAmount || 0
    };
    
    console.log('📤 Processing approved CASH payment:', paymentData);
    
    this.contractService.processApprovedCashPayment(paymentData).subscribe({
      next: (response) => {
        this.processingPayment = false;
        this.toastr.success('✅ Cash payment completed successfully!');
        
        this.removeApprovalRequest(selectedPayment.paymentId);
        
        setTimeout(() => {
          this.loadPayments();
          this.clearSelection();
        }, 1000);
        
        setTimeout(() => {
          this.router.navigate(['/public/insurance/my-contracts']);
        }, 3000);
      },
      error: (err) => {
        this.processingPayment = false;
        console.error('❌ Error:', err);
        const errorMsg = err.error?.error || err.message || 'Error processing payment';
        this.toastr.error(errorMsg);
        this.markRequestAsFailed(selectedPayment.paymentId);
      }
    });
  }
  
  removeApprovalRequest(paymentId: number): void {
    const pendingRequests = this.getPendingApprovalRequests();
    const filtered = pendingRequests.filter(r => r.paymentId !== paymentId);
    localStorage.setItem('pendingCashApprovals', JSON.stringify(filtered));
  }
  
  markRequestAsFailed(paymentId: number): void {
    const pendingRequests = this.getPendingApprovalRequests();
    const request = pendingRequests.find(r => r.paymentId === paymentId);
    if (request) {
      request.status = 'failed';
      request.failedAt = new Date().toISOString();
      localStorage.setItem('pendingCashApprovals', JSON.stringify(pendingRequests));
    }
  }
  
  processManualPayment(selectedPayment: any): void {
    let paymentTypeValue: string;
    if (this.paymentMethod === 'BANK_TRANSFER') {
      paymentTypeValue = 'BANK_TRANSFER';
    } else {
      paymentTypeValue = 'CASH';
    }
    
    const paymentData = {
      clientEmail: this.clientEmail,
      contractId: this.contractId,
      installmentAmount: selectedPayment.amount,
      paymentType: paymentTypeValue,
      remainingAmount: this.contract?.remainingAmount || 0
    };
    
    console.log('📤 Sending manual payment:', paymentData);
    
    this.contractService.makePayment(paymentData).subscribe({
      next: (response) => {
        this.processingPayment = false;
        this.toastr.success('Payment completed successfully!');
        console.log('✅ Response:', response);
        
        setTimeout(() => {
          this.loadPayments();
          this.clearSelection();
        }, 1000);
        
        setTimeout(() => {
          this.router.navigate(['/public/insurance/my-contracts']);
        }, 3000);
      },
      error: (err) => {
        this.processingPayment = false;
        console.error('❌ Error:', err);
        const errorMsg = err.error?.error || err.message || 'Error processing payment';
        this.toastr.error(errorMsg);
      }
    });
  }
  
  getStatusBadgeClass(status: string): string {
    switch(status) {
      case 'PAID': return 'status-paid';
      case 'PENDING': return 'status-pending';
      case 'FAILED': return 'status-failed';
      case 'LATE': return 'status-late';
      default: return '';
    }
  }
  
  getStatusLabel(status: string): string {
    switch(status) {
      case 'PAID': return 'Paid';
      case 'PENDING': return 'Pending';
      case 'FAILED': return 'Failed';
      case 'LATE': return 'Late';
      default: return status;
    }
  }
  
  formatDate(date: string): string {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString('en-US', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }
  
  goBack(): void {
    this.router.navigate(['/public/insurance/my-contracts']);
  }
  
  ngOnDestroy(): void {
    // Cleanup if needed
  }
}