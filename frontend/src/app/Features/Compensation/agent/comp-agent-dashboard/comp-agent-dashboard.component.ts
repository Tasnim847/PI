// comp-agent-dashboard.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CompensationService } from '../../services/compensation.service';
import { ToastrService } from 'ngx-toastr';
import { Compensation } from '../../../../shared';

@Component({
  selector: 'app-comp-agent-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './comp-agent-dashboard.component.html',
  styleUrls: ['./comp-agent-dashboard.component.css']
})
export class CompAgentDashboardComponent implements OnInit {
  selectedCompensation: Compensation | null = null;
  compensationDetails: any = null;
  scoringDetails: any = null;
  loadingDetails = false;
  errorDetails: string | null = null;

  constructor(
    private compensationService: CompensationService,
    private toastr: ToastrService,
    private router: Router,
    private route: ActivatedRoute  // Ajouté pour récupérer l'ID
  ) {}

  ngOnInit(): void {
    // Récupérer l'ID depuis l'URL
    const compensationId = this.route.snapshot.params['id'];
    
    console.log('Compensation ID from URL:', compensationId);
    
    if (compensationId) {
      // Charger la compensation depuis l'API
      this.loadCompensationById(Number(compensationId));
    } else {
      // Vérifier le state (pour compatibilité)
      const navigation = this.router.getCurrentNavigation();
      const state = navigation?.extras.state as { selectedCompensation: Compensation };
      
      if (state?.selectedCompensation) {
        this.selectedCompensation = state.selectedCompensation;
        this.loadCompensationDetails(state.selectedCompensation.compensationId);
      } else {
        // Afficher un message d'erreur au lieu de rediriger
        console.log('No compensation ID found');
        this.errorDetails = 'No compensation selected. Please go back and select a compensation.';
        this.toastr.warning(this.errorDetails);
      }
    }
  }

  // Charger la compensation par ID depuis l'API
  loadCompensationById(compensationId: number): void {
    this.loadingDetails = true;
    this.errorDetails = null;
    
    // Récupérer toutes les compensations et trouver celle avec l'ID
    this.compensationService.getAgentCompensations().subscribe({
      next: (compensations) => {
        const compensation = compensations.find(c => c.compensationId === compensationId);
        if (compensation) {
          this.selectedCompensation = compensation;
          this.loadCompensationDetails(compensation.compensationId);
        } else {
          this.errorDetails = 'Compensation not found';
          this.loadingDetails = false;
          this.toastr.error(this.errorDetails);
        }
      },
      error: (err) => {
        console.error('Error loading compensations:', err);
        this.errorDetails = 'Unable to load compensation data';
        this.loadingDetails = false;
        this.toastr.error(this.errorDetails);
      }
    });
  }

  // Load complete compensation details with scoring
  loadCompensationDetails(compensationId: number | undefined): void {
    if (!compensationId) {
      this.errorDetails = 'Invalid compensation ID';
      return;
    }
    
    this.loadingDetails = true;
    this.errorDetails = null;
    
    this.compensationService.getCompensationWithScoring(compensationId).subscribe({
      next: (data) => {
        console.log('Détails chargés:', data);
        this.compensationDetails = data;
        this.scoringDetails = data;
        // Update selected compensation with any new data
        if (data.compensation) {
          this.selectedCompensation = { ...this.selectedCompensation, ...data.compensation };
        }
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('Error loading details:', err);
        const errorMsg = err.error?.message || 'Unable to load compensation details';
        this.errorDetails = errorMsg;
        this.loadingDetails = false;
        this.toastr.error(errorMsg);
      }
    });
  }

  // Mark as paid
  markAsPaid(): void {
    if (!this.selectedCompensation) return;
    
    if (confirm(`Confirm payment for compensation #${this.selectedCompensation.compensationId}?`)) {
      this.compensationService.markAsPaid(this.selectedCompensation.compensationId).subscribe({
        next: () => {
          this.toastr.success(`Compensation #${this.selectedCompensation!.compensationId} marked as paid`);
          // Recharger les détails
          this.loadCompensationDetails(this.selectedCompensation!.compensationId);
        },
        error: (err) => {
          const errorMsg = err.error?.message || 'Error processing payment';
          this.toastr.error(errorMsg);
        }
      });
    }
  }

  // Recalculate compensation
  recalculateCompensation(): void {
    if (!this.selectedCompensation?.claim?.claimId) {
      this.toastr.warning('Cannot recalculate: Claim ID not found');
      return;
    }
    
    if (confirm(`Recalculate compensation #${this.selectedCompensation.compensationId}?`)) {
      this.compensationService.recalculateCompensation(this.selectedCompensation.claim.claimId).subscribe({
        next: (response) => {
          this.toastr.success(`Compensation #${this.selectedCompensation?.compensationId} recalculated`);
          // Recharger les détails
          this.loadCompensationDetails(this.selectedCompensation!.compensationId);
        },
        error: (err) => {
          const errorMsg = err.error?.message || 'Error during recalculation';
          this.toastr.error(errorMsg);
        }
      });
    }
  }

  // Navigation methods
  goBackToList(): void {
    this.router.navigate(['/public/agent/compensations']);
  }

  // Helper methods
  getClientName(compensation: Compensation): string {
    if (compensation.client) {
      return `${compensation.client.firstName || ''} ${compensation.client.lastName || ''}`.trim() || 'Unknown client';
    }
    if (compensation.claim?.client) {
      return `${compensation.claim.client.firstName || ''} ${compensation.claim.client.lastName || ''}`.trim() || 'Unknown client';
    }
    return 'Unknown client';
  }

  getClientId(compensation: Compensation): number | null {
    if (compensation.client) {
      return compensation.client.id;
    }
    return compensation.claim?.client?.id || null;
  }

  getStatusLabel(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDING': 'Pending',
      'CALCULATED': 'Calculated',
      'PAID': 'Paid',
      'CANCELLED': 'Cancelled'
    };
    return statusMap[status] || status;
  }

  getStatusClass(status: string): string {
    const classMap: { [key: string]: string } = {
      'PENDING': 'status-pending',
      'CALCULATED': 'status-calculated',
      'PAID': 'status-paid',
      'CANCELLED': 'status-cancelled'
    };
    return classMap[status] || 'status-default';
  }

  getRiskLabel(riskLevel: string): string {
    const labelMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'Very Low',
      'FAIBLE': 'Low',
      'MODERE': 'Moderate',
      'ELEVE': 'High',
      'TRES_ELEVE': 'Very High'
    };
    return labelMap[riskLevel] || riskLevel;
  }

  getRiskClass(riskLevel: string): string {
    const classMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'risk-low',
      'FAIBLE': 'risk-low',
      'MODERE': 'risk-medium',
      'ELEVE': 'risk-high',
      'TRES_ELEVE': 'risk-very-high'
    };
    return classMap[riskLevel] || 'risk-medium';
  }

  getRiskLevelClass(riskLevel: string): string {
    const classMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'score-low',
      'FAIBLE': 'score-low',
      'MODERE': 'score-medium',
      'ELEVE': 'score-high',
      'TRES_ELEVE': 'score-very-high'
    };
    return classMap[riskLevel] || 'score-medium';
  }

  getDecisionLabel(decision: string): string {
    const decisionMap: { [key: string]: string } = {
      'AUTO_APPROVE': 'Auto Approve',
      'AUTO_REJECT': 'Auto Reject',
      'MANUAL_REVIEW': 'Manual Review'
    };
    return decisionMap[decision] || decision;
  }

  getDecisionClass(decision: string): string {
    const classMap: { [key: string]: string } = {
      'AUTO_APPROVE': 'decision-approve',
      'AUTO_REJECT': 'decision-reject',
      'MANUAL_REVIEW': 'decision-review'
    };
    return classMap[decision] || 'decision-default';
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    const d = new Date(date);
    if (isNaN(d.getTime())) return 'N/A';
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatAmount(amount: number | undefined | null): string {
    if (amount === undefined || amount === null) return '0.00 DT';
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount) + ' DT';
  }

  formatMessage(message: string): string {
    if (!message) return '';
    return message.replace(/\n/g, '<br>');
  }

  // Ajoutez ces méthodes à la fin de la classe

// Financial calculations
getCoveragePercentage(): number {
  const total = (this.selectedCompensation?.amount || 0) + (this.selectedCompensation?.clientOutOfPocket || 0);
  if (total === 0) return 0;
  return Math.round((this.selectedCompensation?.amount || 0) / total * 100);
}

getOutOfPocketPercentage(): number {
  const total = (this.selectedCompensation?.amount || 0) + (this.selectedCompensation?.clientOutOfPocket || 0);
  if (total === 0) return 0;
  return Math.round((this.selectedCompensation?.clientOutOfPocket || 0) / total * 100);
}

getDeductiblePercentage(): number {
  const maxAmount = Math.max(this.selectedCompensation?.originalClaimedAmount || 0, 1);
  return Math.round((this.selectedCompensation?.deductible || 0) / maxAmount * 100);
}

// Risk Gauge methods
getRiskGaugeColor(): string {
  const score = this.scoringDetails?.claimScore?.riskScore || 0;
  if (score < 30) return '#28a745';
  if (score < 60) return '#ffc107';
  if (score < 80) return '#fd7e14';
  return '#dc3545';
}

getRiskGaugeDashArray(): string {
  const score = this.scoringDetails?.claimScore?.riskScore || 0;
  const circumference = 251.2; // 80 * 3.14
  const dashArray = (score / 100) * circumference;
  return `${dashArray} ${circumference}`;
}

getRiskGaugePointer(): number {
  const score = this.scoringDetails?.claimScore?.riskScore || 0;
  // 20 + (score / 100) * 160
  return 20 + (score / 100) * 160;
}

// Risk factors list
getRiskFactorsList(): any[] {
  const factors = this.scoringDetails?.claimScore?.factors || [];
  const factorScores = this.scoringDetails?.claimScore?.factorScores || {};
  
  const factorMap: { [key: string]: { name: string; icon: string } } = {
    'montant': { name: 'Amount', icon: 'fas fa-coins' },
    'delai': { name: 'Timeliness', icon: 'fas fa-clock' },
    'documents': { name: 'Documents', icon: 'fas fa-file-alt' },
    'frequence': { name: 'Frequency', icon: 'fas fa-chart-line' },
    'historique': { name: 'History', icon: 'fas fa-history' }
  };
  
  return factors.map((factor: string) => ({
    name: factorMap[factor.toLowerCase()]?.name || factor,
    score: factorScores[factor] || 50,
    icon: factorMap[factor.toLowerCase()]?.icon || 'fas fa-chart-simple'
  }));
}

getFactorClass(score: number): string {
  if (score >= 70) return 'high-risk';
  if (score >= 40) return 'medium-risk';
  return 'low-risk';
}

// Extract amount from message
extractAmount(message: string, label: string): string {
  if (!message) return 'N/A';
  const regex = new RegExp(`${label}[\\s:]*([\\d\\s,]+(?:,\\d{2})?)\\s*DT`, 'i');
  const match = message.match(regex);
  if (match) {
    return match[1].trim() + ' DT';
  }
  return 'Not specified';
}

// Get scoring metrics
getScoringMetrics(): any[] {
  const claimScore = this.scoringDetails?.claimScore;
  if (!claimScore) return [];
  
  return [
    { label: 'Risk Score', value: `${claimScore.riskScore || 0}/100`, icon: 'fas fa-chart-line', class: 'primary' },
    { label: 'Fraud Probability', value: `${claimScore.fraudProbability || 0}%`, icon: 'fas fa-exclamation-triangle', class: 'warning' },
    { label: 'Validation Status', value: claimScore.validationStatus || 'N/A', icon: 'fas fa-check-circle', class: 'success' },
    { label: 'Claims History', value: `${claimScore.claimCount || 0} claims`, icon: 'fas fa-history', class: 'info' }
  ];
}

// Extract warning messages from the explanation
getWarningMessages(message: string): any[] {
  if (!message) return [];
  
  const warnings = [];
  const lines = message.split('\n');
  
  for (const line of lines) {
    if (line.includes('⚠️')) {
      let icon = 'exclamation-triangle';
      let text = line.replace('⚠️', '').trim();
      
      if (text.includes('anormalement élevé')) {
        icon = 'chart-line';
      } else if (text.includes('très tôt après souscription')) {
        icon = 'clock';
      } else if (text.includes('REVUE MANUELLE')) {
        icon = 'user-check';
      }
      
      warnings.push({ icon, text });
    }
  }
  
  return warnings;
}
}