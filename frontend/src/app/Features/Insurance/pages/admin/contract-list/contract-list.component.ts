// contract-list.component.ts (admin)
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ContractService } from '../../../services/contract.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './contract-list.component.html',
  styleUrls: ['./contract-list.component.css']
})
export class ContractListComponent implements OnInit {
  contracts: any[] = [];
  filteredContracts: any[] = [];
  paginatedContracts: any[] = [];
  selectedStatus: string = 'ALL';
  searchTerm: string = '';
  isLoading = false;
  selectedContract: any = null;
  showRiskModal = false;
  showSimulateModal = false;
  simulateMonths: number = 1;
  isCheckingDelays = false; // Pour éviter les doubles clics

  // Pagination properties
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 1;
  pages: number[] = [];

  Math = Math;

  statuses = ['ALL', 'ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED'];

  constructor(
    private contractService: ContractService,
    private toastr: ToastrService,
    private router: Router 
  ) {}

  ngOnInit(): void {
    this.loadContracts();
  }

  loadContracts(): void {
    this.isLoading = true;
    this.contractService.getAllContracts().subscribe({
      next: (contracts) => {
        this.contracts = contracts;
        this.filterContracts();
        this.isLoading = false;
      },
      error: (err) => {
        this.toastr.error('Erreur lors du chargement des contrats');
        this.isLoading = false;
      }
    });
  }

  filterContracts(): void {
    this.filteredContracts = this.contracts.filter(contract => {
      const matchStatus = this.selectedStatus === 'ALL' || contract.status === this.selectedStatus;
      const matchSearch = !this.searchTerm || 
        contract.contractId.toString().includes(this.searchTerm) ||
        contract.client?.email?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        contract.product?.name?.toLowerCase().includes(this.searchTerm.toLowerCase());
      return matchStatus && matchSearch;
    });
    
    // Reset to first page when filtering
    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredContracts.length / this.itemsPerPage);
    this.pages = Array.from({ length: this.totalPages }, (_, i) => i + 1);
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedContracts = this.filteredContracts.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagination();
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.goToPage(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.goToPage(this.currentPage + 1);
    }
  }

  getPaymentPercentage(contract: any): number {
    if (!contract || !contract.premium || contract.premium === 0) {
      return 0;
    }
    const percentage = (contract.totalPaid / contract.premium) * 100;
    return Math.min(Math.max(percentage, 0), 100);
  }

  getStatusBadgeClass(status: string): string {
    const classes: any = {
      'ACTIVE': 'active',
      'INACTIVE': 'inactive',
      'COMPLETED': 'completed',
      'CANCELLED': 'cancelled',
      'EXPIRED': 'expired'
    };
    return classes[status] || 'inactive';
  }

  getStatusLabel(status: string): string {
    const labels: any = {
      'ACTIVE': 'Actif',
      'INACTIVE': 'En attente',
      'COMPLETED': 'Terminé',
      'CANCELLED': 'Annulé',
      'EXPIRED': 'Expiré'
    };
    return labels[status] || status;
  }

  viewRisk(contract: any): void {
    this.router.navigate(['/backoffice/insurance/contract-risk', contract.contractId]); 
  }

  activateContract(contract: any): void {
    if (confirm(`Activer le contrat #${contract.contractId} ?`)) {
      this.contractService.activateContract(contract.contractId).subscribe({
        next: () => {
          this.toastr.success('Contrat activé avec succès');
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error(err.error?.message || 'Erreur lors de l\'activation');
        }
      });
    }
  }

  rejectContract(contract: any): void {
    const reason = prompt('Raison du rejet :');
    if (reason) {
      this.contractService.rejectContract(contract.contractId, reason).subscribe({
        next: () => {
          this.toastr.success('Contrat rejeté');
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error('Erreur lors du rejet');
        }
      });
    }
  }

  simulateLatePayment(contract: any): void {
    this.selectedContract = contract;
    this.showSimulateModal = true;
  }

  confirmSimulate(): void {
    if (this.selectedContract && this.simulateMonths > 0) {
      this.contractService.simulateLatePayments(this.selectedContract.contractId, this.simulateMonths).subscribe({
        next: (response) => {
          this.toastr.success(`${this.simulateMonths} mois de retard simulés`);
          this.showSimulateModal = false;
          this.loadContracts();
        },
        error: (err) => {
          this.toastr.error('Erreur lors de la simulation');
        }
      });
    }
  }



  /**
    * Vérifie les retards et envoie les emails de rappel
    * Utilise l'endpoint existant /contrats/check-late-payments
  */
  runSystemCheck(): void {
    if (this.isCheckingDelays) {
      this.toastr.warning('Vérification en cours...');
      return;
    }

    this.isCheckingDelays = true;
    this.toastr.info('🔍 Vérification des retards de paiement en cours...', 'Patientez', { timeOut: 3000 });

    // Appel à l'endpoint existant checkLatePayments
    this.contractService.checkLatePayments().subscribe({
      next: (response: any) => {
        this.isCheckingDelays = false;
      
        console.log('Réponse du backend:', response);
      
        // Le backend retourne un message comme "Vérification des retards effectuée"
        const message = typeof response === 'string' ? response : response.message;
        const remindersSent = response.remindersSent || 0;
      
        if (remindersSent > 0) {
          this.toastr.success(
            `✅ ${remindersSent} email(s) de rappel envoyé(s) aux clients`, 
            'Emails envoyés !',
            { timeOut: 5000 }
          );
        } else {
          this.toastr.success(
            '✅ Vérification terminée - Les emails ont été envoyés si nécessaire', 
            'Succès',
            { timeOut: 4000 }
          );
        }
      
        // Recharger les contrats pour mettre à jour les statuts
        setTimeout(() => {
          this.loadContracts();
        }, 1000);
      },
      error: (err) => {
        this.isCheckingDelays = false;
        console.error('Erreur lors de la vérification:', err);
      
        let errorMessage = 'Erreur lors de la vérification des retards';
        if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (typeof err.error === 'string') {
          errorMessage = err.error;
        }
      
        this.toastr.error(errorMessage, 'Erreur');
      }
    });
  }

  /**
    * Vérification détaillée contrat par contrat
    * Affiche plus d'informations sur chaque contrat
  */
  runDetailedSystemCheck(): void {
    if (this.isCheckingDelays) {
      this.toastr.warning('Vérification en cours...');
      return;
    }

    this.isCheckingDelays = true;
    this.toastr.info('🔍 Analyse détaillée des contrats...', 'Vérification', { timeOut: 2000 });

    // Récupérer tous les contrats inactifs/en attente qui peuvent avoir des retards
    const contractsToCheck = this.filteredContracts.filter(c => 
      c.status === 'INACTIVE' || c.status === 'ACTIVE'
    );
  
    let totalChecked = 0;
    let errors = 0;

    if (contractsToCheck.length === 0) {
      this.isCheckingDelays = false;
      this.toastr.info('Aucun contrat à vérifier', 'Information');
      return;
    }

    // Pour chaque contrat, vérifier les retards
    contractsToCheck.forEach(contract => {
      this.contractService.checkContractLatePayments(contract.contractId).subscribe({
        next: (response: any) => {
          totalChecked++;
          console.log(`✅ Contrat #${contract.contractId} vérifié:`, response);
        
          if (totalChecked + errors === contractsToCheck.length) {
            this.isCheckingDelays = false;
            this.toastr.success(
              `✅ Vérification terminée: ${totalChecked} contrat(s) analysé(s)`,
              'Succès',
              { timeOut: 4000 }
            );
            this.loadContracts();
          }
        },
        error: (err) => {
          errors++;
          console.error(`❌ Erreur contrat #${contract.contractId}:`, err);
        
          if (totalChecked + errors === contractsToCheck.length) {
            this.isCheckingDelays = false;
            if (errors > 0) {
              this.toastr.warning(
                `Vérification terminée avec ${errors} erreur(s)`,
                'Partiellement réussi',
                { timeOut: 4000 }
              );
            } else {
              this.toastr.success('Vérification terminée avec succès', 'Succès');
            }
            this.loadContracts();
          }
        }
      });
    });
  }

  /**
    * Vérifie les contrats complétés
  */
  runCompletedCheck(): void {
    this.contractService.checkCompletedContracts().subscribe({
      next: (response) => {
        this.toastr.success('Vérification des contrats complétés effectuée');
        this.loadContracts();
      },
      error: (err) => {
        this.toastr.error('Erreur lors de la vérification');
      }
    });
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.filterContracts();
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR');
  }

  closeModal(): void {
    this.showRiskModal = false;
    this.showSimulateModal = false;
    this.selectedContract = null;
    this.simulateMonths = 1;
  }

  goToDashboard(): void {
    this.router.navigate(['/backoffice/insurance/admin-dashboard']);
  }

  // Ajoutez cette méthode dans votre ContractListComponent
  /**
    * Envoyer un email de rappel de paiement pour un contrat spécifique
    * Utilise l'endpoint /api/reminders/check-contract/{contractId}
   */
  sendPaymentReminder(contract: any): void {
    if (!contract || !contract.contractId) {
      this.toastr.error('Contrat invalide');
      return;
    }

    if (!confirm(`Envoyer un rappel de paiement pour le contrat #${contract.contractId} ?`)) {
      return;
    }

    this.isLoading = true;
    this.toastr.info(`📧 Vérification et envoi des rappels...`);

    // Utiliser l'endpoint existant qui fonctionne déjà
    this.contractService.checkContractLatePayments(contract.contractId).subscribe({
      next: (response: any) => {
        this.isLoading = false;
      
        if (response && typeof response === 'object') {
          if (response.message) {
            this.toastr.success(response.message, 'Succès');
          } else {
            this.toastr.success(`✅ Vérification terminée pour le contrat #${contract.contractId}`, 'Succès');
          }
        } else if (typeof response === 'string') {
          this.toastr.success(response, 'Succès');
        }
      
        this.loadContracts();
      },
      error: (err) => {
        this.isLoading = false;
        this.toastr.error('Erreur lors de l\'envoi', 'Erreur');
      }
    });
  }



  // Ajoutez ces propriétés dans votre composant :

  showReminderModal = false;
  selectedContractForReminder: any = null;
  selectedDaysBefore: number = 7; // Valeur par défaut
  customDaysBefore: number | null = null;
  isSendingReminder = false;

  // Ajoutez ces méthodes :

  /**
   * Ouvrir le modal de choix des jours pour le rappel
  */
  openReminderModal(contract: any): void {
    this.selectedContractForReminder = contract;
    this.selectedDaysBefore = 7; // Valeur par défaut
    this.customDaysBefore = null;
    this.showReminderModal = true;
  }

/**
 * Fermer le modal de rappel
 */
  closeReminderModal(): void {
    this.showReminderModal = false;
    this.selectedContractForReminder = null;
    this.selectedDaysBefore = 7;
    this.customDaysBefore = null;
    this.isSendingReminder = false;
  }

  /**
  * Définir les jours personnalisés
  */
  setCustomDays(): void {
    if (this.customDaysBefore !== null && this.customDaysBefore >= 0 && this.customDaysBefore <= 365) {
      this.selectedDaysBefore = this.customDaysBefore;
    }
  }

  /**
    * Obtenir le texte du rappel en fonction des jours
  */
  getReminderText(): string {
    if (this.selectedDaysBefore === 0) {
      return "AUJOURD'HUI";
    } else if (this.selectedDaysBefore === 1) {
      return "DEMAIN";
    } else {
      return `${this.selectedDaysBefore} JOURS`;
    }
  }

  /**
    * Envoyer le rappel avec le nombre de jours sélectionné
  */
  sendReminderWithDays(): void {
    if (!this.selectedContractForReminder || !this.selectedContractForReminder.contractId) {
      this.toastr.error('Contrat invalide');
      this.closeReminderModal();
      return;
    }

    if (this.selectedDaysBefore === null || this.selectedDaysBefore < 0) {
      this.toastr.error('Veuillez sélectionner un nombre de jours valide');
      return;
    }

    this.isSendingReminder = true;
     this.toastr.info(`📧 Envoi du rappel (J-${this.selectedDaysBefore}) pour le contrat #${this.selectedContractForReminder.contractId}...`, 'Envoi en cours');

    // Appel à l'endpoint de test avec les jours spécifiques
    this.contractService.sendTestReminder(this.selectedContractForReminder.contractId, this.selectedDaysBefore).subscribe({
      next: (response: any) => {
        this.isSendingReminder = false;
      
        console.log('Réponse du backend:', response);
      
        if (response.success) {
          const message = `✅ ${response.message}\n📧 Client: ${response.client}\n💰 Montant: ${response.paymentAmount} DT\n📅 Échéance: ${new Date(response.paymentDate).toLocaleDateString('fr-FR')}\n⏰ Jours avant: ${response.daysBefore}`;
        
          this.toastr.success(message, 'Rappel envoyé !', {
            timeOut: 8000,
            enableHtml: true
          });
        
          this.closeReminderModal();
        
          // Recharger les contrats
          setTimeout(() => {
            this.loadContracts();
          }, 2000);
        } else {
          this.toastr.error(response.error || 'Erreur lors de l\'envoi du rappel', 'Erreur');
        }
      },
      error: (err) => {
        this.isSendingReminder = false;
        console.error('Erreur lors de l\'envoi du rappel:', err);
      
        let errorMessage = 'Erreur lors de l\'envoi du rappel';
        if (err.error?.error) {
          errorMessage = err.error.error;
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (typeof err.error === 'string') {
          errorMessage = err.error;
        }
      
        this.toastr.error(errorMessage, 'Erreur');
      }
    });
  }
}