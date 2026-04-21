// src/app/pages/dashboard/dashboard.component.ts
import { Component, OnInit, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { StatisticsService } from '../../services/statistics.service';
import { ConnectionStatsService } from '../../services/connection-stats.service';
import {ConnectionMapComponent} from './connection-map/connection-map.component'; // 👈 Ajouter

interface DashboardStats {
  totalUsers: number;
  totalClients: number;
  totalAgentsAssurance: number;
  totalAgentsFinance: number;
  totalAdmins: number;
  recentActivities: string[];
  connectionStats: {
    weekdayConnections: number;
    weekendConnections: number;
    totalConnections: number;
    weekdayPercentage: number;
    weekendPercentage: number;
    startDate: Date;
    endDate: Date;
  };
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, ConnectionMapComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats = {
    totalUsers: 0,
    totalClients: 0,
    totalAgentsAssurance: 0,
    totalAgentsFinance: 0,
    totalAdmins: 0,
    recentActivities: [],
    connectionStats: {
      weekdayConnections: 0,
      weekendConnections: 0,
      totalConnections: 0,
      weekdayPercentage: 0,
      weekendPercentage: 0,
      startDate: new Date(),
      endDate: new Date()
    }
  };

  loading = true;
  error: string | null = null;
  selectedPeriod: number = 7; // 7, 30, ou 0 pour mois courant

  constructor(
    private statsService: StatisticsService,
    private connectionStatsService: ConnectionStatsService, // 👈 Ajouter
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.loadDashboardStats();
    } else {
      this.loading = false;
    }
  }

  loadDashboardStats() {
    console.log('🔄 Chargement des stats...');
    this.loading = true;
    this.error = null;

    this.statsService.getDashboardStats().subscribe({
      next: (data: DashboardStats) => {
        console.log('✅ Stats reçues:', data);
        this.stats = data;
        this.loading = false;
      },
      error: (err: Error) => {
        console.error('❌ Erreur:', err);
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  // 👈 NOUVEAU: Changer la période pour les connexions
  changePeriod(period: number) {
    this.selectedPeriod = period;
    this.loading = true;

    let request;
    if (period === 7) {
      request = this.connectionStatsService.getLast7DaysStats();
    } else if (period === 30) {
      request = this.connectionStatsService.getLast30DaysStats();
    } else {
      request = this.connectionStatsService.getCurrentMonthStats();
    }

    request.subscribe({
      next: (connectionStats) => {
        this.stats.connectionStats = {
          weekdayConnections: connectionStats.weekdayConnections,
          weekendConnections: connectionStats.weekendConnections,
          totalConnections: connectionStats.totalConnections,
          weekdayPercentage: connectionStats.weekdayPercentage,
          weekendPercentage: connectionStats.weekendPercentage,
          startDate: connectionStats.startDate,
          endDate: connectionStats.endDate
        };
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement stats connexions:', err);
        this.loading = false;
      }
    });
  }

  navigateTo(type: string) {
    this.router.navigate([`/backoffice/${type}`], {
      queryParams: { openAdd: true }
    });
  }

  getRolePercentage(roleCount: number): number {
    if (this.stats.totalUsers === 0) return 0;
    return (roleCount / this.stats.totalUsers) * 100;
  }

  retry() {
    this.loadDashboardStats();
  }

  getTotalConnections(): number {
    return this.stats.connectionStats.totalConnections;
  }

  getWeekdayPercentage(): number {
    return this.stats.connectionStats.weekdayPercentage;
  }

  getWeekendPercentage(): number {
    return this.stats.connectionStats.weekendPercentage;
  }

  getRoleDistribution() {
    return [
      { name: 'Clients', count: this.stats.totalClients, percentage: this.getRolePercentage(this.stats.totalClients), color: '#2193b0' },
      { name: 'Insurance Agents', count: this.stats.totalAgentsAssurance, percentage: this.getRolePercentage(this.stats.totalAgentsAssurance), color: '#11998e' },
      { name: 'Finance Agents', count: this.stats.totalAgentsFinance, percentage: this.getRolePercentage(this.stats.totalAgentsFinance), color: '#f2994a' },
      { name: 'Administrators', count: this.stats.totalAdmins, percentage: this.getRolePercentage(this.stats.totalAdmins), color: '#eb3349' }
    ];
  }

  getConnectionDistribution() {
    return [
      { name: 'Weekday', count: this.stats.connectionStats.weekdayConnections, percentage: this.getWeekdayPercentage(), color: '#667eea' },
      { name: 'Weekend', count: this.stats.connectionStats.weekendConnections, percentage: this.getWeekendPercentage(), color: '#764ba2' }
    ];
  }
}
