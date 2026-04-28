import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreditScore {
  clientId: number;
  clientName: string;
  clientEmail: string;
  score: number;
  riskLevel: string;
  recommendation: string;
  analysis: string;
  calculatedAt: string;
  
  // Métriques
  totalCredits: number;
  activeCredits: number;
  closedCredits: number;
  totalAmount: number;
  currentDebt: number;
  averageLatePercentage: number;
  averageMonthlyPayment: number;
  daysSinceLastCredit: number;
}

export interface ScoringResponse {
  success: boolean;
  message: string;
  data?: CreditScore;
  clientId?: number;
  score?: number;
  riskLevel?: string;
  recommendation?: string;
  analysis?: string;
  calculatedAt?: string;
  metrics?: any;
}

@Injectable({
  providedIn: 'root'
})
export class ScoringService {
  private apiUrl = 'http://localhost:8081/Scoring';

  constructor(private http: HttpClient) { }

  // ========== MÉTHODES PRIVÉES ==========
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  // ========== CALCULER LE SCORE COMPLET ==========
  calculateCreditScore(clientId: number): Observable<ScoringResponse> {
    return this.http.get<ScoringResponse>(`${this.apiUrl}/calculate/${clientId}`, {
      headers: this.getHeaders()
    });
  }

  // ========== ANALYSER AVEC GEMINI ==========
  analyzeClientProfile(clientId: number): Observable<ScoringResponse> {
    return this.http.post<ScoringResponse>(`${this.apiUrl}/analyze/${clientId}`, {}, {
      headers: this.getHeaders()
    });
  }

  // ========== SCORE RAPIDE ==========
  getQuickScore(clientId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/quick-score/${clientId}`, {
      headers: this.getHeaders()
    });
  }

  // ========== UTILITAIRES ==========
  getRiskLevelClass(riskLevel: string): string {
    switch(riskLevel?.toUpperCase()) {
      case 'FAIBLE':
        return 'bg-success';
      case 'MOYEN':
        return 'bg-warning';
      case 'ÉLEVÉ':
        return 'bg-danger';
      case 'TRÈS_ÉLEVÉ':
        return 'bg-dark';
      default:
        return 'bg-secondary';
    }
  }

  getRiskLevelLabel(riskLevel: string): string {
    switch(riskLevel?.toUpperCase()) {
      case 'FAIBLE':
        return 'Risque Faible';
      case 'MOYEN':
        return 'Risque Moyen';
      case 'ÉLEVÉ':
        return 'Risque Élevé';
      case 'TRÈS_ÉLEVÉ':
        return 'Risque Très Élevé';
      default:
        return 'Non évalué';
    }
  }

  getRecommendationClass(recommendation: string): string {
    switch(recommendation?.toUpperCase()) {
      case 'APPROUVER':
        return 'bg-success';
      case 'APPROUVER_AVEC_CONDITIONS':
        return 'bg-warning';
      case 'REJETER':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getRecommendationLabel(recommendation: string): string {
    switch(recommendation?.toUpperCase()) {
      case 'APPROUVER':
        return 'Approuver';
      case 'APPROUVER_AVEC_CONDITIONS':
        return 'Approuver avec conditions';
      case 'REJETER':
        return 'Rejeter';
      default:
        return 'À évaluer';
    }
  }

  getScoreColor(score: number): string {
    if (score >= 750) return '#28a745'; // Vert
    if (score >= 650) return '#ffc107'; // Jaune
    if (score >= 550) return '#fd7e14'; // Orange
    return '#dc3545'; // Rouge
  }

  getScoreLabel(score: number): string {
    if (score >= 750) return 'Excellent';
    if (score >= 700) return 'Très bon';
    if (score >= 650) return 'Bon';
    if (score >= 600) return 'Acceptable';
    if (score >= 550) return 'Moyen';
    if (score >= 500) return 'Faible';
    return 'Très faible';
  }
}