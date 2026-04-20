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

  // ========== CHARGER TOUTES LES DONNÉES ==========
  loadAllData(): void {
    this.isLoading = true;
    
    // Charger les crédits et remboursements en parallèle
    Promise.all([
      this.loadCredits(),
      this.loadRepayments()
    ]).then(() => {
      // Associer les crédits (qui contiennent les clients) aux remboursements
      this.enrichRepayments();
      this.isLoading = false;
    }).catch(err => {
      console.error('Erreur chargement données:', err);
      this.isLoading = false;
    });
  }

  // ========== CHARGER LES CLIENTS ==========
  loadClients(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<any[]>(`${this.clientApiUrl}/getAllClients`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allClients = data || [];
          console.log('Clients chargés:', this.allClients.length);
          resolve();
        },
        error: (err) => {
          console.error('Erreur chargement clients:', err);
          this.allClients = [];
          resolve(); // Continue même en cas d'erreur
        }
      });
    });
  }

  // ========== CHARGER LES CRÉDITS ==========
  loadCredits(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<any[]>(`${this.creditApiUrl}/allCredit`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allCredits = data || [];
          console.log('Crédits chargés:', this.allCredits.length);
          resolve();
        },
        error: (err) => {
          console.error('Erreur chargement crédits:', err);
          this.allCredits = [];
          resolve(); // Continue même en cas d'erreur
        }
      });
    });
  }

  // ========== CHARGER LES REMBOURSEMENTS ==========
  loadRepayments(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<Repayment[]>(`${this.apiUrl}/allRepayment`, {
        headers: this.getHeaders()
      }).subscribe({
        next: (data) => {
          this.allRepayments = data || [];
          console.log('Remboursements chargés:', this.allRepayments.length);
          resolve();
        },
        error: (err) => {
          console.error('Erreur chargement remboursements:', err);
          this.allRepayments = [];
          reject(err);
        }
      });
    });
  }

  // ========== ENRICHIR LES REMBOURSEMENTS AVEC CLIENT ET CRÉDIT ==========
  enrichRepayments(): void {
    // Créer une map creditId -> credit pour accès rapide
    const creditMap = new Map<number, any>();
    
    for (const credit of this.allCredits) {
      if (credit.creditId) {
        creditMap.set(credit.creditId, credit);
      }
    }
    
    // Enrichir les remboursements
    this.allRepayments.forEach(repayment => {
      // Essayer de trouver le creditId de plusieurs façons:
      // 1. Directement dans le repayment
      // 2. Via la référence (si elle contient l'ID)
      // 3. Via la recherche dans les crédits
      
      let creditId = repayment.creditId;
      
      // Si pas de creditId direct, essayer d'extraire de la référence
      if (!creditId && repayment.reference) {
        // La référence pourrait être au format "CREDIT_123" ou similaire
        const match = repayment.reference.match(/\d+/);
        if (match) {
          creditId = parseInt(match[0], 10);
        }
      }
      
      // Si on a trouvé un creditId, chercher le crédit correspondant
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
    
    console.log('=== ENRICHISSEMENT TERMINÉ ===');
    console.log('Avec crédit ID:', this.allRepayments.filter(r => r.creditId).length);
    console.log('Total remboursements:', this.allRepayments.length);
    
    // Afficher un exemple
    const withCredit = this.allRepayments.find(r => r.creditId);
    if (withCredit) {
      console.log('Exemple avec crédit:', withCredit);
      console.log('Credit ID:', withCredit.creditId);
    } else {
      console.log('Aucun remboursement avec crédit trouvé');
      // Afficher les premiers remboursements pour debug
      if (this.allRepayments.length > 0) {
        console.log('Premier remboursement:', this.allRepayments[0]);
        console.log('Clés disponibles:', Object.keys(this.allRepayments[0]));
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

  // ========== SUPPRIMER UN REMBOURSEMENT (ADMIN) ==========
  deleteRepayment(repaymentId: number | undefined): void {
    if (!repaymentId) {
      this.errorMessage = 'ID de remboursement invalide';
      this.toastr.error(this.errorMessage);
      return;
    }

    if (!confirm('Êtes-vous sûr de vouloir supprimer ce remboursement ?')) {
      return;
    }

    this.http.delete(`${this.apiUrl}/deleteRepayment/${repaymentId}`, {
      headers: this.getHeaders()
    }).subscribe({
      next: () => {
        this.successMessage = 'Remboursement supprimé avec succès';
        this.toastr.success(this.successMessage);
        this.loadAllRepayments();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de la suppression';
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

  // ========== TÉLÉCHARGER LE PDF D'AMORTISSEMENT ==========
  downloadAmortissementPdf(creditId: number | undefined): void {
    if (!creditId) {
      this.errorMessage = 'ID de crédit invalide';
      this.toastr.error(this.errorMessage);
      return;
    }

    this.http.get(`${this.apiUrl}/credits/${creditId}/amortissement/pdf`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        // Créer un lien de téléchargement
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `amortissement_credit_${creditId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        
        this.successMessage = 'PDF téléchargé avec succès';
        this.toastr.success(this.successMessage);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors du téléchargement du PDF';
        this.toastr.error(this.errorMessage);
      }
    });
  }

  // ========== ENVOYER LE PDF PAR EMAIL ==========
  sendAmortissementPdfByEmail(creditId: number | undefined): void {
    if (!creditId) {
      this.errorMessage = 'ID de crédit invalide';
      this.toastr.error(this.errorMessage);
      return;
    }

    // Trouver le remboursement correspondant pour afficher l'email du client
    const repayment = this.allRepayments.find(r => r.creditId === creditId);
    const clientEmail = repayment?.client?.email || 'Email non trouvé';
    
    const confirmMessage = `Êtes-vous sûr de vouloir envoyer le PDF d'amortissement par email au client ?\n\nEmail: ${clientEmail}`;
    
    if (!confirm(confirmMessage)) {
      return;
    }

    console.log(`Envoi du PDF pour le crédit ID: ${creditId}`);
    console.log(`Email du client: ${clientEmail}`);
    console.log(`URL: ${this.apiUrl}/credits/${creditId}/send-pdf-email`);

    this.http.post(`${this.apiUrl}/credits/${creditId}/send-pdf-email`, {}, {
      headers: this.getHeaders()
    }).subscribe({
      next: (response: any) => {
        console.log('Réponse du serveur:', response);
        this.successMessage = `PDF envoyé par email avec succès à ${clientEmail}`;
        this.toastr.success(this.successMessage);
      },
      error: (err) => {
        console.error('Erreur complète:', err);
        console.error('Status:', err.status);
        console.error('Error body:', err.error);
        
        let errorMsg = 'Erreur lors de l\'envoi du PDF';
        
        if (err.status === 403) {
          errorMsg = 'Accès refusé - Vous devez être admin pour envoyer des emails';
        } else if (err.status === 404) {
          errorMsg = 'Crédit non trouvé';
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
