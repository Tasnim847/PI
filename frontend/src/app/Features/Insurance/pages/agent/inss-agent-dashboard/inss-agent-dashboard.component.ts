import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import Chart from 'chart.js/auto';
import { ToastrService } from 'ngx-toastr';
import { ContractService } from '../../../services/contract.service';

@Component({
  selector: 'app-inss-agent-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inss-agent-dashboard.component.html',
  styleUrls: ['./inss-agent-dashboard.component.css']
})
export class InssAgentDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  
  contractId: string | null = null;
  contract: any = null;
  client: any = null;
  isLoading = false;
  error = '';
  
  showFullNote: boolean = false;
  parsedEvaluationNote: any = null;
  
  stats = {
    totalPremium: 0
  };

  // Chart instance
  private componentPieChart: Chart | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.contractId = params['contractId'];
      if (this.contractId) {
        this.loadContractData();
      } else {
        this.error = 'Aucun contrat sélectionné';
        this.toastr.warning('Aucun contrat sélectionné');
        setTimeout(() => this.goBackToContracts(), 2000);
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initComponentPieChart(), 300);
  }

  ngOnDestroy(): void {
    if (this.componentPieChart) {
      this.componentPieChart.destroy();
    }
  }

  goBackToContracts(): void {
    this.router.navigate(['public/agent/contracts']);
  }

  async loadContractData(): Promise<void> {
    if (!this.contractId) return;

    this.isLoading = true;
    this.error = '';
    
    try {
      this.contract = await this.contractService.getContractById(Number(this.contractId)).toPromise();
      this.client = this.contract?.client || null;
      
      if (this.contract?.riskClaim?.evaluationNote) {
        this.parsedEvaluationNote = this.parseEvaluationNote(this.contract.riskClaim.evaluationNote);
        console.log('✅ Note parsée:', this.parsedEvaluationNote);
        
        // Initialize pie chart after data is loaded
        setTimeout(() => this.initComponentPieChart(), 200);
      }
      
      this.stats.totalPremium = this.contract?.premium || 0;
      this.toastr.success('Données chargées avec succès');
      
    } catch (error: any) {
      console.error('❌ Error:', error);
      this.error = 'Erreur lors du chargement des données';
      this.toastr.error('Erreur lors du chargement du contrat');
    } finally {
      this.isLoading = false;
    }
  }

  initComponentPieChart(): void {
    if (!this.parsedEvaluationNote?.components?.length) return;
    
    const canvas = document.getElementById('componentPieChart') as HTMLCanvasElement;
    if (!canvas) return;
    
    // Destroy existing chart
    if (this.componentPieChart) {
      this.componentPieChart.destroy();
    }
    
    const labels = this.parsedEvaluationNote.components.map((c: any) => c.name);
    const scores = this.parsedEvaluationNote.components.map((c: any) => c.score);
    const colors = labels.map((label: string) => this.getComponentColor(label));
    
    this.componentPieChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: scores,
          backgroundColor: colors,
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        cutout: '60%', // ✅ cutout doit être ici, pas dans datasets
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                const label = context.label || '';
                const value = context.raw as number;
                return `${label}: ${value}/100`;
              }
            },
            backgroundColor: '#1a1e24',
            titleColor: '#ffffff',
            bodyColor: '#9aa1ab',
            padding: 10,
            cornerRadius: 8
          }
        },
        layout: {
          padding: 10
        }
      }
    });
  }

  getComponentColor(componentName: string): string {
    const colors: { [key: string]: string } = {
      'Financier': '#1a5ac3',
      'Comportemental': '#36c165',
      'Historique': '#dfa94c',
      'Démographique': '#8b5cf6'
    };
    return colors[componentName] || '#6c757d';
  }

  parseEvaluationNote(note: string): any {
    if (!note) return null;
    
    const result: any = {
      components: [],
      riskFactors: [],
      bonusPoints: [],
      malusPoints: [],
      financialDetails: {}
    };
    
    // Score Client
    const clientScoreMatch = note.match(/Score Client:\s*([\d.]+)\/100/);
    if (clientScoreMatch) {
      result.clientScore = parseFloat(clientScoreMatch[1]);
    }
    
    // Niveau de risque et classe
    const riskLevelMatch = note.match(/Niveau de risque:\s*(\w+)/);
    if (riskLevelMatch) {
      result.clientRiskLevel = riskLevelMatch[1];
    }
    
    const riskClassMatch = note.match(/Classe de risque:\s*(\w+)/);
    if (riskClassMatch) {
      result.riskClass = riskClassMatch[1];
    }
    
    // Score total
    const totalScoreMatch = note.match(/Score total:\s*([\d.]+)\/100/);
    if (totalScoreMatch) {
      result.totalScore = parseFloat(totalScoreMatch[1]);
    }
    
    // Composantes
    const componentMatches = note.match(/- (\w+): ([\d.]+)/g);
    if (componentMatches) {
      componentMatches.forEach(match => {
        const nameMatch = match.match(/- (\w+):/);
        const scoreMatch = match.match(/: ([\d.]+)/);
        if (nameMatch && scoreMatch) {
          let name = nameMatch[1];
          let displayName = '';
          switch(name) {
            case 'financial': displayName = 'Financier'; break;
            case 'behavioral': displayName = 'Comportemental'; break;
            case 'historical': displayName = 'Historique'; break;
            case 'demographic': displayName = 'Démographique'; break;
            default: displayName = name;
          }
          result.components.push({
            name: displayName,
            originalName: name,
            score: parseFloat(scoreMatch[1])
          });
        }
      });
    }
    
    // Contribution client
    const contributionMatch = note.match(/Contribution client au risque:\s*([+-]\d+)\s*points/);
    if (contributionMatch) {
      result.clientContribution = contributionMatch[1];
    }
    
    // Type de produit
    const productMatch = note.match(/Type de produit:\s*(\w+)/);
    if (productMatch) {
      result.productType = productMatch[1];
    }
    
    const productPointsMatch = note.match(/Produit auto:\s*([+-]\d+)\s*points/);
    if (productPointsMatch) {
      result.productPoints = productPointsMatch[1];
    }
    
    // Détails financiers
    const deductibleMatch = note.match(/Franchise:\s*([\d.]+)\s*DT/);
    if (deductibleMatch) {
      result.financialDetails.deductible = parseFloat(deductibleMatch[1]);
      if (note.includes('Franchise très basse')) {
        result.financialDetails.deductibleNote = '⚠️ Très basse (+20 points)';
        result.malusPoints.push('Franchise très basse: +20 points');
      }
    }
    
    const coverageMatch = note.match(/Plafond de couverture:\s*([\d.]+)\s*DT/);
    if (coverageMatch) {
      result.financialDetails.coverageLimit = parseFloat(coverageMatch[1]);
      if (note.includes('Plafond standard')) {
        result.financialDetails.coverageLimitNote = '✅ Standard (+5 points)';
        result.bonusPoints.push('Plafond standard: +5 points');
      }
    }
    
    const durationMatch = note.match(/Durée du contrat:\s*(\d+)\s*ans/);
    if (durationMatch) {
      result.financialDetails.contractDuration = parseInt(durationMatch[1]);
      if (note.includes('Durée moyenne')) {
        result.financialDetails.durationNote = '✅ Moyenne (+10 points)';
        result.bonusPoints.push('Durée moyenne: +10 points');
      }
    }
    
    // Facteurs de risque
    const riskFactorsMatch = note.match(/Facteurs de risque identifiés:\s*([^💡]+)/);
    if (riskFactorsMatch) {
      const factorsText = riskFactorsMatch[1];
      const factors = factorsText.split('-').filter((f: string) => f.trim() && f.trim().length > 0);
      result.riskFactors = factors.map((f: string) => f.trim().replace(/\n/g, '').trim());
    }
    
    // Recommandation
    const recommendationMatch = note.match(/RECOMMANDATION:\s*([^\n]+)/);
    if (recommendationMatch) {
      result.recommendation = recommendationMatch[1].trim();
    }
    
    return result;
  }

  getGlobalRiskScore(): number {
    return this.contract?.riskClaim?.riskScore || 0;
  }

  getRiskLevelLabel(riskLevel: string): string {
    const labels: { [key: string]: string } = {
      'LOW': 'FAIBLE',
      'MEDIUM': 'MOYEN',
      'HIGH': 'ÉLEVÉ',
      'CRITICAL': 'CRITIQUE'
    };
    return labels[riskLevel] || riskLevel || 'NON ÉVALUÉ';
  }

  getScoreClass(score: number): string {
    if (score >= 70) return 'high';
    if (score >= 50) return 'medium';
    return 'low';
  }

  getScoreSummaryClass(score: number): string {
    if (score >= 70) return 'score-high';
    if (score >= 50) return 'score-medium';
    return 'score-low';
  }

  getRecommendationClass(riskLevel: string): string {
    switch(riskLevel) {
      case 'LOW': return 'rec-low';
      case 'MEDIUM': return 'rec-medium';
      case 'HIGH': return 'rec-high';
      default: return 'rec-low';
    }
  }

  getRecommendationText(riskLevel: string): string {
    switch(riskLevel) {
      case 'LOW': return '✅ ACCEPTER - Risque faible';
      case 'MEDIUM': return '⚠️ EXAMINER - Risque moyen';
      case 'HIGH': return '❌ REJETER - Risque élevé';
      default: return 'Aucune recommandation';
    }
  }

  getComponentIcon(componentName: string): string {
    const icons: { [key: string]: string } = {
      'Financier': 'bi bi-cash-stack',
      'Comportemental': 'bi bi-person-circle',
      'Historique': 'bi bi-clock-history',
      'Démographique': 'bi bi-people'
    };
    return icons[componentName] || 'bi bi-bar-chart';
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'ACTIVE': 'active',
      'INACTIVE': 'inactive',
      'CANCELLED': 'cancelled',
      'EXPIRED': 'expired',
      'COMPLETED': 'completed'
    };
    return statusMap[status] || '';
  }

  getStatusText(status: string): string {
    const statusMap: { [key: string]: string } = {
      'ACTIVE': 'ACTIF',
      'INACTIVE': 'EN ATTENTE',
      'CANCELLED': 'ANNULÉ',
      'EXPIRED': 'EXPIRÉ',
      'COMPLETED': 'TERMINÉ'
    };
    return statusMap[status] || status;
  }

  getClientFullName(): string {
    if (this.client) {
      return `${this.client.firstName || ''} ${this.client.lastName || ''}`.trim() || 'N/A';
    }
    if (this.contract?.client) {
      return `${this.contract.client.firstName || ''} ${this.contract.client.lastName || ''}`.trim() || 'N/A';
    }
    return 'N/A';
  }

  getClientEmail(): string {
    return this.client?.email || this.contract?.client?.email || 'N/A';
  }

  getClientPhone(): string {
    return this.client?.telephone || this.contract?.client?.telephone || 'N/A';
  }

  formatAmount(amount: number): string {
    if (amount === undefined || amount === null) return '0 DT';
    return amount.toLocaleString('fr-TN', { 
      minimumFractionDigits: 3, 
      maximumFractionDigits: 3 
    }) + ' DT';
  }

  formatDate(date: any): string {
    if (!date) return 'N/A';
    try {
      return new Date(date).toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return 'N/A';
    }
  }

  exportToCSV(): void {
    try {
      const csvData = [{
        'Contract ID': this.contract?.contractId || this.contractId || 'N/A',
        'Client': this.getClientFullName(),
        'Email': this.getClientEmail(),
        'Phone': this.getClientPhone(),
        'Status': this.getStatusText(this.contract?.status),
        'Premium (DT)': this.contract?.premium || 0,
        'Risk Level': this.getRiskLevelLabel(this.contract?.riskClaim?.riskLevel),
        'Risk Score': this.contract?.riskClaim?.riskScore || 'N/A',
        'Client Score': this.parsedEvaluationNote?.clientScore || 'N/A',
        'Total Score': this.parsedEvaluationNote?.totalScore || 'N/A',
        'Recommendation': this.parsedEvaluationNote?.recommendation || 'N/A',
        'Start Date': this.formatDate(this.contract?.startDate),
        'End Date': this.formatDate(this.contract?.endDate),
        'Product': this.contract?.product?.name || 'N/A'
      }];
      
      const headers = Object.keys(csvData[0]);
      const csvRows = [headers.join(',')];
      
      for (const row of csvData) {
        const values = headers.map(header => {
          const value = row[header as keyof typeof row];
          const escapedValue = String(value).replace(/"/g, '""');
          return `"${escapedValue}"`;
        });
        csvRows.push(values.join(','));
      }
      
      const blob = new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `contract_${this.contractId}_risk_report.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      
      this.toastr.success('Export CSV réussi');
    } catch (error) {
      console.error('Export error:', error);
      this.toastr.error('Erreur lors de l\'export CSV');
    }
  }
}