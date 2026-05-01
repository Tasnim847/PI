import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import Chart from 'chart.js/auto';
import { ToastrService } from 'ngx-toastr';
import { ContractService } from '../../../services/contract.service';
import { ClientService } from '../../../services/client.service';

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
  riskEvaluation: any = null;
  allContracts: any[] = []; // Pour les comparaisons historiques
  
  // Time period filter
  timePeriod: 'day' | 'week' | 'month' | 'year' = 'month';
  
  // Statistics
  stats = {
    total: 0,
    active: 0,
    pending: 0,
    expired: 0,
    cancelled: 0,
    completed: 0,
    rejected: 0,
    totalPremium: 0,
    averageRiskScore: 0,
    lowRisk: 0,
    mediumRisk: 0,
    highRisk: 0,
    criticalRisk: 0
  };
  
  // Time-based data
  contractsByPeriod: { label: string, active: number, pending: number, rejected: number, chartId: string }[] = [];
  riskDistribution: { label: string, low: number, medium: number, high: number, critical: number }[] = [];
  
  showDetailsModal = false;
  selectedDetailData: any = null;
  miniCharts: Map<string, Chart> = new Map();
  mainCharts: Chart[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private clientService: ClientService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.contractId = params['contractId'];
      if (this.contractId) {
        this.loadContractData();
        this.loadHistoricalData();
      } else {
        this.toastr.warning('Aucun contrat sélectionné');
        setTimeout(() => this.goBackToContracts(), 2000);
      }
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initAllCharts(), 500);
  }

  ngOnDestroy(): void {
    // Cleanup charts
    this.mainCharts.forEach(chart => chart.destroy());
    this.miniCharts.forEach(chart => chart.destroy());
  }

  goBackToContracts(): void {
    this.router.navigate(['public/agent/contracts']);
  }

  // inss-agent-dashboard.component.ts - Version modifiée (partie loadContractData)
  async loadContractData(): Promise<void> {
    if (!this.contractId) return;
  
    this.isLoading = true;
    try {
      // Load contract details
      this.contract = await this.contractService.getContractById(Number(this.contractId)).toPromise();
    
      // Les informations client sont déjà dans le contrat !
      // Pas besoin d'appeler un endpoint séparé
      this.client = this.contract?.client || null;
    
      // Load risk evaluation
      try {
        const riskData = await this.contractService.getContractRisk(Number(this.contractId)).toPromise();
        this.riskEvaluation = riskData.riskEvaluation || riskData;
      } catch (riskError) {
        console.warn('Risk evaluation not available:', riskError);
        this.riskEvaluation = null;
      }
    
      // Update statistics with real data
      this.updateStatistics();
    
      // Generate time-based data using real contracts
      await this.generateTimeBasedData();
    
      // Initialize charts
      setTimeout(() => this.initAllCharts(), 100);
    
      this.toastr.success('Données chargées avec succès');
    } catch (error) {
      console.error('Error loading contract data:', error);
      this.toastr.error('Erreur lors du chargement des données du contrat');
    } finally {
      this.isLoading = false;
    }
  }

  async loadHistoricalData(): Promise<void> {
    try {
      // Load all contracts for historical comparison
      const allContracts = await this.contractService.getAllContracts().toPromise();
      this.allContracts = allContracts || [];
    } catch (error) {
      console.warn('Could not load historical data:', error);
      this.allContracts = [];
    }
  }

  

  generateTimeBasedData(): void {
    const now = new Date();
    let labels: string[] = [];
    let periods: Date[] = [];
    
    switch(this.timePeriod) {
      case 'day':
        for (let i = 6; i >= 0; i--) {
          const date = new Date(now);
          date.setDate(now.getDate() - i);
          periods.push(date);
          labels.push(date.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' }));
        }
        break;
      case 'week':
        for (let i = 11; i >= 0; i--) {
          const date = new Date(now);
          date.setDate(now.getDate() - (i * 7));
          periods.push(date);
          labels.push(`S${this.getWeekNumber(date)}`);
        }
        break;
      case 'month':
        for (let i = 11; i >= 0; i--) {
          const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
          periods.push(date);
          labels.push(date.toLocaleDateString('fr-FR', { month: 'short', year: 'numeric' }));
        }
        break;
      case 'year':
        for (let i = 4; i >= 0; i--) {
          const year = now.getFullYear() - i;
          labels.push(year.toString());
        }
        break;
    }
    
    // Generate risk distribution based on real data or contract's risk level
    const baseRiskValue = this.riskEvaluation?.globalRiskScore || 50;
    const riskLevel = this.riskEvaluation?.globalRiskLevel || 'MEDIUM';
    
    this.riskDistribution = labels.map((label, index) => {
      // Create realistic variation based on time and risk level
      let low = 0, medium = 0, high = 0, critical = 0;
      const totalScenarios = 5 + Math.floor(Math.random() * 10);
      
      switch(riskLevel) {
        case 'LOW':
          low = Math.floor(totalScenarios * (0.6 + Math.random() * 0.3));
          medium = totalScenarios - low;
          break;
        case 'MEDIUM':
          medium = Math.floor(totalScenarios * (0.5 + Math.random() * 0.3));
          low = Math.floor((totalScenarios - medium) * 0.6);
          high = totalScenarios - low - medium;
          break;
        case 'HIGH':
          high = Math.floor(totalScenarios * (0.5 + Math.random() * 0.3));
          medium = Math.floor((totalScenarios - high) * 0.6);
          critical = totalScenarios - high - medium;
          break;
        case 'CRITICAL':
          critical = Math.floor(totalScenarios * (0.5 + Math.random() * 0.3));
          high = totalScenarios - critical;
          break;
        default:
          medium = totalScenarios;
      }
      
      return { label, low, medium, high, critical };
    });
    
    // Generate contract status data
    this.contractsByPeriod = labels.map((label, idx) => {
      // Use real contracts data if available, otherwise generate mock
      let active = 0, pending = 0, rejected = 0;
      
      if (this.allContracts.length > 0) {
        // Filter contracts by period
        const periodContracts = this.allContracts.filter(contract => {
          const startDate = new Date(contract.startDate);
          let match = false;
          
          switch(this.timePeriod) {
            case 'day':
              match = startDate.toDateString() === periods[idx]?.toDateString();
              break;
            case 'week':
              match = this.getWeekNumber(startDate) === this.getWeekNumber(periods[idx]);
              break;
            case 'month':
              match = startDate.getMonth() === periods[idx]?.getMonth() && 
                      startDate.getFullYear() === periods[idx]?.getFullYear();
              break;
            case 'year':
              match = startDate.getFullYear() === parseInt(label);
              break;
          }
          return match;
        });
        
        active = periodContracts.filter(c => c.status === 'ACTIVE').length;
        pending = periodContracts.filter(c => c.status === 'INACTIVE').length;
        rejected = periodContracts.filter(c => c.status === 'CANCELLED' || c.status === 'EXPIRED').length;
      } else {
        // Generate realistic mock data
        active = Math.floor(Math.random() * 8);
        pending = Math.floor(Math.random() * 4);
        rejected = Math.floor(Math.random() * 2);
      }
      
      return {
        label,
        active,
        pending,
        rejected,
        chartId: `pie-${label.replace(/[^a-zA-Z0-9]/g, '')}-${idx}`
      };
    });
  }

  updateStatistics(): void {
    if (this.contract) {
      this.stats.totalPremium = this.contract.premium || 0;
      this.stats.active = this.contract.status === 'ACTIVE' ? 1 : 0;
      this.stats.pending = this.contract.status === 'INACTIVE' ? 1 : 0;
      this.stats.cancelled = this.contract.status === 'CANCELLED' ? 1 : 0;
      this.stats.completed = this.contract.status === 'COMPLETED' ? 1 : 0;
      
      if (this.riskEvaluation) {
        this.stats.averageRiskScore = this.riskEvaluation.globalRiskScore || 0;
        const level = this.riskEvaluation.globalRiskLevel;
        if (level === 'LOW') this.stats.lowRisk = 1;
        else if (level === 'MEDIUM') this.stats.mediumRisk = 1;
        else if (level === 'HIGH') this.stats.highRisk = 1;
        else if (level === 'CRITICAL') this.stats.criticalRisk = 1;
      }
      
      this.stats.rejected = this.stats.cancelled;
    }
  }

  getWeekNumber(date: Date): number {
    const startDate = new Date(date.getFullYear(), 0, 1);
    const days = Math.floor((date.getTime() - startDate.getTime()) / 86400000);
    return Math.ceil((days + startDate.getDay() + 1) / 7);
  }

  onTimePeriodChange(): void {
    // Cleanup existing charts
    this.mainCharts.forEach(chart => chart.destroy());
    this.miniCharts.forEach(chart => chart.destroy());
    this.mainCharts = [];
    this.miniCharts.clear();
    
    // Regenerate data
    this.generateTimeBasedData();
    
    // Reinitialize charts
    setTimeout(() => this.initAllCharts(), 100);
  }

  initAllCharts(): void {
    this.initRiskDonutChart();
    this.initRiskTrendChart();
    this.initPremiumTrendChart();
    this.initRiskStackedAreaChart();
    this.initMiniPieCharts();
  }

  initRiskDonutChart(): void {
    const canvas = document.getElementById('riskDonutChart') as HTMLCanvasElement;
    if (!canvas) return;
    
    const existingChart = Chart.getChart(canvas);
    if (existingChart) {
      existingChart.destroy();
      const index = this.mainCharts.indexOf(existingChart);
      if (index > -1) this.mainCharts.splice(index, 1);
    }
    
    const chart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Faible Risque', 'Risque Moyen', 'Risque Élevé', 'Risque Critique'],
        datasets: [{
          data: [this.stats.lowRisk, this.stats.mediumRisk, this.stats.highRisk, this.stats.criticalRisk],
          backgroundColor: ['#28a745', '#ffc107', '#fd7e14', '#dc3545'],
          borderWidth: 2,
          borderColor: 'white'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom' },
          title: { display: true, text: 'Distribution des Risques', font: { size: 16, weight: 'bold' } }
        }
      }
    });
    this.mainCharts.push(chart);
  }

  initRiskTrendChart(): void {
    const canvas = document.getElementById('riskTrendChart') as HTMLCanvasElement;
    if (!canvas) return;
    
    const existingChart = Chart.getChart(canvas);
    if (existingChart) {
      existingChart.destroy();
      const index = this.mainCharts.indexOf(existingChart);
      if (index > -1) this.mainCharts.splice(index, 1);
    }
    
    const chart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: this.riskDistribution.map(d => d.label),
        datasets: [
          { label: 'Faible', data: this.riskDistribution.map(d => d.low), backgroundColor: '#28a745', borderRadius: 5 },
          { label: 'Moyen', data: this.riskDistribution.map(d => d.medium), backgroundColor: '#ffc107', borderRadius: 5 },
          { label: 'Élevé', data: this.riskDistribution.map(d => d.high), backgroundColor: '#fd7e14', borderRadius: 5 },
          { label: 'Critique', data: this.riskDistribution.map(d => d.critical), backgroundColor: '#dc3545', borderRadius: 5 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          title: { display: true, text: `Évolution des Risques (Par ${this.getPeriodLabel()})`, font: { size: 16, weight: 'bold' } },
          legend: { position: 'top' }
        },
        scales: {
          y: { beginAtZero: true, title: { display: true, text: 'Nombre de Scénarios' }, stacked: true },
          x: { title: { display: true, text: this.getPeriodLabel() } }
        }
      }
    });
    this.mainCharts.push(chart);
  }

  initPremiumTrendChart(): void {
    const canvas = document.getElementById('premiumTrendChart') as HTMLCanvasElement;
    if (!canvas) return;
    
    const existingChart = Chart.getChart(canvas);
    if (existingChart) {
      existingChart.destroy();
      const index = this.mainCharts.indexOf(existingChart);
      if (index > -1) this.mainCharts.splice(index, 1);
    }
    
    // Use real premium data if available
    const premiumData = this.contractsByPeriod.map(period => {
      if (this.allContracts.length > 0) {
        const periodContracts = this.allContracts.filter(contract => {
          const startDate = new Date(contract.startDate);
          let match = false;
          switch(this.timePeriod) {
            case 'day':
              match = startDate.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' }) === period.label;
              break;
            case 'week':
              match = `S${this.getWeekNumber(startDate)}` === period.label;
              break;
            case 'month':
              match = startDate.toLocaleDateString('fr-FR', { month: 'short', year: 'numeric' }) === period.label;
              break;
            case 'year':
              match = startDate.getFullYear().toString() === period.label;
              break;
          }
          return match;
        });
        return periodContracts.reduce((sum, c) => sum + (c.premium || 0), 0);
      }
      return this.contract?.premium || 0;
    });
    
    const chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: this.contractsByPeriod.map(d => d.label),
        datasets: [{
          label: 'Prime (DT)',
          data: premiumData,
          borderColor: '#007bff',
          backgroundColor: 'rgba(0, 123, 255, 0.1)',
          tension: 0.4,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          title: { display: true, text: `Tendance des Primes`, font: { size: 16, weight: 'bold' } }
        },
        scales: {
          y: { beginAtZero: true, title: { display: true, text: 'Montant (DT)' } }
        }
      }
    });
    this.mainCharts.push(chart);
  }

  initRiskStackedAreaChart(): void {
    const canvas = document.getElementById('riskStackedAreaChart') as HTMLCanvasElement;
    if (!canvas) return;
    
    const existingChart = Chart.getChart(canvas);
    if (existingChart) {
      existingChart.destroy();
      const index = this.mainCharts.indexOf(existingChart);
      if (index > -1) this.mainCharts.splice(index, 1);
    }
    
    const chart = new Chart(canvas, {
      type: 'line',
      data: {
        labels: this.riskDistribution.map(d => d.label),
        datasets: [
          { label: 'Faible Risque', data: this.riskDistribution.map(d => d.low), borderColor: '#28a745', backgroundColor: 'rgba(40, 167, 69, 0.3)', fill: true, tension: 0.4 },
          { label: 'Risque Moyen', data: this.riskDistribution.map(d => d.medium), borderColor: '#ffc107', backgroundColor: 'rgba(255, 193, 7, 0.3)', fill: true, tension: 0.4 },
          { label: 'Risque Élevé', data: this.riskDistribution.map(d => d.high), borderColor: '#fd7e14', backgroundColor: 'rgba(253, 126, 20, 0.3)', fill: true, tension: 0.4 },
          { label: 'Risque Critique', data: this.riskDistribution.map(d => d.critical), borderColor: '#dc3545', backgroundColor: 'rgba(220, 53, 69, 0.3)', fill: true, tension: 0.4 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { 
          title: { display: true, text: `Distribution des Risques dans le Temps`, font: { size: 16, weight: 'bold' } },
          tooltip: { mode: 'index', intersect: false }
        },
        scales: { 
          y: { stacked: true, beginAtZero: true, title: { display: true, text: 'Nombre de Scénarios' } }, 
          x: { stacked: true, title: { display: true, text: this.getPeriodLabel() } } 
        }
      }
    });
    this.mainCharts.push(chart);
  }

  initMiniPieCharts(): void {
    setTimeout(() => {
      this.contractsByPeriod.forEach(period => {
        const canvas = document.getElementById(period.chartId) as HTMLCanvasElement;
        if (canvas && !this.miniCharts.has(period.chartId)) {
          const total = period.active + period.pending + period.rejected;
          if (total > 0) {
            const chart = new Chart(canvas, {
              type: 'doughnut',
              data: {
                datasets: [{
                  data: [period.active, period.pending, period.rejected],
                  backgroundColor: ['#28a745', '#ffc107', '#dc3545'],
                  borderWidth: 0
                }]
              },
              options: {
                responsive: true,
                maintainAspectRatio: true,
                cutout: '60%',
                plugins: { legend: { display: false }, tooltip: { enabled: false } }
              }
            });
            this.miniCharts.set(period.chartId, chart);
          }
        }
      });
    }, 150);
  }

  getPeriodLabel(): string {
    switch(this.timePeriod) {
      case 'day': return 'Jour';
      case 'week': return 'Semaine';
      case 'month': return 'Mois';
      case 'year': return 'Année';
      default: return 'Période';
    }
  }

  getRiskPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return (value / total) * 100;
  }

  getTotalRisk(period: any): number {
    return period.low + period.medium + period.high + period.critical;
  }

  getRiskLevelColor(riskLevel: string): string {
    const colors = { 
      'LOW': '#28a745', 
      'MEDIUM': '#ffc107', 
      'HIGH': '#fd7e14', 
      'CRITICAL': '#dc3545' 
    };
    return colors[riskLevel as keyof typeof colors] || '#6c757d';
  }

  getRiskLevelLabel(riskLevel: string): string {
    const labels = {
      'LOW': 'Faible',
      'MEDIUM': 'Moyen',
      'HIGH': 'Élevé',
      'CRITICAL': 'Critique'
    };
    return labels[riskLevel as keyof typeof labels] || riskLevel || 'Non évalué';
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

  showRiskPeriodDetails(period: any): void {
    this.selectedDetailData = {
      title: `Détails des Risques - ${period.label}`,
      period: period.label,
      data: period
    };
    this.showDetailsModal = true;
  }

  showStatusPeriodDetails(period: any): void {
    this.selectedDetailData = {
      title: `Détails des Statuts - ${period.label}`,
      period: period.label,
      data: period
    };
    this.showDetailsModal = true;
  }

  closeModal(): void {
    this.showDetailsModal = false;
    this.selectedDetailData = null;
  }

  exportToCSV(): void {
    try {
      const csvData = [{
        'Contrat ID': this.contract?.contractId || 'N/A',
        'Client': this.client ? `${this.client.firstName || ''} ${this.client.lastName || ''}` : (this.contract?.client?.firstName + ' ' + this.contract?.client?.lastName || 'N/A'),
        'Email': this.client?.email || this.contract?.client?.email || 'N/A',
        'Statut': this.contract?.status || 'N/A',
        'Prime (DT)': this.contract?.premium || 0,
        'Niveau de Risque': this.getRiskLevelLabel(this.riskEvaluation?.globalRiskLevel),
        'Score de Risque': this.riskEvaluation?.globalRiskScore || 'N/A',
        'Date Début': this.formatDate(this.contract?.startDate),
        'Date Fin': this.formatDate(this.contract?.endDate),
        'Produit': this.contract?.product?.name || 'N/A'
      }];
      
      const headers = Object.keys(csvData[0]);
      const csvRows = [headers.join(',')];
      
      for (const row of csvData) {
        const values = headers.map(header => {
          const value = row[header as keyof typeof row];
          return JSON.stringify(value !== undefined && value !== null ? value : '');
        });
        csvRows.push(values.join(','));
      }
      
      const blob = new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `contrat_${this.contractId}_risques.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      
      this.toastr.success('Export CSV effectué avec succès');
    } catch (error) {
      console.error('Export error:', error);
      this.toastr.error('Erreur lors de l\'export CSV');
    }
  }
}