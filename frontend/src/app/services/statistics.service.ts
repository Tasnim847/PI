// src/app/services/statistics.service.ts
import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, of, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { AdminService } from './admin.service';
import { ConnectionStatsService } from './connection-stats.service'; // 👈 Ajouter

@Injectable({ providedIn: 'root' })
export class StatisticsService {

  constructor(
    private adminService: AdminService,
    private connectionStatsService: ConnectionStatsService, // 👈 Ajouter
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  getDashboardStats(): Observable<any> {
    if (!isPlatformBrowser(this.platformId)) {
      return of({
        totalClients: 0,
        totalAgentsAssurance: 0,
        totalAgentsFinance: 0,
        totalAdmins: 0,
        totalUsers: 0,
        recentActivities: [],
        connectionStats: {
          weekdayConnections: 0,
          weekendConnections: 0,
          totalConnections: 0,
          weekdayPercentage: 0,
          weekendPercentage: 0
        }
      });
    }

    // Récupérer les stats utilisateurs ET les stats de connexion en parallèle
    return forkJoin({
      clients: this.adminService.getClients(),
      agentsAssurance: this.adminService.getAgentsAssurance(),
      agentsFinance: this.adminService.getAgentsFinance(),
      admins: this.adminService.getAdmins(),
      connectionStats: this.connectionStatsService.getLast7DaysStats() // 👈 Appel API réel
    }).pipe(
      map(({ clients, agentsAssurance, agentsFinance, admins, connectionStats }) => ({
        totalClients: clients.length,
        totalAgentsAssurance: agentsAssurance.length,
        totalAgentsFinance: agentsFinance.length,
        totalAdmins: admins.length,
        totalUsers: clients.length + agentsAssurance.length + agentsFinance.length + admins.length,
        recentActivities: [],
        connectionStats: {
          weekdayConnections: connectionStats.weekdayConnections,
          weekendConnections: connectionStats.weekendConnections,
          totalConnections: connectionStats.totalConnections,
          weekdayPercentage: connectionStats.weekdayPercentage,
          weekendPercentage: connectionStats.weekendPercentage,
          startDate: connectionStats.startDate,
          endDate: connectionStats.endDate
        }
      }))
    );
  }
}