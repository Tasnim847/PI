import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Repayment {
  repaymentId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'STRIPE';
  reference: string;
  status: 'PAID' | 'LATE' | 'FAILED';
  credit?: {
    creditId: number;
    amount: number;
    monthlyPayment: number;
  };
  client?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  };
}

export interface StripePaymentIntent {
  clientSecret: string;
  paymentIntentId: string;
  creditId: number;
  amount: number;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class RepaymentService {
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
  
  /** POST /Repayment/pay-credit/{id} - Payer un crédit (CASH/CARD/BANK_TRANSFER) */
  payCredit(creditId: number, amount: number, paymentMethod: string): Observable<Repayment> {
    const body = {
      amount: amount,
      paymentMethod: paymentMethod,
      paymentDate: new Date().toISOString().split('T')[0]
    };
    return this.http.post<Repayment>(`${this.apiUrl}/Repayment/pay-credit/${creditId}`, body, { headers: this.getHeaders() });
  }

  /** POST /Repayment/stripe-pay/{id} - Paiement Stripe */
  stripePay(creditId: number, amount: number): Observable<StripePaymentIntent> {
    const body = {
      amount: amount,
      paymentMethod: 'STRIPE',
      paymentDate: new Date().toISOString().split('T')[0]
    };
    return this.http.post<StripePaymentIntent>(`${this.apiUrl}/Repayment/stripe-pay/${creditId}`, body, { headers: this.getHeaders() });
  }

  /** GET /Repayment/remaining/{id} - Montant restant à payer */
  getRemainingAmount(creditId: number): Observable<{ remainingAmount: number }> {
    return this.http.get<{ remainingAmount: number }>(`${this.apiUrl}/Repayment/remaining/${creditId}`, { headers: this.getHeaders() });
  }

  /** GET /Repayment/myPayments - Mes paiements */
  getMyPayments(): Observable<Repayment[]> {
    return this.http.get<Repayment[]>(`${this.apiUrl}/Repayment/myPayments`, { headers: this.getHeaders() });
  }

  /** GET /Repayment/credits/{id}/amortissement/pdf - Télécharger PDF */
  downloadAmortissementPdf(creditId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/Repayment/credits/${creditId}/amortissement/pdf`, {
      headers: this.getHeaders(),
      responseType: 'blob'
    });
  }

  // ========== ADMIN ==========
  
  /** GET /Repayment/history/{id} - Historique des paiements d'un crédit */
  getPaymentHistory(creditId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/Repayment/history/${creditId}`, { headers: this.getHeaders() });
  }

  /** GET /Repayment/allRepayment - Tous les paiements */
  getAllPayments(): Observable<Repayment[]> {
    return this.http.get<Repayment[]>(`${this.apiUrl}/Repayment/allRepayment`, { headers: this.getHeaders() });
  }

  /** GET /Repayment/getRepayment/{id} - Détail d'un paiement */
  getPaymentById(paymentId: number): Observable<Repayment> {
    return this.http.get<Repayment>(`${this.apiUrl}/Repayment/getRepayment/${paymentId}`, { headers: this.getHeaders() });
  }

  /** POST /Repayment/addRepayment - Ajouter un paiement (admin) */
  addRepayment(repayment: Repayment): Observable<Repayment> {
    return this.http.post<Repayment>(`${this.apiUrl}/Repayment/addRepayment`, repayment, { headers: this.getHeaders() });
  }

  /** PUT /Repayment/updateRepayment - Modifier un paiement */
  updateRepayment(repayment: Repayment): Observable<Repayment> {
    return this.http.put<Repayment>(`${this.apiUrl}/Repayment/updateRepayment`, repayment, { headers: this.getHeaders() });
  }

  /** DELETE /Repayment/deleteRepayment/{id} - Supprimer un paiement */
  deleteRepayment(paymentId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/Repayment/deleteRepayment/${paymentId}`, { headers: this.getHeaders() });
  }

  /** POST /Repayment/credits/{id}/send-pdf-email - Envoyer PDF par email */
  sendPdfByEmail(creditId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/Repayment/credits/${creditId}/send-pdf-email`, {}, { headers: this.getHeaders() });
  }
}