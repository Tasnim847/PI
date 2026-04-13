// claims.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClaimDTO } from '../../../shared/dto/claim-dto.model';

@Injectable({
  providedIn: 'root'
})
export class ClaimsService {
  private apiUrl = 'http://localhost:8081/claims';

  constructor(private http: HttpClient) { }

  getMyClaims(): Observable<ClaimDTO[]> {
    return this.http.get<ClaimDTO[]>(`${this.apiUrl}/allClaim`);
  }

  getClaimById(id: number): Observable<ClaimDTO> {
    return this.http.get<ClaimDTO>(`${this.apiUrl}/getClaim/${id}`);
  }

  addClaim(claimDTO: ClaimDTO, files: File[]): Observable<ClaimDTO> {
    const formData = new FormData();
    
    // Nettoyer le DTO avant envoi
    const cleanDTO: any = {
      contractId: claimDTO.contractId,
      claimedAmount: claimDTO.claimedAmount,
      description: claimDTO.description
    };
    
    // Ajouter les détails spécifiques s'ils existent
    if ((claimDTO as any).autoDetails) {
      cleanDTO.autoDetails = (claimDTO as any).autoDetails;
    }
    if ((claimDTO as any).healthDetails) {
      cleanDTO.healthDetails = (claimDTO as any).healthDetails;
    }
    if ((claimDTO as any).homeDetails) {
      cleanDTO.homeDetails = (claimDTO as any).homeDetails;
    }
    
    const claimJson = JSON.stringify(cleanDTO);
    console.log('JSON envoyé au backend:', claimJson);
    
    formData.append('claim', claimJson);
    
    files.forEach(file => {
      formData.append('documents', file);
    });
    
    return this.http.post<ClaimDTO>(`${this.apiUrl}/addClaim`, formData);
  }

  updateClaim(claimDTO: ClaimDTO): Observable<ClaimDTO> {
    return this.http.put<ClaimDTO>(`${this.apiUrl}/updateClaim`, claimDTO);
  }

  deleteClaim(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/deleteClaim/${id}`);
  }

  getCompensationDetails(claimId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/calculate-compensation/${claimId}`);
  }

  autoDecision(claimId: number): Observable<ClaimDTO> {
    return this.http.post<ClaimDTO>(`${this.apiUrl}/claim/${claimId}/auto-decision`, {});
  }
}