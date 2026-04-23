// src/app/Feature/Complaint/pages/complaint-kpi/complaint-kpi.component.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
// CHEMIN CORRECT - 4 niveaux vers app/ puis services/
import { ComplaintKpiService, DashboardKpiResponse } from '../../../../services/complaint-kpi.service';

@Component({
  selector: 'app-complaint-kpi',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './complaint-kpi.component.html',
  styleUrls: ['./complaint-kpi.component.css']
})
export class ComplaintKpiComponent implements OnInit, OnDestroy {
  
  averageProcessingTime: number = 0;
  resolutionRate: number = 0;
  rejectionRate: number = 0;
  topAgent: string = 'Chargement...';
  
  statistics = {
    total: 0,
    pending: 0,
    inProgress: 0,
    approved: 0,
    rejected: 0,
    closed: 0
  };
  
  isLoading: boolean = false;
  errorMessage: string = '';
  private refreshInterval: any = null;

  constructor(private kpiService: ComplaintKpiService) {}
  
  ngOnInit(): void {
    console.log('📊 Composant KPI initialisé');
    this.loadAllKpis();
    this.refreshInterval = setInterval(() => {
      this.loadAllKpis(false);
    }, 30000);
  }
  
  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }
  
  loadAllKpis(showLoading: boolean = true): void {
    if (showLoading) this.isLoading = true;
    this.errorMessage = '';
    
    this.kpiService.getDashboardKpis().subscribe({
      next: (response: DashboardKpiResponse) => {
        console.log('📊 Données KPI reçues:', response);
        
        this.averageProcessingTime = typeof response.averageProcessingTime === 'number' 
          ? response.averageProcessingTime : 0;
        
        this.resolutionRate = typeof response.resolutionRate === 'number' 
          ? response.resolutionRate : 0;
        
        this.rejectionRate = typeof response.rejectionRate === 'number' 
          ? response.rejectionRate : 0;
        
        this.topAgent = typeof response.topAgent === 'string' 
          ? response.topAgent : 'Non déterminé';
        
        if (response.statistics) {
          this.statistics = response.statistics;
        }
        
        if (showLoading) this.isLoading = false;
      },
      error: (error: any) => {
        console.error('❌ Erreur:', error);
        
        if (error.status === 403) {
          this.errorMessage = '⛔ Accès refusé. Droits insuffisants.';
        } else if (error.status === 401) {
          this.errorMessage = '🔐 Session expirée.';
          setTimeout(() => {
            localStorage.clear();
            window.location.href = '/login';
          }, 2000);
        } else {
          this.errorMessage = `❌ Erreur ${error.status}`;
        }
        
        if (showLoading) this.isLoading = false;
      }
    });
  }
  
  getFormattedAverageTime(): string {
    if (this.averageProcessingTime === 0) return 'Non calculé';
    const days = Math.floor(this.averageProcessingTime);
    const hours = Math.round((this.averageProcessingTime - days) * 24);
    
    if (days === 0 && hours === 0) return '< 1 jour';
    if (days === 0) return `${hours} heure(s)`;
    if (hours === 0) return `${days} jour(s)`;
    return `${days}j ${hours}h`;
  }
  
  getFormattedRate(rate: number): string {
    if (isNaN(rate)) return '0%';
    return `${rate.toFixed(1)}%`;
  }
  
  getResolutionRateClass(): string {
    if (this.resolutionRate >= 80) return 'success';
    if (this.resolutionRate >= 50) return 'warning';
    return 'danger';
  }
  
  getRejectionRateClass(): string {
    if (this.rejectionRate <= 20) return 'success';
    if (this.rejectionRate <= 40) return 'warning';
    return 'danger';
  }
  
  getAverageTimeClass(): string {
    if (this.averageProcessingTime <= 3) return 'success';
    if (this.averageProcessingTime <= 7) return 'warning';
    return 'danger';
  }
  
  refresh(): void {
    this.loadAllKpis(true);
  }
  
  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return (value / total) * 100;
  }
  
  goBack(): void {
    window.history.back();
  }
}