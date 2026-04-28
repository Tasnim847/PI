import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CreditService, Credit, CreditStatus } from '../../../services/credit.service';

@Component({
  selector: 'app-agent-credit-approvals',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './agent-credit-approvals.component.html',
  styleUrls: ['./agent-credit-approvals.component.css']
})
export class AgentCreditApprovalsComponent implements OnInit {

  credits: Credit[] = [];
  filteredCredits: Credit[] = [];

  selectedStatus = 'PENDING';
  searchTerm = '';
  isLoading = false;

  selectedCredit: Credit | null = null;
  showApproveModal = false;
  showRejectModal = false;

  approveInterestRate: number = 8;

  stats = {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
    inRepayment: 0,
    closed: 0
  };

  constructor(private creditService: CreditService) {}

  ngOnInit(): void {
    this.loadCredits();
  }

  loadCredits(): void {
    this.isLoading = true;

    this.creditService.getAllCredits().subscribe({
      next: (credits: Credit[]) => {
        this.credits = credits || [];
        this.calculateStats();
        this.filterCredits();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading credits:', err);
        this.isLoading = false;
      }
    });
  }

  filterCredits(): void {
    let filtered = [...this.credits];

    if (this.selectedStatus !== 'ALL') {
      filtered = filtered.filter(c => c.status === this.selectedStatus);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c =>
        c.creditId?.toString().includes(term) ||
        c.clientFirstName?.toLowerCase().includes(term) ||
        c.clientLastName?.toLowerCase().includes(term) ||
        c.clientEmail?.toLowerCase().includes(term) ||
        c.amount?.toString().includes(term)
      );
    }

    this.filteredCredits = filtered;
  }

  onStatusChange(): void {
    this.filterCredits();
  }

  onSearch(): void {
    this.filterCredits();
  }

  calculateStats(): void {
    this.stats.total = this.credits.length;
    this.stats.pending = this.credits.filter(c => c.status === 'PENDING').length;
    this.stats.approved = this.credits.filter(c => c.status === 'APPROVED').length;
    this.stats.rejected = this.credits.filter(c => c.status === 'REJECTED').length;
    this.stats.inRepayment = this.credits.filter(c => c.status === 'IN_REPAYMENT').length;
    this.stats.closed = this.credits.filter(c => c.status === 'CLOSED').length;
  }

  getStatusLabel(status: CreditStatus): string {
    return {
      PENDING: 'Pending',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      IN_REPAYMENT: 'In Repayment',
      CLOSED: 'Closed'
    }[status] || status;
  }

  getStatusClass(status: CreditStatus): string {
    return {
      PENDING: 'status-pending',
      APPROVED: 'status-approved',
      REJECTED: 'status-rejected',
      IN_REPAYMENT: 'status-repayment',
      CLOSED: 'status-closed'
    }[status] || 'status-default';
  }

  formatDate(date: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US');
  }

  formatAmount(amount: number): string {
    if (!amount && amount !== 0) return '-';
    return amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' TND';
  }

  getClientName(credit: Credit): string {
    if (credit.clientFirstName && credit.clientLastName) {
      return `${credit.clientFirstName} ${credit.clientLastName}`;
    }
    return credit.clientFullName || 'N/A';
  }

  openApproveModal(credit: Credit): void {
    this.selectedCredit = credit;
    this.approveInterestRate = 8;
    this.showApproveModal = true;
  }

  confirmApprove(): void {
    if (!this.selectedCredit) return;

    if (!this.approveInterestRate || this.approveInterestRate <= 0) {
      alert('Please enter a valid interest rate');
      return;
    }

    this.creditService.approveCredit(this.selectedCredit.creditId, this.approveInterestRate).subscribe({
      next: () => {
        alert(`Credit #${this.selectedCredit!.creditId} approved successfully`);
        this.loadCredits();
        this.showApproveModal = false;
        this.selectedCredit = null;
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to approve credit');
      }
    });
  }

  openRejectModal(credit: Credit): void {
    this.selectedCredit = credit;
    this.showRejectModal = true;
  }

  confirmReject(): void {
    if (!this.selectedCredit) return;

    this.creditService.rejectCredit(this.selectedCredit.creditId).subscribe({
      next: () => {
        alert(`Credit #${this.selectedCredit!.creditId} rejected successfully`);
        this.loadCredits();
        this.showRejectModal = false;
        this.selectedCredit = null;
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to reject credit');
      }
    });
  }

  refresh(): void {
    this.loadCredits();
  }

  closeModal(): void {
    this.showApproveModal = false;
    this.showRejectModal = false;
    this.selectedCredit = null;
  }
}
