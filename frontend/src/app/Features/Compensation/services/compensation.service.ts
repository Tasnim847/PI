import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Compensation } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class CompensationService {
  private apiUrl = 'http://localhost:8083/compensations';

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    
    return headers;
  }

  // Get client's compensations
  getMyCompensations(): Observable<Compensation[]> {
    return this.http.get<Compensation[]>(`${this.apiUrl}/my-compensations`, { headers: this.getHeaders() });
  }

  // Get compensation by ID
  getCompensationById(id: number): Observable<Compensation> {
    return this.http.get<Compensation>(`${this.apiUrl}/getComp/${id}`, { headers: this.getHeaders() });
  }

  // Get compensation details with scoring
  getCompensationWithScoring(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/with-scoring`, { headers: this.getHeaders() });
  }

  // Get complete compensation details for client
  getMyCompensationDetails(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/my-details`, { headers: this.getHeaders() });
  }

  // ✅ Paiement par CARTE (Stripe)
  payCompensationByCard(compensationId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${compensationId}/pay-by-card`, {}, { headers: this.getHeaders() });
  }

  // ✅ Paiement en CASH
  payCompensationByCash(compensationId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${compensationId}/pay-by-cash`, {}, { headers: this.getHeaders() });
  }

  // ✅ Confirmer paiement Stripe
  confirmCompensationPayment(paymentIntentId: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/confirm-payment/${paymentIntentId}`, {}, { headers: this.getHeaders() });
  }

  // Get all compensations (admin)
  getAllCompensations(): Observable<Compensation[]> {
    return this.http.get<Compensation[]>(`${this.apiUrl}/allComp`, { headers: this.getHeaders() });
  }

  // Recalculate compensation
  recalculateCompensation(claimId: number): Observable<Compensation> {
    return this.http.post<Compensation>(`${this.apiUrl}/recalculate/${claimId}`, {}, { headers: this.getHeaders() });
  }

  // Ajouter cette méthode dans CompensationService
  markAsPaid(compensationId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${compensationId}/pay`, {}, { 
      headers: this.getHeaders(),
      observe: 'response'  // Pour voir la réponse complète
    });
  } 

  // Dans CompensationService, ajoutez ces méthodes :

  // Get compensations des clients de l'agent connecté
  getAgentCompensations(): Observable<Compensation[]> {
    return this.http.get<Compensation[]>(`${this.apiUrl}/agent/compensations`, { headers: this.getHeaders() });
  }

  // Get la liste des clients de l'agent
  getAgentClients(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/agent/clients`, { headers: this.getHeaders() });
  }

  // Dans compensation.service.ts
  // Dans compensation.service.ts - CORRECTION DE L'URL
  getAdminCompensationDetails(compensationId: number): Observable<any> {
    // ❌ MAUVAIS (actuel) : /admin/compensations/1/details
    // ✅ BON : /compensations/1/with-scoring (déjà existant)
    return this.http.get(`${this.apiUrl}/${compensationId}/with-scoring`, { headers: this.getHeaders() });
  }
}