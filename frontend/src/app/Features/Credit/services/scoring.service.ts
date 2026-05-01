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
  
  // Metrics
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

  // ========== PRIVATE METHODS ==========
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  // ========== CALCULATE FULL SCORE ==========
  calculateCreditScore(clientId: number): Observable<ScoringResponse> {
    return this.http.get<ScoringResponse>(`${this.apiUrl}/calculate/${clientId}`, {
      headers: this.getHeaders()
    });
  }

  // ========== ANALYZE WITH GEMINI ==========
  analyzeClientProfile(clientId: number): Observable<ScoringResponse> {
    return this.http.post<ScoringResponse>(`${this.apiUrl}/analyze/${clientId}`, {}, {
      headers: this.getHeaders()
    });
  }

  // ========== QUICK SCORE ==========
  getQuickScore(clientId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/quick-score/${clientId}`, {
      headers: this.getHeaders()
    });
  }

  // ========== UTILITIES ==========
  getRiskLevelClass(riskLevel: string): string {
    switch(riskLevel?.toUpperCase()) {
      case 'FAIBLE':
      case 'LOW':
        return 'bg-success';
      case 'MOYEN':
      case 'MEDIUM':
        return 'bg-warning';
      case 'ÉLEVÉ':
      case 'HIGH':
        return 'bg-danger';
      case 'TRÈS_ÉLEVÉ':
      case 'VERY_HIGH':
        return 'bg-dark';
      default:
        return 'bg-secondary';
    }
  }

  getRiskLevelLabel(riskLevel: string): string {
    switch(riskLevel?.toUpperCase()) {
      case 'FAIBLE':
      case 'LOW':
        return 'Low Risk';
      case 'MOYEN':
      case 'MEDIUM':
        return 'Medium Risk';
      case 'ÉLEVÉ':
      case 'HIGH':
        return 'High Risk';
      case 'TRÈS_ÉLEVÉ':
      case 'VERY_HIGH':
        return 'Very High Risk';
      default:
        return 'Not Evaluated';
    }
  }

  getRecommendationClass(recommendation: string): string {
    switch(recommendation?.toUpperCase()) {
      case 'APPROUVER':
      case 'APPROVE':
        return 'bg-success';
      case 'APPROUVER_AVEC_CONDITIONS':
      case 'APPROVE_WITH_CONDITIONS':
        return 'bg-warning';
      case 'REJETER':
      case 'REJECT':
        return 'bg-danger';
      default:
        return 'bg-secondary';
    }
  }

  getRecommendationLabel(recommendation: string): string {
    switch(recommendation?.toUpperCase()) {
      case 'APPROUVER':
      case 'APPROVE':
        return 'Approve';
      case 'APPROUVER_AVEC_CONDITIONS':
      case 'APPROVE_WITH_CONDITIONS':
        return 'Approve with Conditions';
      case 'REJETER':
      case 'REJECT':
        return 'Reject';
      default:
        return 'To Evaluate';
    }
  }

  getScoreColor(score: number): string {
    if (score >= 750) return '#28a745'; // Green
    if (score >= 650) return '#ffc107'; // Yellow
    if (score >= 550) return '#fd7e14'; // Orange
    return '#dc3545'; // Red
  }

  getScoreLabel(score: number): string {
    if (score >= 750) return 'Excellent';
    if (score >= 700) return 'Very Good';
    if (score >= 650) return 'Good';
    if (score >= 600) return 'Acceptable';
    if (score >= 550) return 'Average';
    if (score >= 500) return 'Low';
    return 'Very Low';
  }
}