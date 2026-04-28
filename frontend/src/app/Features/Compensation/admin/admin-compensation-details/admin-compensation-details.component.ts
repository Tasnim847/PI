// admin-compensation-details.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CompensationService } from '../../services/compensation.service';

@Component({
  selector: 'app-admin-compensation-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-compensation-details.component.html',
  styleUrls: ['./admin-compensation-details.component.css']
})
export class AdminCompensationDetailsComponent implements OnInit {
  compensationId: number | null = null;
  compensationData: any = null;
  loading = false;
  error = '';

  // Properties for metrics extracted from message
  coverageRate: number = 0;
  globalScore: number = 0;
  alertLevel: string = 'low';

  parsedRecommendation: any = {};
  parsedDetails: any = {};

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

  // Load compensation details using the with-scoring endpoint
  loadCompensationDetails(): void {
    this.loading = true;
    this.error = '';

    // Using existing with-scoring endpoint which returns all needed data
    this.compensationService.getCompensationWithScoring(this.compensationId!).subscribe({
      next: (data) => {
        this.compensationData = data;
        this.extractMetricsFromMessage(data.compensation?.message);
        this.parseMessageDetails(data.compensation?.message);
        this.parseRecommendation(data.claimScore?.recommendation);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error loading compensation details: ' + err.message;
        this.loading = false;
      }
    });
  }

  // ========== METHODS ==========

  downloadReport(): void {
    const reportContent = `
      ADMIN COMPENSATION REPORT
      =========================
      Compensation #: ${this.compensationId}
      Date: ${new Date().toLocaleDateString()}
      
      FINANCIAL INFORMATION
      ---------------------
      Initial Claimed Amount: ${this.formatAmount(this.compensationData?.compensation?.originalClaimedAmount)}
      Approved Amount: ${this.formatAmount(this.compensationData?.compensation?.approvedAmount)}
      Deductible: ${this.formatAmount(this.compensationData?.compensation?.deductible)}
      Coverage Limit: ${this.formatAmount(this.compensationData?.compensation?.coverageLimit)}
      Insurance Reimbursement: ${this.formatAmount(this.compensationData?.compensation?.amount)}
      Out of Pocket: ${this.formatAmount(this.compensationData?.compensation?.clientOutOfPocket)}
      
      CLAIM INFORMATION
      -----------------
      Claim ID: ${this.compensationData?.compensation?.claim?.claimId || 'N/A'}
      Claim Status: ${this.compensationData?.compensation?.claim?.status || 'N/A'}
      Incident Date: ${this.formatDate(this.compensationData?.compensation?.claim?.incidentDate) || 'N/A'}
      
      RISK SCORES
      -----------
      Global Score: ${this.getGlobalScore()}/100
      Alert Level: ${this.getAlertLevelText()}
      Coverage Rate: ${this.getCoverageRate()}%
      Risk Level: ${this.getRiskLevelText(this.compensationData?.claimScore?.riskLevel)}
      Suggested Decision: ${this.getDecisionText(this.compensationData?.claimScore?.decisionSuggestion)}
      Suspicious Claim: ${this.compensationData?.claimScore?.isSuspicious ? 'YES ⚠️' : 'NO ✅'}
    `;
    
    const blob = new Blob([reportContent], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `admin_compensation_${this.compensationId}_report.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  // Navigate back to compensation list
  goBack(): void {
    this.router.navigate(['/backoffice/compensation']);
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
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
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
      'high': 'HIGH',
      'medium': 'MEDIUM',
      'low': 'LOW'
    };
    return alertMap[this.alertLevel] || 'NORMAL';
  }

  extractKeyFactors(message: string): string[] {
    if (!message) return [];
    
    const factors: string[] = [];
    const factorPatterns = [
      { pattern: /Montant[:\s]*(\d+(?:[.,]\d+)?\s*%?\s*\/\s*\d+)/i, text: 'Amount Analysis' },
      { pattern: /Délai[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Declaration Delay' },
      { pattern: /Fréquence[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Claim Frequency' },
      { pattern: /Historique[:\s]*(\d+(?:[.,]\d+)?)/i, text: 'Client History' },
      { pattern: /documents?/i, text: 'Documentation Provided' },
      { pattern: /justification/i, text: 'Justification Required' },
      { pattern: /vérification/i, text: 'Verification Needed' }
    ];
    
    factorPatterns.forEach(factor => {
      if (factor.pattern.test(message)) {
        factors.push(factor.text);
      }
    });
    
    if (factors.length === 0 && message.length > 0) {
      factors.push('Additional Analysis Required');
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
      'CALCULATED': 'CALCULATED',
      'PAID': 'PAID',
      'PENDING': 'PENDING',
      'CANCELLED': 'CANCELLED'
    };
    return statusMap[status] || status;
  }

  getRiskLevelText(level: string): string {
    const levelMap: { [key: string]: string } = {
      'TRES_FAIBLE': 'VERY LOW',
      'FAIBLE': 'LOW',
      'MODERE': 'MODERATE',
      'ELEVE': 'HIGH',
      'TRES_ELEVE': 'VERY HIGH'
    };
    return levelMap[level] || level;
  }

  getRiskLevelClass(level?: string): string {
    if (this.globalScore >= 70) return 'risk-high';
    if (this.globalScore >= 40) return 'risk-medium';
    return 'risk-low';
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
      'AUTO_APPROVE': 'AUTO APPROVE',
      'AUTO_REJECT': 'AUTO REJECT',
      'MANUAL_REVIEW': 'MANUAL REVIEW'
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

  getIndicatorClass(value: number): string {
    if (value >= 70) return 'indicator-high';
    if (value >= 40) return 'indicator-medium';
    return 'indicator-low';
  }

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

  extractValue(text: string, regex: RegExp): number {
    const match = text.match(regex);
    return match ? parseFloat(match[1].replace(',', '.')) : 0;
  }

  extractText(text: string, regex: RegExp): string {
    const match = text.match(regex);
    return match ? match[1].trim() : '';
  }
}