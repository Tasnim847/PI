// src/app/Features/Insurance/pages/client/my-contracts/my-contracts.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ContractService } from '../../../services/contract.service';
import { ToastrService } from 'ngx-toastr';
import { saveAs } from 'file-saver';

@Component({
  selector: 'app-my-contracts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './my-contracts.component.html',
  styleUrls: ['./my-contracts.component.css']
})
export class MyContractsComponent implements OnInit {
  contracts: any[] = [];
  filteredContracts: any[] = [];
  selectedStatus: string = 'ALL';
  statuses = ['ALL', 'ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;

  constructor(
    private contractService: ContractService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    this.isLoading = true;
    this.contractService.getMyContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.filterContracts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement des contrats');
        this.isLoading = false;
      }
    });
  }

  filterContracts(): void {
    if (this.selectedStatus === 'ALL') {
      this.filteredContracts = this.contracts;
    } else {
      this.filteredContracts = this.contracts.filter(
        c => c.status === this.selectedStatus
      );
    }
  }

  getStatusBadgeClass(status: string): string {
    const classes: {[key: string]: string} = {
      'ACTIVE': 'bg-success',
      'INACTIVE': 'bg-warning',
      'COMPLETED': 'bg-info',
      'CANCELLED': 'bg-danger',
      'EXPIRED': 'bg-secondary'
    };
    return classes[status] || 'bg-secondary';
  }

  getStatusLabel(status: string): string {
    const labels: {[key: string]: string} = {
      'ACTIVE': 'Actif',
      'INACTIVE': 'En attente',
      'COMPLETED': 'Terminé',
      'CANCELLED': 'Annulé',
      'EXPIRED': 'Expiré'
    };
    return labels[status] || status;
  }

  viewRisk(contract: any): void {
    this.selectedContract = contract;
    this.contractService.getContractRisk(contract.contractId).subscribe({
      next: (riskData) => {
        contract.riskClaim = riskData.riskEvaluation;
        this.showRiskModal = true;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du chargement de l\'évaluation du risque');
      }
    });
  }

  downloadPdf(contractId: number): void {
    this.contractService.downloadContractPdf(contractId).subscribe({
      next: (blob) => {
        // Téléchargement avec file-saver
        saveAs(blob, `contrat_${contractId}.pdf`);
        this.toastr.success('PDF téléchargé avec succès');
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.toastr.error('Erreur lors du téléchargement du PDF');
      }
    });
  }

  cancelContract(contract: any): void {
    if (confirm('Êtes-vous sûr de vouloir annuler ce contrat ?')) {
      this.contractService.deleteContract(contract.contractId).subscribe({
        next: () => {
          this.toastr.success('Contrat annulé avec succès');
          this.loadContracts();
        },
        error: (err) => {
          console.error('Erreur:', err);
          this.toastr.error('Erreur lors de l\'annulation du contrat');
        }
      });
    }
  }

  // Méthode helper pour formater les dates manuellement (alternative)
  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR');
  }
}