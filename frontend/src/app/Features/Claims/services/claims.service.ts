// claims.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClaimDTO } from '../../../shared/dto/claim-dto.model';
import { ClaimScoreDTO, ClientScoreResult, DetailedAnalysis } from '../../../shared/dto/scoring-dto.model';
import { Claim } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class ClaimsService {
  private apiUrl = 'http://localhost:8081/claims';
  private scoringApiUrl = 'http://localhost:8081/api/scoring';


  constructor(private http: HttpClient) { }

  // Ajoutez cette méthode dans ClaimsService
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





  // ============ NOUVEAUX ENDPOINTS DE SCORING ============
  
  /**
   * Get advanced scoring for a specific claim
   */
  getAdvancedClaimScore(claimId: number): Observable<ClaimScoreDTO> {
    return this.http.get<ClaimScoreDTO>(`${this.scoringApiUrl}/claim/${claimId}/advanced`);
  }

  /**
   * Auto decision with advanced AI scoring
   */
  autoDecisionWithAdvancedScoring(claimId: number): Observable<ClaimDTO> {
    return this.http.post<ClaimDTO>(`${this.scoringApiUrl}/claim/${claimId}/auto-decision-advanced`, {});
  }

  /**
   * Get detailed analysis for a claim (combines claim and client scoring)
   */
  getDetailedClaimAnalysis(claimId: number): Observable<DetailedAnalysis> {
    return this.http.get<DetailedAnalysis>(`${this.scoringApiUrl}/claim/${claimId}/detailed-analysis`);
  }

  /**
   * Get client score
   */
  getClientScore(clientId: number): Observable<ClientScoreResult> {
    return this.http.get<ClientScoreResult>(`${this.scoringApiUrl}/client/${clientId}`);
  }

  /**
   * Get all claims with scores for dashboard
   */
  getAllClaimsWithScores(): Observable<ClaimScoreDTO[]> {
    // You might need to create a batch endpoint or call individually
    return this.http.get<ClaimScoreDTO[]>(`${this.scoringApiUrl}/claims/all-scores`);
  }


  // Dans votre ClaimsService, vous avez maintenant :
getAllClaims(): Observable<Claim[]> {
  return this.http.get<Claim[]>(`${this.apiUrl}/claims/all`, { headers: this.getHeaders() });
}
  
}