import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClaimDTO } from '../../../shared/dto/claim-dto.model';

@Injectable({
  providedIn: 'root'
})
export class ClaimsService {
  private apiUrl = 'http://localhost:8081/claims'; // À adapter selon votre backend

  constructor(private http: HttpClient) { }

  // Récupérer tous les claims du client connecté
  getMyClaims(): Observable<ClaimDTO[]> {
    return this.http.get<ClaimDTO[]>(`${this.apiUrl}/allClaim`);
  }

  // Récupérer un claim par son ID
  getClaimById(id: number): Observable<ClaimDTO> {
    return this.http.get<ClaimDTO>(`${this.apiUrl}/getClaim/${id}`);
  }

  // Ajouter un nouveau claim
  addClaim(claimDTO: ClaimDTO): Observable<ClaimDTO> {
    return this.http.post<ClaimDTO>(`${this.apiUrl}/addClaim`, claimDTO);
  }

  // Mettre à jour un claim
  updateClaim(claimDTO: ClaimDTO): Observable<ClaimDTO> {
    return this.http.put<ClaimDTO>(`${this.apiUrl}/updateClaim`, claimDTO);
  }

  // Supprimer un claim
  deleteClaim(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/deleteClaim/${id}`);
  }

  // Obtenir les détails de compensation d'un claim
  getCompensationDetails(claimId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/calculate-compensation/${claimId}`);
  }

  // Décision automatique
  autoDecision(claimId: number): Observable<ClaimDTO> {
    return this.http.post<ClaimDTO>(`${this.apiUrl}/claim/${claimId}/auto-decision`, {});
  }
}