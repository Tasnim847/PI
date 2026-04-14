import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Compensation } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class CompensationService {
  private apiUrl = 'http://localhost:8081/compensations';

  constructor(private http: HttpClient) { }

  // Récupérer les compensations du client connecté
  getMyCompensations(): Observable<Compensation[]> {
    return this.http.get<Compensation[]>(`${this.apiUrl}/my-compensations`);
  }

  // Récupérer une compensation par son ID
  getCompensationById(id: number): Observable<Compensation> {
    return this.http.get<Compensation>(`${this.apiUrl}/getComp/${id}`);
  }

  // Obtenir les détails d'une compensation avec scoring
  getCompensationWithScoring(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/with-scoring`);
  }

  // Obtenir les détails complets d'une compensation pour le client
  getMyCompensationDetails(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/my-details`);
  }

  // Vérifier le solde avant paiement
  checkBalanceBeforePayment(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/check-balance`);
  }

  // Payer une compensation
  payCompensation(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/pay-by-client`, {});
  }

  // Dans compensation.service.ts
  getAllCompensations(): Observable<Compensation[]> {
    return this.http.get<Compensation[]>(`${this.apiUrl}/allComp`);
  }

  recalculateCompensation(claimId: number): Observable<Compensation> {
    return this.http.post<Compensation>(`${this.apiUrl}/recalculate/${claimId}`, {});
  }
}