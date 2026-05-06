import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

// Types
export type CreditStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'IN_REPAYMENT' | 'CLOSED';

export interface Client {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
}

export interface Credit {
  creditId: number;
  amount: number;
  interestRate: number;
  monthlyPayment: number;
  durationInMonths: number;
  startDate: string;
  endDate: string;
  dueDate: string;
  status: CreditStatus;
  client?: Client;
  clientId: number | null;
  clientFirstName?: string;
  clientLastName?: string;
  clientFullName?: string;
  clientEmail?: string;
  clientPhone?: string;
}

export interface CreditRequest {
  amount: number;
  durationInMonths: number;
  dueDate: string;
}

export interface CreditHistoryDTO {
  credit: Credit;
  lateRepaymentPercentage: number;
}

export interface CreditHistoryWithAverageDTO {
  credits: CreditHistoryDTO[];
  averageLatePercentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class CreditService {
  private apiUrl = 'http://localhost:8083/Credit';

  constructor(private http: HttpClient) { }

  // ========== MÉTHODES PRIVÉES ==========
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    });
  }

  // ========== CLIENT - DEMANDE DE CRÉDIT ==========
  requestCredit(request: CreditRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/requestCredit`, request, {
      headers: this.getHeaders()
    });
  }

  // ========== CLIENT - MES CRÉDITS ==========
  getMyCredits(): Observable<Credit[]> {
    return this.http.get<Credit[]>(`${this.apiUrl}/myCredits`, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN/AGENT_FINANCE - TOUS LES CRÉDITS ==========
  getAllCredits(): Observable<Credit[]> {
    return this.http.get<Credit[]>(`${this.apiUrl}/allCredit`, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN - CRÉDIT PAR ID ==========
  getCreditById(id: number): Observable<Credit> {
    return this.http.get<Credit>(`${this.apiUrl}/getCredit/${id}`, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN - AJOUTER UN CRÉDIT ==========
  addCredit(credit: Credit): Observable<Credit> {
    return this.http.post<Credit>(`${this.apiUrl}/addCredit`, credit, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN - MODIFIER UN CRÉDIT ==========
  updateCredit(credit: Credit): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/updateCredit`, credit, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN - SUPPRIMER UN CRÉDIT ==========
  deleteCredit(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/deleteCredit/${id}`, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN/AGENT_FINANCE - APPROUVER ==========
  approveCredit(id: number, interestRate: number): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/approve/${id}?interestRate=${interestRate}`, null, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN/AGENT_FINANCE - REJETER ==========
  rejectCredit(id: number): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/reject/${id}`, null, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN/AGENT_FINANCE - HISTORIQUE CLIENT ==========
  getClosedCreditsWithAverage(clientId: number): Observable<CreditHistoryWithAverageDTO> {
    return this.http.get<CreditHistoryWithAverageDTO>(
      `${this.apiUrl}/closedCreditsWithAverage/${clientId}`,
      { headers: this.getHeaders() }
    );
  }

  // ========== TEST NOTIFICATION ==========
  testNotification(creditId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/test-notification/${creditId}`, null, {
      headers: this.getHeaders()
    });
  }

  // ========== ADMIN - SEND AMORTIZATION EMAIL ==========
  sendAmortizationEmail(creditId: number): Observable<any> {
    return this.http.post(`http://localhost:8083/Repayment/credits/${creditId}/send-pdf-email`, null, {
      headers: this.getHeaders()
    });
  }
}
