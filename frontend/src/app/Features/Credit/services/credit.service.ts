import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Credit {
  creditId: number;
  amount: number;
  interestRate: number;
  monthlyPayment: number;
  durationInMonths: number;
  startDate: string;
  endDate: string;
  dueDate: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'IN_REPAYMENT' | 'CLOSED';
  client: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    telephone: string;
  };
  agentFinance?: any;
  admin?: any;
}

export interface CreditRequest {
  amount: number;
  durationInMonths: number;
  dueDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class CreditService {
  private apiUrl = 'http://localhost:8081';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // ========== CLIENT ==========
  
  /** POST /Credit/requestCredit - Demander un crédit */
  requestCredit(creditRequest: CreditRequest): Observable<Credit> {
    return this.http.post<Credit>(`${this.apiUrl}/Credit/requestCredit`, creditRequest, { headers: this.getHeaders() });
  }

  /** GET /Credit/myCredits - Voir mes crédits */
  getMyCredits(): Observable<Credit[]> {
    return this.http.get<Credit[]>(`${this.apiUrl}/Credit/myCredits`, { headers: this.getHeaders() });
  }

  // ========== ADMIN ==========
  
  /** GET /Credit/allCredit - Voir tous les crédits */
  getAllCredits(): Observable<Credit[]> {
    return this.http.get<Credit[]>(`${this.apiUrl}/Credit/allCredit`, { headers: this.getHeaders() });
  }

  /** PUT /Credit/approve/{id} - Approuver un crédit */
  approveCredit(creditId: number, interestRate: number): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/Credit/approve/${creditId}?interestRate=${interestRate}`, {}, { headers: this.getHeaders() });
  }

  /** PUT /Credit/reject/{id} - Rejeter un crédit */
  rejectCredit(creditId: number): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/Credit/reject/${creditId}`, {}, { headers: this.getHeaders() });
  }

  /** PUT /Credit/updateCredit - Modifier un crédit */
  updateCredit(credit: Credit): Observable<Credit> {
    return this.http.put<Credit>(`${this.apiUrl}/Credit/updateCredit`, credit, { headers: this.getHeaders() });
  }

  /** DELETE /Credit/deleteCredit/{id} - Supprimer un crédit */
  deleteCredit(creditId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/Credit/deleteCredit/${creditId}`, { headers: this.getHeaders() });
  }

  /** GET /Credit/getCredit/{id} - Détail d'un crédit */
  getCreditById(creditId: number): Observable<Credit> {
    return this.http.get<Credit>(`${this.apiUrl}/Credit/getCredit/${creditId}`, { headers: this.getHeaders() });
  }

  /** GET /Credit/closedCreditsWithAverage/{clientId} - Historique client */
  getClosedCreditsWithAverage(clientId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/Credit/closedCreditsWithAverage/${clientId}`, { headers: this.getHeaders() });
  }
}