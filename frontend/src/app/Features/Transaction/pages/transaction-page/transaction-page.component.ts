import { Component, OnInit, OnDestroy, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../../../services/transaction.service';
import { Transaction } from '../../../../shared/models/transaction.model';
import { TransactionType } from '../../../../shared/enums/transaction-type.enum';
import { AccountStatisticsDTO } from '../../../../shared/dto/account-statistics.dto';

declare var Chart: any;

@Component({
  selector: 'app-transaction-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-page.component.html',
  styleUrls: ['./transaction-page.component.css']
})
export class TransactionPageComponent implements OnInit, OnDestroy {

  transactions: Transaction[] = [];
  selectedTransaction: Transaction | null = null;
  isFormVisible = false;
  isEditMode = false;
  isTransferVisible = false;
  isStatsVisible = false;
  isPdfVisible = false;
  TransactionType = TransactionType;

  formData: Partial<Transaction> = { amount: 0, type: TransactionType.DEPOSIT };
  transferData = { fromAccountId: 0, toAccountId: 0, amount: 0 };
  selectedAccountId: number = 0;

  stats: AccountStatisticsDTO | null = null;
  statsAccountId: number = 0;
  statsPeriod: string = 'all';
  isLoadingStats: boolean = false;
  currentDate: Date = new Date();
  chartsReady = false; // ✅ nouveau flag

  pdfAccountId: number = 0;

  private barChart: any = null;
  private doughnutChart: any = null;

  constructor(
    private transactionService: TransactionService,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadTransactions();
    this.loadChartJs(); // ✅ charger Chart.js au démarrage
  }

  ngOnDestroy(): void {
    if (this.barChart) this.barChart.destroy();
    if (this.doughnutChart) this.doughnutChart.destroy();
  }

  // ✅ Charger Chart.js dynamiquement si pas déjà présent
  private loadChartJs(): void {
    if (typeof Chart !== 'undefined') return;

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js';
    script.onload = () => console.log('Chart.js chargé');
    document.head.appendChild(script);
  }

  loadTransactions(): void {
    this.transactionService.getAllTransactions().subscribe({
      next: (data) => this.transactions = data,
      error: (err) => console.error('Error loading transactions', err)
    });
  }

  openCreateForm(): void {
    this.isEditMode = false;
    this.formData = { amount: 0, type: TransactionType.DEPOSIT };
    this.selectedAccountId = 0;
    this.isFormVisible = true;
    this.isTransferVisible = false;
    this.isStatsVisible = false;
    this.isPdfVisible = false;
  }

  openEditForm(transaction: Transaction): void {
    this.isEditMode = true;
    this.formData = { ...transaction };
    this.selectedTransaction = transaction;
    this.isFormVisible = true;
    this.isTransferVisible = false;
    this.isStatsVisible = false;
    this.isPdfVisible = false;
  }

  openTransferForm(): void {
    this.isTransferVisible = true;
    this.isFormVisible = false;
    this.isStatsVisible = false;
    this.isPdfVisible = false;
    this.transferData = { fromAccountId: 0, toAccountId: 0, amount: 0 };
  }

  openStats(): void {
    this.isStatsVisible = true;
    this.isFormVisible = false;
    this.isTransferVisible = false;
    this.isPdfVisible = false;
    this.stats = null;
    this.chartsReady = false;
  }

  openPdfDialog(): void {
    this.isPdfVisible = true;
    this.isFormVisible = false;
    this.isTransferVisible = false;
    this.isStatsVisible = false;
    this.pdfAccountId = 0;
  }

  cancelPdf(): void {
    this.isPdfVisible = false;
    this.pdfAccountId = 0;
  }

  loadStats(): void {
    if (!this.statsAccountId) {
      alert('Please enter an Account ID!');
      return;
    }

    this.isLoadingStats = true;
    this.chartsReady = false;
    this.currentDate = new Date();

    // Détruire les anciens charts
    if (this.barChart) { this.barChart.destroy(); this.barChart = null; }
    if (this.doughnutChart) { this.doughnutChart.destroy(); this.doughnutChart = null; }

    this.transactionService.getAccountStatistics(this.statsAccountId).subscribe({
      next: (data) => {
        this.stats = data;
        this.isLoadingStats = false;

        // ✅ Attendre que Angular rende le *ngIf="stats" + les canvas
        this.ngZone.runOutsideAngular(() => {
          setTimeout(() => {
            this.ngZone.run(() => {
              this.chartsReady = true;
              setTimeout(() => this.renderCharts(), 50);
            });
          }, 200);
        });
      },
      error: (err) => {
        console.error('Status:', err.status);
        console.error('Error body:', err.error);
        this.isLoadingStats = false;
        alert(`Erreur ${err.status}: Compte introuvable. Vérifiez l'ID.`);
      }
    });
  }

  getDepositWithdrawRatio(): number {
    if (!this.stats) return 0;
    if (this.stats.totalWithdrawals === 0) return this.stats.totalDeposits;
    return this.stats.totalDeposits / this.stats.totalWithdrawals;
  }

  getRatioLabel(): string {
    const ratio = this.getDepositWithdrawRatio();
    if (ratio > 2) return 'Very good balance';
    if (ratio > 1) return 'Good balance';
    if (ratio === 1) return 'Perfect balance';
    return 'Overdraft warning';
  }

  renderCharts(): void {
    if (!this.stats) return;

    // ✅ Vérifier que Chart.js est disponible
    if (typeof Chart === 'undefined') {
      console.error('Chart.js non disponible, retry dans 500ms');
      setTimeout(() => this.renderCharts(), 500);
      return;
    }

    // Détruire si existants
    if (this.barChart) { this.barChart.destroy(); this.barChart = null; }
    if (this.doughnutChart) { this.doughnutChart.destroy(); this.doughnutChart = null; }

    // ✅ Bar Chart
    const barCtx = document.getElementById('barChart') as HTMLCanvasElement;
    if (barCtx) {
      this.barChart = new Chart(barCtx, {
        type: 'bar',
        data: {
          labels: ['Total Deposits', 'Total Withdrawals', 'Current Balance'],
          datasets: [{
            label: 'Amount (TND)',
            data: [
              this.stats.totalDeposits,
              this.stats.totalWithdrawals,
              this.stats.currentBalance
            ],
            backgroundColor: [
              'rgba(79, 70, 229, 0.8)',
              'rgba(212, 195, 241, 0.8)',
              'rgba(99, 102, 241, 0.8)'
            ],
            borderColor: ['#4f46e5', '#7c3aed', '#aeafea'],
            borderWidth: 2,
            borderRadius: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            title: {
              display: true,
              text: 'Financial Overview',
              font: { size: 14, weight: 'bold' },
              color: '#1e1b4b'
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              grid: { color: '#ede9fe' },
              title: { display: true, text: 'Amount (TND)', color: '#4f46e5' }
            },
            x: {
              grid: { display: false },
              ticks: { color: '#4f46e5' }
            }
          }
        }
      });
    } else {
      console.error('Canvas #barChart introuvable dans le DOM');
    }

    // ✅ Doughnut Chart
    const doughnutCtx = document.getElementById('doughnutChart') as HTMLCanvasElement;
    if (doughnutCtx) {
      this.doughnutChart = new Chart(doughnutCtx, {
        type: 'doughnut',
        data: {
          labels: ['Deposits', 'Withdrawals'],
          datasets: [{
            data: [
              this.stats.totalDepositCount,
              this.stats.totalWithdrawalCount
            ],
            backgroundColor: [
              'rgba(79, 70, 229, 0.8)',
              'rgba(212, 195, 241, 0.8)'
            ],
            borderColor: ['#4f46e5', '#7c3aed'],
            borderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: { color: '#4f46e5', font: { weight: 'bold' } }
            },
            title: {
              display: true,
              text: 'Transaction Distribution',
              font: { size: 14, weight: 'bold' },
              color: '#1e1b4b'
            }
          }
        }
      });
    } else {
      console.error('Canvas #doughnutChart introuvable dans le DOM');
    }
  }

  submitForm(): void {
    if (this.isEditMode && this.selectedTransaction?.transactionId) {
      this.transactionService.updateTransaction(
        this.selectedTransaction.transactionId, this.formData as Transaction
      ).subscribe({
        next: () => { this.loadTransactions(); this.isFormVisible = false; },
        error: (err) => console.error('Error updating transaction', err)
      });
    } else {
      if (!this.selectedAccountId) { alert('Please enter an Account ID!'); return; }
      this.transactionService.createTransaction(
        this.selectedAccountId, this.formData as Transaction
      ).subscribe({
        next: () => { this.loadTransactions(); this.isFormVisible = false; },
        error: (err) => console.error('Error creating transaction', err)
      });
    }
  }

  submitTransfer(): void {
    this.transactionService.transfer(
      this.transferData.fromAccountId,
      this.transferData.toAccountId,
      this.transferData.amount
    ).subscribe({
      next: () => { this.loadTransactions(); this.isTransferVisible = false; },
      error: (err) => console.error('Error during transfer', err)
    });
  }

  deleteTransaction(id: number): void {
    if (confirm('Confirm deletion?')) {
      this.transactionService.deleteTransaction(id).subscribe({
        next: () => this.loadTransactions(),
        error: (err) => console.error('Error deleting transaction', err)
      });
    }
  }

  downloadStatement(accountId: number): void {
    if (!accountId) { alert('Account ID not found!'); return; }
    this.transactionService.getAccountStatement(accountId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `account_statement_${accountId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error downloading PDF', err);
        alert('Error generating PDF. Please check if the account exists.');
      }
    });
  }

  downloadStatementByAccountId(): void {
    if (!this.pdfAccountId) { alert('Please enter an Account ID!'); return; }
    this.downloadStatement(this.pdfAccountId);
    this.cancelPdf();
  }

  cancelForm(): void {
    this.isFormVisible = false;
    this.isTransferVisible = false;
    this.isStatsVisible = false;
    this.isPdfVisible = false;
    this.stats = null;
    this.chartsReady = false;
    if (this.barChart) { this.barChart.destroy(); this.barChart = null; }
    if (this.doughnutChart) { this.doughnutChart.destroy(); this.doughnutChart = null; }
  }
}