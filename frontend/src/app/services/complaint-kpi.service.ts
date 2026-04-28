// src/app/Features/services/complaint-kpi.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AverageProcessingTimeResponse {
  averageProcessingTime: number;
  unit: string;
}

export interface RateResponse {
  resolutionRate?: number;
  rejectionRate?: number;
  unit: string;
}

export interface TopAgentResponse {
  topAgent: string;
}

export interface DashboardKpiResponse {
  averageProcessingTime: number | any;
  resolutionRate: number | any;
  rejectionRate: number | any;
  topAgent: string | any;
  statistics?: {
    total: number;
    pending: number;
    inProgress: number;
    approved: number;
    rejected: number;
    closed: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class ComplaintKpiService {
  
  private apiUrl = 'http://localhost:8081/complaints';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // Dashboard complet (tous les KPIs en un seul appel)
  getDashboardKpis(): Observable<DashboardKpiResponse> {
    return this.http.get<DashboardKpiResponse>(
      `${this.apiUrl}/kpi/dashboard`,
      { headers: this.getAuthHeaders() }
    );
  }

  // Temps moyen de traitement
  getAverageProcessingTime(): Observable<AverageProcessingTimeResponse> {
    return this.http.get<AverageProcessingTimeResponse>(
      `${this.apiUrl}/kpi/average-processing-time`,
      { headers: this.getAuthHeaders() }
    );
  }

  // Taux de résolution
  getResolutionRate(): Observable<RateResponse> {
    return this.http.get<RateResponse>(
      `${this.apiUrl}/kpi/resolution-rate`,
      { headers: this.getAuthHeaders() }
    );
  }

  // Taux de rejet
  getRejectionRate(): Observable<RateResponse> {
    return this.http.get<RateResponse>(
      `${this.apiUrl}/kpi/rejection-rate`,
      { headers: this.getAuthHeaders() }
    );
  }

  // Meilleur agent
  getTopAgent(): Observable<TopAgentResponse> {
    return this.http.get<TopAgentResponse>(
      `${this.apiUrl}/kpi/top-agent`,
      { headers: this.getAuthHeaders() }
    );
  }
}