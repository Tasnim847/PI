import { Component, OnInit, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';

export interface Repayment {
  repaymentId?: number;
  id?: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  status: string;
  credit?: any;
  client?: any;
  reference?: string;
  creditId?: number;
  clientId?: number;
}

@Component({
  selector: 'app-admin-repayment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-repayment.component.html',
  styleUrl: './admin-repayment.component.css'
})
export class AdminRepaymentComponent implements OnInit {
  // ========== DONNÉES ==========
  allRepayments: Repayment[] = [];
  filteredRepayments: Repayment[] = [];
  allClients: any[] = [];
  allCredits: any[] = [];
  
  // ========== FILTRES ==========
  searchTerm: string = '';
  filterStatus: string = '';
  filterMethod: string = '';
  
  // ========== ÉTATS ==========
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  private apiUrl = 'http://localhost:8081/Repayment';
  private creditApiUrl = 'http://localhost:8081/Credit';
  private clientApiUrl = 'http://localhost:8081/Client';

  constructor(
    private http: HttpClient,
    private toastr: ToastrService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadAllData();
    }
  }

  // ========== LOAD ALL DATA ==========
  loadAllData(): void {
    this.isLoading = true;
    
    // Load credits and repayments in parallel
    Promise.all([
      this.loadCredits(),
      this.loadRepayments()
    ]).then(() => {
      // Associate credits (which contain clients) with repayments
      this.enrichRepayments();
      this.isLoading = false;
    }).catch(err => {
      console.error('Error loading data:', err);
      this.isLoading = false;
    });
  }

  // ========== LOAD CLIENTS ==========
  loadClients(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<any[]>(`${this.clientApiUrl}/getAllClients`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allClients = data || [];
          console.log('Clients loaded:', this.allClients.length);
          resolve();
        },
        error: (err) => {
          console.error('Error loading clients:', err);
          this.allClients = [];
          resolve(); // Continue even on error
        }
      });
    });
  }

  // ========== LOAD CREDITS ==========
  loadCredits(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<any[]>(`${this.creditApiUrl}/allCredit`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allCredits = data || [];
          console.log('Credits loaded:', this.allCredits.length);
          resolve();
        },
        error: (err) => {
          console.error('Error loading credits:', err);
          this.allCredits = [];
          resolve(); // Continue even on error
        }
      });
    });
  }

  // ========== LOAD REPAYMENTS ==========
  loadRepayments(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<Repayment[]>(`${this.apiUrl}/allRepayment`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allRepayments = data || [];
          console.log('Repayments loaded:', this.allRepayments.length);
          resolve();
        },
        error: (err) => {
          console.error('Error loading repayments:', err);
          this.allRepayments = [];
          reject(err);
        }
      });
    });
  }

  // ========== ENRICH REPAYMENTS WITH CLIENT AND CREDIT ==========
  enrichRepayments(): void {
    // Create a creditId -> credit map for quick access
    const creditMap = new Map<number, any>();
    
    for (const credit of this.allCredits) {
      if (credit.creditId) {
        creditMap.set(credit.creditId, credit);
      }
    }
    
    // Enrich repayments
    this.allRepayments.forEach(repayment => {
      // Try to find creditId in several ways:
      // 1. Directly in the repayment
      // 2. Via the reference (if it contains the ID)
      // 3. Via search in credits
      
      let creditId = repayment.creditId;
      
      // If no direct creditId, try to extract from reference
      if (!creditId && repayment.reference) {
        // Reference could be in format "CREDIT_123" or similar
        const match = repayment.reference.match(/\d+/);
        if (match) {
          creditId = parseInt(match[0], 10);
        }
      }
      
      // If we found a creditId, search for corresponding credit
      if (creditId) {
        const credit = creditMap.get(creditId);
        if (credit) {
          repayment.creditId = creditId;
          repayment.credit = credit;
          repayment.client = credit.client;
        }
      }
    });
    
    this.filteredRepayments = [...this.allRepayments];
    
    console.log('=== ENRICHMENT COMPLETE ===');
    console.log('With credit ID:', this.allRepayments.filter(r => r.creditId).length);
    console.log('Total repayments:', this.allRepayments.length);
    
    // Display an example
    const withCredit = this.allRepayments.find(r => r.creditId);
    if (withCredit) {
      console.log('Example with credit:', withCredit);
      console.log('Credit ID:', withCredit.creditId);
    } else {
      console.log('No repayment with credit found');
      // Display first repayments for debug
      if (this.allRepayments.length > 0) {
        console.log('First repayment:', this.allRepayments[0]);
        console.log('Available keys:', Object.keys(this.allRepayments[0]));
      }
    }
  }

  // ========== CHARGER TOUS LES REMBOURSEMENTS (ADMIN) ==========
  loadAllRepayments(): void {
    this.loadAllData();
  }

  // ========== FILTRER LES REMBOURSEMENTS ==========
  applyFilters(): void {
    this.filteredRepayments = this.allRepayments.filter(repayment => {
      const repaymentId = repayment.repaymentId || repayment.id;
      const matchesSearch = !this.searchTerm || 
        (repaymentId ? repaymentId.toString().includes(this.searchTerm) : false) ||
        repayment.client?.firstName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        repayment.client?.lastName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        repayment.amount.toString().includes(this.searchTerm);
      
      const matchesStatus = !this.filterStatus || repayment.status === this.filterStatus;
      const matchesMethod = !this.filterMethod || repayment.paymentMethod === this.filterMethod;
      
      return matchesSearch && matchesStatus && matchesMethod;
    });
  }

  // ========== DELETE REPAYMENT (ADMIN) ==========
  deleteRepayment(repaymentId: number | undefined): void {
    if (!repaymentId) {
      this.errorMessage = 'Invalid repayment ID';
      this.toastr.error(this.errorMessage);
      return;
    }

    if (!confirm('Are you sure you want to delete this repayment?')) {
      return;
    }

    this.http.delete(`${this.apiUrl}/deleteRepayment/${repaymentId}`, {
      headers: this.getHeaders()
    }).subscribe({
      next: () => {
        this.successMessage = 'Repayment deleted successfully';
        this.toastr.success(this.successMessage);
        this.loadAllRepayments();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Error deleting repayment';
        this.toastr.error(this.errorMessage);
      }
    });
  }

  // ========== UTILITAIRES ==========
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
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

  getTotalAmount(): number {
    return this.filteredRepayments.reduce((sum, r) => sum + r.amount, 0);
  }

  // ========== DOWNLOAD AMORTIZATION PDF ==========
  downloadAmortissementPdf(creditId: number | undefined): void {
    if (!creditId) {
      this.errorMessage = 'Invalid credit ID';
      this.toastr.error(this.errorMessage);
      return;
    }

    this.http.get(`${this.apiUrl}/credits/${creditId}/amortissement/pdf`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `amortissement_credit_${creditId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        
        this.successMessage = 'PDF downloaded successfully';
        this.toastr.success(this.successMessage);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Error downloading PDF';
        this.toastr.error(this.errorMessage);
      }
    });
  }

  // ========== SEND AMORTIZATION PDF BY EMAIL ==========
  sendAmortissementPdfByEmail(creditId: number | undefined): void {
    if (!creditId) {
      this.errorMessage = 'Invalid credit ID';
      this.toastr.error(this.errorMessage);
      return;
    }

    // Find corresponding repayment to display client email
    const repayment = this.allRepayments.find(r => r.creditId === creditId);
    const clientEmail = repayment?.client?.email || 'Email not found';
    
    const confirmMessage = `Are you sure you want to send the amortization PDF by email to the client?\n\nEmail: ${clientEmail}`;
    
    if (!confirm(confirmMessage)) {
      return;
    }

    console.log(`Sending PDF for credit ID: ${creditId}`);
    console.log(`Client email: ${clientEmail}`);
    console.log(`URL: ${this.apiUrl}/credits/${creditId}/send-pdf-email`);

    this.http.post(`${this.apiUrl}/credits/${creditId}/send-pdf-email`, {}, {
      headers: this.getHeaders()
    }).subscribe({
      next: (response: any) => {
        console.log('Server response:', response);
        this.successMessage = `PDF sent by email successfully to ${clientEmail}`;
        this.toastr.success(this.successMessage);
      },
      error: (err) => {
        console.error('Complete error:', err);
        console.error('Status:', err.status);
        console.error('Error body:', err.error);
        
        let errorMsg = 'Error sending PDF';
        
        if (err.status === 403) {
          errorMsg = 'Access denied - You must be admin to send emails';
        } else if (err.status === 404) {
          errorMsg = 'Credit not found';
        } else if (err.error?.error) {
          errorMsg = err.error.error;
        } else if (err.error?.message) {
          errorMsg = err.error.message;
        }
        
        this.errorMessage = errorMsg;
        this.toastr.error(this.errorMessage);
      }
    });
  }
}
