// src/app/services/connection-stats.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConnectionStats {
  weekdayConnections: number;
  weekendConnections: number;
  totalConnections: number;
  weekdayPercentage: number;
  weekendPercentage: number;
  startDate: Date;
  endDate: Date;
  periodDays?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ConnectionStatsService {
  // Utilise le même port que ton AdminService (8083)
  private apiUrl = 'http://localhost:8081/api/admin';

  constructor(private http: HttpClient) {}

  // Statistiques des 7 derniers jours
  getLast7DaysStats(): Observable<ConnectionStats> {
    return this.http.get<ConnectionStats>(`${this.apiUrl}/connection-stats/last7days`);
  }

  // Statistiques des 30 derniers jours
  getLast30DaysStats(): Observable<ConnectionStats> {
    return this.http.get<ConnectionStats>(`${this.apiUrl}/connection-stats/last30days`);
  }

  // Statistiques du mois en cours
  getCurrentMonthStats(): Observable<ConnectionStats> {
    return this.http.get<ConnectionStats>(`${this.apiUrl}/connection-stats/current-month`);
  }

  // Statistiques personnalisées (ex: 15 jours)
  getStatsForDays(days: number): Observable<ConnectionStats> {
    return this.http.get<ConnectionStats>(`${this.apiUrl}/connection-stats?days=${days}`);
  }
}
