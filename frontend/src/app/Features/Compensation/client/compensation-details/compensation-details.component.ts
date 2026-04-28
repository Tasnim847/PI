import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CompensationService } from '../../services/compensation.service';

@Component({
  selector: 'app-compensation-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './compensation-details.component.html',
  styleUrls: ['./compensation-details.component.css']
})
export class CompensationDetailsComponent implements OnInit {
  compensationId: number | null = null;
  compensationData: any = null;
  loading = false;
  error = '';

  // Propriétés pour les métriques extraites du message
  coverageRate: number = 0;
  globalScore: number = 0;
  alertLevel: string = 'low';

  parsedRecommendation: any = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private compensationService: CompensationService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.compensationId = +params['id'];
      if (this.compensationId) {
        this.loadCompensationDetails();
      }
    });
  }

  loadCompensationDetails(): void {
    this.loading = true;
    this.error = '';

    this.compensationService.getMyCompensationDetails(this.compensationId!).subscribe({
      next: (data) => {
        this.compensationData = data;
        this.extractMetricsFromMessage(data.compensation?.message);
        this.parseMessageDetails(data.compensation?.message);
        this.parseRecommendation(data.claimScore?.recommendation);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des détails: ' + err.message;
        this.loading = false;
      }
    });
  }

  // ========== MÉTHODES AJOUTÉES ==========

  downloadReport(): void {
    // Génération d'un rapport PDF simple
    const reportContent = `
      RAPPORT DE COMPENSATION
      =======================
      Compensation #: ${this.compensationId}
      Date: ${new Date().toLocaleDateString()}
      
      INFORMATIONS FINANCIÈRES
      ------------------------
      Montant initial réclamé: ${this.formatAmount(this.compensationData?.compensation?.originalClaimedAmount)}
      Montant approuvé: ${this.formatAmount(this.compensationData?.compensation?.approvedAmount)}
      Franchise: ${this.formatAmount(this.compensationData?.compensation?.deductible)}
      Plafond de couverture: ${this.formatAmount(this.compensationData?.compensation?.coverageLimit)}
      Remboursement assurance: ${this.formatAmount(this.compensationData?.compensation?.amount)}
      Reste à charge: ${this.formatAmount(this.compensationData?.compensation?.clientOutOfPocket)}
      
      SCORES DE RISQUE
      ----------------
      Score Global: ${this.getGlobalScore()}/100
      Niveau d'alerte: ${this.getAlertLevelText()}
      Taux de prise en charge: ${this.getCoverageRate()}%
    `;
    
    const blob = new Blob([reportContent], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `compensation_${this.compensationId}_report.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getRiskLevelClass(level?: string): string {
    // Pour la carte de score global
    if (this.globalScore >= 70) return 'risk-high';
    if (this.globalScore >= 40) return 'risk-medium';
    return 'risk-low';
  }

  getIndicatorClass(value: number): string {
    if (value >= 70) return 'indicator-high';
    if (value >= 40) return 'indicator-medium';
    return 'indicator-low';
  }

  getRiskClass(riskLevel: string): string {
    const risk = riskLevel?.toLowerCase() || '';
    if (risk.includes('élevé') || risk.includes('high')) return 'risk-text-high';
    if (risk.includes('moyen') || risk.includes('medium')) return 'risk-text-medium';
    return 'risk-text-low';
  }

  parseRecommendation(message: string) {
    if (!message) return;

    this.parsedRecommendation = {
      scoreGlobal: this.extractValue(message, /Score global[:\s]*(\d+[.,]?\d*)/i),
      montantScore: this.extractValue(message, /Montant[:\s]*(\d+)\s*\/\s*100/i),
      delaiScore: this.extractValue(message, /Délai[:\s]*(\d+)\s*\/\s*100/i),
      documentsScore: this.extractValue(message, /Documents[:\s]*(\d+)\s*\/\s*100/i),
      frequenceScore: this.extractValue(message, /Fréquence[:\s]*(\d+)\s*\/\s*100/i),
      historiqueScore: this.extractValue(message, /Historique[:\s]*(\d+)\s*\/\s*100/i),
      decision: this.extractText(message, /Décision suggérée[:\s]*([^-\n]+)/i),
      alerts: this.extractAlerts(message)
    };
  }

  extractAlerts(message: string): string[] {
    const alerts: string[] = [];
    const patterns = [
      /Montant anormalement élevé/i,
      /Vérifier la justification/i,
      /Claim déclaré très tôt/i,
      /Vérifier les circonstances/i
    ];
    patterns.forEach(p => {
      const match = message.match(p);
      if (match) alerts.push(match[0]);
    });
    return alerts;
  }

  goBack(): void {
    this.router.navigate(['/public/compensations']);
  }

  formatAmount(amount: number): string {
    if (amount === undefined || amount === null) return '0,000 DT';
    return amount.toLocaleString('fr-TN', { 
      minimumFractionDigits: 3, 
      maximumFractionDigits: 3 
    }) + ' DT';
  }

  formatDate(date: Date | string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  extractMetricsFromMessage(message: string): boolean {
    if (!message) return false;
    
    const coverageMatch = message.match(/(\d+(?:[.,]\d+)?)\s*%?\s*(?:de prise en charge|pris en charge)/i);
    if (coverageMatch) {
      this.coverageRate = parseFloat(coverageMatch[1].replace(',', '.'));
    } else {
      const approved = this.compensationData?.compensation?.approvedAmount || 0;
      const original = this.compensationData?.compensation?.originalClaimedAmount || 1;
      this.coverageRate = Math.round((approved / original) * 100);
    }
    
    const scoreMatch = message.match(/(\d+(?:[.,]\d+)?)\s*\/\s*100/);
    if (scoreMatch) {
      this.globalScore = parseFloat(scoreMatch[1].replace(',', '.'));
    } else {
      this.globalScore = this.compensationData?.claimScore?.riskScore || 50;
    }
    
    if (this.globalScore >= 70) {
      this.alertLevel = 'high';
    } else if (this.globalScore >= 40) {
      this.alertLevel = 'medium';
    } else {
      this.alertLevel = 'low';
    }
    
    return true;
  }

  getCoverageRate(): number {
    return this.coverageRate;
  }

  getGlobalScore(): number {
    return this.globalScore;
  }

  getAlertLevel(): string {
    return this.alertLevel;
  }

  getAlertLevelText(): string {
    const alertMap: { [key: string]: string } = {
      'high': 'ÉLEVÉ',
      'medium': 'MOYEN',
      'low': 'FAIBLE'
    };
    return alertMap[this.alertLevel] || 'NORMAL';
  }

  extractKeyFactors(message: string): string[] {
    if (!message) return [];
    
    const factors: string[] = [];
    const factorPatterns = [
      { pattern: /Montant[:\s]*(\d+(?:[.,]\d+)?\s*%?\s*\/\s*\d+)/i, text: 'Analyse du montant' },
      { pattern: /Délai[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Délai de déclaration' },
      { pattern: /Fréquence[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Fréquence des sinistres' },
      { pattern: /Historique[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Historique client' },
      { pattern: /documents?/i, text: 'Documentation fournie' },
      { pattern: /justification/i, text: 'Justification requise' },
      { pattern: /vérification/i, text: 'Vérification nécessaire' }
    ];
    
    factorPatterns.forEach(factor => {
      if (factor.pattern.test(message)) {
        factors.push(factor.text);
      }
    });
    
    if (factors.length === 0 && message.length > 0) {
      factors.push('Analyse complémentaire requise');
    }
    
    return factors;
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'CALCULATED': 'calculated',
      'PAID': 'paid',
      'PENDING': 'pending',
      'CANCELLED': 'cancelled'
    };
    return statusMap[status] || '';
  }

  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      'CALCULATED': 'CALCULÉ',
      'PAID': 'PAYÉ',
      'PENDING': 'EN ATTENTE',
      'CANCELLED': 'ANNULÉ'
    };
    return statusMap[status] || status;
  }

  getRiskScoreClass(score: number): string {
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  }

  getRiskLevelText(level: string): string {
    const levelMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'TRÈS FAIBLE',
      'FAIBLE': 'FAIBLE',
      'MOYEN': 'MOYEN',
      'ELEVE': 'ÉLEVÉ',
      'TRES_ELEVE': 'TRÈS ÉLEVÉ'
    };
    return levelMap[level] || level;
  }

  getDecisionClass(decision: string): string {
    const decisionMap: { [key: string]: string } = {
      'AUTO_APPROVE': 'approve',
      'AUTO_REJECT': 'reject',
      'MANUAL_REVIEW': 'review'
    };
    return decisionMap[decision] || '';
  }

  getDecisionText(decision: string): string {
    const decisionMap: { [key: string]: string } = {
      'AUTO_APPROVE': 'APPROBATION AUTO',
      'AUTO_REJECT': 'REJET AUTO',
      'MANUAL_REVIEW': 'REVUE MANUELLE'
    };
    return decisionMap[decision] || decision;
  }

  getProgressBarClass(score: number): string {
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  }

  getScoreClass(score: number): string {
    if (score === undefined || score === null) return '';
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  }

  getScoreStatus(score: number): string {
    if (score === undefined || score === null) return 'N/A';
    if (score >= 70) return 'Risque Élevé';
    if (score >= 40) return 'Risque Moyen';
    return 'Risque Faible';
  }

  parsedDetails: any = {};

  parseMessageDetails(message: string) {
    if (!message) return;

    this.parsedDetails = {
      montantInitial: this.extractValue(message, /(\d+[.,]?\d*)\s*DT\s*.*Franchise/i),
      franchise: this.extractValue(message, /Franchise.*?(\d+[.,]?\d*)\s*DT/i),
      plafond: this.extractValue(message, /Plafond.*?(\d+[.,]?\d*)\s*DT/i),
      montantPris: this.extractValue(message, /pris en charge.*?(\d+[.,]?\d*)\s*DT/i),
      reste: this.extractValue(message, /Reste.*?(\d+[.,]?\d*)\s*DT/i),
      score: this.extractValue(message, /Score.*?(\d+[.,]?\d*)\s*\/\s*100/i),
      decision: this.extractText(message, /Décision suggérée[:\s]*(.*?)(?:\n|$)/i),
      risque: this.extractText(message, /Niveau de risque[:\s]*(.*?)(?:\n|$)/i)
    };
  }

  extractValue(text: string, regex: RegExp): number {
    const match = text.match(regex);
    return match ? parseFloat(match[1].replace(',', '.')) : 0;
  }

  extractText(text: string, regex: RegExp): string {
    const match = text.match(regex);
    return match ? match[1].trim() : '';
  }
}