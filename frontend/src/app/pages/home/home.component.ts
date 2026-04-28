import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService, UserInfo } from '../../services/auth.service';
import { ClientAccountService, ClientAccount } from '../../services/client-account.service';
import { AccountRequestService, AccountRequest } from '../../services/account-request.service';
import { TransactionService } from '../../services/transaction.service';
import { IAAssistantComponent } from '../../components/ia-assistant/ia-assistant.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe, DatePipe, RouterLink, RouterLinkActive,IAAssistantComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  firstName: string = '';
  lastName: string = '';
  userEmail: string = '';
  userRole: string = '';
  
  // Real data
  accounts: ClientAccount[] = [];
  transactions: any[] = [];
  myRequests: AccountRequest[] = [];
  pendingRequests: AccountRequest[] = [];
  
  // UI states for modals
  showRequestModal = false;
  showTransferModal = false;
  newRequestType: string = 'CURRENT';
  
  // Transfer
  transferData = {
    sourceRip: '',
    targetRip: '',
    amount: 0,
    description: ''
  };
  message: string = '';
  messageType: 'success' | 'error' = 'success';
  
  // Rejection
  rejectModalOpen = false;
  selectedRequest: AccountRequest | null = null;
  rejectReason: string = '';
  
  // Current date
  currentDate: string = '';

  constructor(
    private auth: AuthService,
    private router: Router,
    private clientAccountService: ClientAccountService,
    private accountRequestService: AccountRequestService,
    private transactionService: TransactionService
  ) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/public/login']);
      return;
    }
    
    this.loadUserInfo();
    this.setCurrentDate();
    this.loadUserData();
  }

  setCurrentDate() {
    const now = new Date();
    this.currentDate = now.toLocaleDateString('en-US', { 
      weekday: 'long', 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  }

  loadUserInfo() {
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
          this.firstName = payload.firstName || 'Client';
          this.lastName = payload.lastName || '';
          this.userEmail = payload.sub || '';
          this.userRole = payload.role || 'CLIENT';
        } catch (e) {
          console.error('Token error', e);
        }
      }
    }
  }

  loadUserData() {
    if (this.isClient()) {
      this.loadClientAccounts();
      this.loadClientRequests();
      this.loadRecentTransactions();
    } else if (this.isAgentFinance()) {
      this.loadPendingRequests();
    }
  }

  isClient(): boolean {
    return this.userRole === 'CLIENT';
  }

  isAgentFinance(): boolean {
    return this.userRole === 'AGENT_FINANCE';
  }

  isAdmin(): boolean {
    return this.userRole === 'ADMIN';
  }

  loadClientAccounts() {
    this.clientAccountService.getMyAccounts().subscribe({
      next: (data) => {
        this.accounts = data;
        if (this.accounts.length > 0 && this.transactions.length === 0) {
          this.loadRecentTransactions();
        }
      },
      error: (err) => console.error('Error loading accounts', err)
    });
  }

  loadClientRequests() {
    this.accountRequestService.getMyRequests().subscribe({
      next: (data) => {
        this.myRequests = data;
      },
      error: (err) => console.error('Error loading requests', err)
    });
  }

  loadRecentTransactions() {
    if (this.accounts.length > 0) {
      this.transactionService.getTransactionsByAccount(this.accounts[0]?.accountId).subscribe({
        next: (data) => {
          this.transactions = data.slice(0, 5);
        },
        error: (err) => console.error('Error loading transactions', err)
      });
    }
  }

  loadPendingRequests() {
    this.accountRequestService.getPendingRequests().subscribe({
      next: (data) => {
        this.pendingRequests = data;
      },
      error: (err) => console.error('Error loading requests', err)
    });
  }

  getTotalBalance(): number {
    return this.accounts.reduce((sum, acc) => sum + acc.balance, 0);
  }

  getCurrentAccountBalance(): number {
    const current = this.accounts.find(a => a.type === 'CURRENT');
    return current?.balance || 0;
  }

  getSavingsAccountBalance(): number {
    const savings = this.accounts.find(a => a.type === 'SAVINGS');
    return savings?.balance || 0;
  }

  getTransactionCount(): number {
    return this.transactions.length;
  }

  submitRequest() {
    if (!this.newRequestType) {
      this.showMessage('Please select an account type', 'error');
      return;
    }

    this.accountRequestService.createRequest({ type: this.newRequestType }).subscribe({
      next: () => {
        this.showRequestModal = false;
        this.loadClientRequests();
        this.showMessage('Request sent successfully!', 'success');
        this.newRequestType = 'CURRENT';
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.message || 'Error submitting request';
        this.showMessage(errorMsg, 'error');
      }
    });
  }

  submitTransfer() {
    if (!this.transferData.sourceRip) {
      this.showMessage('Please select a source account', 'error');
      return;
    }
    
    if (this.transferData.targetRip.length !== 21) {
      this.showMessage(`Destination RIP must be exactly 21 digits (currently: ${this.transferData.targetRip.length} digits)`, 'error');
      return;
    }
    
    if (this.transferData.amount <= 0) {
      this.showMessage('Amount must be greater than 0', 'error');
      return;
    }

    this.transactionService.transferByRip(
      this.transferData.sourceRip,
      this.transferData.targetRip,
      this.transferData.amount,
      this.transferData.description || 'Transfer'
    ).subscribe({
      next: (response) => {
        this.showMessage(response, 'success');
        this.loadClientAccounts();
        this.loadRecentTransactions();
        this.showTransferModal = false;
        this.transferData = { sourceRip: '', targetRip: '', amount: 0, description: '' };
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.message || 'Error during transfer';
        this.showMessage(errorMsg, 'error');
      }
    });
  }

  downloadStatement() {
    if (this.accounts.length > 0) {
      this.transactionService.getAccountStatement(this.accounts[0].accountId).subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `account_statement_${this.accounts[0].accountId}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.showMessage('PDF downloaded successfully', 'success');
        },
        error: () => {
          this.showMessage('Error downloading PDF', 'error');
        }
      });
    } else {
      this.showMessage('No account available', 'error');
    }
  }

  approveRequest(requestId: number) {
    if (confirm('Confirm approval of this request?')) {
      this.accountRequestService.approveRequest(requestId).subscribe({
        next: () => {
          this.showMessage('Request approved successfully!', 'success');
          this.loadPendingRequests();
        },
        error: (err) => {
          this.showMessage(err.error?.message || 'Error during approval', 'error');
        }
      });
    }
  }

  openRejectModal(request: AccountRequest) {
    this.selectedRequest = request;
    this.rejectModalOpen = true;
    this.rejectReason = '';
  }

  closeRejectModal() {
    this.rejectModalOpen = false;
    this.selectedRequest = null;
    this.rejectReason = '';
  }

  confirmReject() {
    if (this.selectedRequest && this.rejectReason.trim()) {
      this.accountRequestService.rejectRequest(this.selectedRequest.id, this.rejectReason).subscribe({
        next: () => {
          this.showMessage('Request rejected successfully', 'success');
          this.closeRejectModal();
          this.loadPendingRequests();
        },
        error: (err) => {
          this.showMessage(err.error?.message || 'Error during rejection', 'error');
        }
      });
    } else {
      this.showMessage('Please enter a rejection reason', 'error');
    }
  }

  showMessage(msg: string, type: 'success' | 'error') {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => {
      this.message = '';
    }, 5000);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/']);
  }

  // ============================================
  // NAVIGATION TO DEDICATED PAGES
  // ============================================
  goToAccounts() {
    this.router.navigate(['/public/client/accounts']);
  }

  goToAdminAccounts() {
    this.router.navigate(['/backoffice/account']);
  }

  goToAdminTransactions() {
    this.router.navigate(['/backoffice/transaction']);
  }
}