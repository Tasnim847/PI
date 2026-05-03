// agent-claims-details.component.ts - Version corrigée
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface ClaimDetail {
  claimId: number;
  claimDate: string;
  claimedAmount: number;
  approvedAmount: number;
  description: string;
  status: string;
  fraud: boolean;
  message?: string;
  compensationId?: number;  // Ajouté
  contractId?: number;       // Ajouté
  client?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    telephone: string;
    address?: string;
    cin?: string;
  };
  contract?: {
    contractId: number;
    contractNumber: string;
    type: string;
    startDate: string;
    endDate: string;
    premium: number;
  };
  documents?: DocumentDetail[];
  autoDetails?: any;
  healthDetails?: any;
  homeDetails?: any;
  compensation?: {
    compensationId: number;
    amount: number;
    status: string;
    paymentDate?: string;
  };
}

export interface DocumentDetail {
  documentId: number;
  name: string;
  type: string;
  filePath: string;
  uploadDate: string;
  status: string;
}

@Component({
  selector: 'app-agent-claims-details',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './agent-claims-details.component.html',
  styleUrls: ['./agent-claims-details.component.css']
})
export class AgentClaimsDetailsComponent implements OnInit {
  claimId: number | null = null;
  claim: ClaimDetail | null = null;
  loading = false;
  error: string | null = null;
  activeTab: string = 'details';
  
  approvedAmount: number | null = null;
  rejectionReason: string = '';
  showApproveModal = false;
  showRejectModal = false;

  private documentObj: Document = document; // ✅ Solution pour l'erreur createElement

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.claimId = +params['id'];
      if (this.claimId) {
        this.loadClaimDetails();
      }
    });
  }


  loadClaimDetails() {
    this.loading = true;
    this.error = null;
  
    console.log('🔍 Loading claim details for ID:', this.claimId);
  
    this.http.get<any>(`http://localhost:8081/claims/getClaim/${this.claimId}`)
      .subscribe({
        next: (data) => {
          console.log('✅ Claim data received:', data);
          console.log('📄 Documents:', data.documents);
          console.log('👤 Client:', data.client);
          console.log('📋 Contract ID:', data.contractId);
        
          // Initialiser le claim avec les données reçues
          this.claim = {
            claimId: data.claimId,
            claimDate: data.claimDate,
            claimedAmount: data.claimedAmount,
            approvedAmount: data.approvedAmount,
            description: data.description,
            status: data.status,
            fraud: data.fraud || false,
            message: data.message,
            compensationId: data.compensationId,
            contractId: data.contractId,
            client: data.client,
            documents: data.documents,
            autoDetails: data.autoDetails,
            healthDetails: data.healthDetails,
            homeDetails: data.homeDetails,
            compensation: data.compensation,
            contract: undefined  // Initialement undefined
          };
        
          this.approvedAmount = data.approvedAmount || data.claimedAmount;
        
          // Si contractId existe, charger les détails du contrat
          if (data.contractId) {
            this.loadContractDetails(data.contractId);
          } else {
            this.loading = false;
          }
        },
        error: (err) => {
          console.error('❌ API Error:', err);
          this.error = `Erreur: ${err.status} - ${err.message}`;
          this.loading = false;
        }
     });
  }


  loadContractDetails(contractId: number) {
     console.log('🔍 Loading contract details for ID:', contractId);
  
    // Utiliser le même endpoint que dans ContractService
    const contractEndpoint = `http://localhost:8081/contrats/getCont/${contractId}`;
  
    console.log('📡 Calling contract endpoint:', contractEndpoint);
  
    this.http.get<any>(contractEndpoint).subscribe({
      next: (contractData) => {
        console.log('✅ Contract data received:', contractData);
      
        // Mapper les données du contrat selon la structure retournée
        if (this.claim && contractData) {
          this.claim.contract = {
            contractId: contractData.contractId || contractData.id,
            contractNumber: contractData.contractNumber || contractData.number || `CON-${contractId}`,
            type: contractData.type || contractData.contractType || 'Unknown',
            startDate: contractData.startDate,
            endDate: contractData.endDate,
            premium: contractData.premium || contractData.monthlyPremium || 0
          };
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Error loading contract:', err);
        console.log('Contract ID was:', contractId);
        console.log('Endpoint tried:', contractEndpoint);
      
        // Option: Afficher le contractId même sans les détails
        if (this.claim) {
          this.claim.contract = {
            contractId: contractId,
            contractNumber: `CON-${contractId}`,
            type: 'Loading...',
            startDate: new Date().toISOString(),
            endDate: new Date().toISOString(),
            premium: 0
          };
        }
        this.loading = false;
      
        // Afficher un message plus informatif
        this.error = `Contract details not available for ID: ${contractId}. Please check the contract service endpoint.`;
      }
    });
  }

  getStatusLabel(status: string): string {
    const labels: { [key: string]: string } = {
      'IN_REVIEW': 'En révision',
      'APPROVED': 'Approuvé',
      'REJECTED': 'Rejeté',
      'COMPENSATED': 'Compensé',
      'PAID': 'Payé'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: { [key: string]: string } = {
      'IN_REVIEW': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger',
      'COMPENSATED': 'info',
      'PAID': 'success'
    };
    return classes[status] || 'secondary';
  }

  getDocumentIcon(type: string): string {
    const ext = type?.toLowerCase() || '';
    if (ext.includes('pdf')) return 'fa-file-pdf';
    if (ext.includes('image') || ext.includes('jpg') || ext.includes('png')) return 'fa-file-image';
    if (ext.includes('word') || ext.includes('doc')) return 'fa-file-word';
    if (ext.includes('excel') || ext.includes('xls')) return 'fa-file-excel';
    return 'fa-file';
  }

  getDocumentColor(type: string): string {
    const ext = type?.toLowerCase() || '';
    if (ext.includes('pdf')) return 'danger';
    if (ext.includes('image')) return 'info';
    if (ext.includes('word')) return 'primary';
    if (ext.includes('excel')) return 'success';
    return 'secondary';
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return 'Unknown';
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${sizes[i]}`;
  }

  // agent-claims-details.component.ts - Mise à jour des méthodes

  viewDocument(document: DocumentDetail) {
    // Utiliser le nouvel endpoint de téléchargement
    const fileUrl = `http://localhost:8081/claims/download/${document.documentId}`;
    window.open(fileUrl, '_blank');
  }

  downloadDocument(document: DocumentDetail) {
    const link = this.documentObj.createElement('a');
    const fileUrl = `http://localhost:8081/claims/download/${document.documentId}`;
    link.href = fileUrl;
    link.download = document.name;
    link.target = '_blank';
    link.click();
  }

  // Ajoutez une méthode pour vérifier les fichiers (debug)
  checkFiles() {
    this.http.get('http://localhost:8081/claims/debug/files')
        .subscribe({
            next: (data) => console.log('📁 Debug files:', data),
            error: (err) => console.error('Debug error:', err)
        });
  }

  approveClaim() {
    if (!this.approvedAmount || this.approvedAmount <= 0) {
      alert('Veuillez entrer un montant valide');
      return;
    }

    if (this.approvedAmount > (this.claim?.claimedAmount || 0)) {
      alert(`Le montant approuvé ne peut pas dépasser ${this.claim?.claimedAmount} DT`);
      return;
    }

    this.loading = true;
    this.http.post(`http://localhost:8081/claims/approve/${this.claimId}`, null, {
      params: { approvedAmount: this.approvedAmount.toString() }
    }).subscribe({
      next: () => {
        alert('✅ Réclamation approuvée avec succès !');
        this.loadClaimDetails();
        this.showApproveModal = false;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  rejectClaim() {
    if (!this.rejectionReason || this.rejectionReason.trim() === '') {
      alert('Veuillez entrer une raison pour le rejet');
      return;
    }

    this.loading = true;
    this.http.post(`http://localhost:8081/claims/reject/${this.claimId}`, null, {
      params: { reason: this.rejectionReason }
    }).subscribe({
      next: () => {
        alert('❌ Réclamation rejetée');
        this.loadClaimDetails();
        this.showRejectModal = false;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        alert('Erreur: ' + (err.error?.message || err.message));
      }
    });
  }

  goBack() {
    this.router.navigate(['/public/agent/claims']);
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  getCompletionPercentage(): number {
    if (!this.claim) return 0;
    let completed = 0;
    let total = 4;
    
    if (this.claim.client) completed++;
    if (this.claim.contract) completed++;
    if (this.claim.documents && this.claim.documents.length > 0) completed++;
    if (this.claim.description) completed++;
    
    return (completed / total) * 100;
  }
}