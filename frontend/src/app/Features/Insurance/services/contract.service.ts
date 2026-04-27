import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { RiskEvaluationDTO } from '../../../shared/dto/risk-evaluation.dto';
import { RiskFactorDTO } from '../../../shared/dto/risk-factor.dto';
import { CategoryRiskDTO } from '../../../shared/dto/category-risk.dto';
import { environment } from '../../../../environments/environment'; 
import { InsuranceContract } from '../../../shared';

export interface CashApprovalRequest {
  id: number;
  paymentId: number;
  contractId: number;
  amount: number;
  clientEmail: string;
  clientName: string;
  clientId: number;
  agentId: number;
  requestedAt: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'FAILED';
  rejectionReason?: string;
  approvedAt?: string;
  rejectedAt?: string;
}


@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private apiUrl = 'http://localhost:8081/contrats';
  private agentApiUrl = 'http://localhost:8081/agent';
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

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

  private handleError(error: any): Observable<never> {
    console.error('API Error:', error);
    let errorMessage = 'Une erreur est survenue';
    
    if (error.error instanceof ErrorEvent) {
      errorMessage = error.error.message;
    } else {
      errorMessage = error.error?.message || `Code: ${error.status}`;
    }
    
    return throwError(() => new Error(errorMessage));
  }

  // ========== CLIENT ENDPOINTS ==========
  
  addContract(contract: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/addCont`, contract, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getMyContracts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/myContracts`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getContractById(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/getCont/${id}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  updateContract(id: number, contract: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/updateCont/${id}`, contract, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  deleteContract(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/deleteCont/${id}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== PAYMENT ENDPOINTS ==========
  
  getPaymentsByContract(contractId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/payments/contract/${contractId}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  /**
   * Effectue un paiement pour un contrat
   * @param paymentData Les données du paiement
   * @returns Observable contenant la réponse du serveur
   */
  makePayment(paymentData: any): Observable<any> {
    const body = {
      clientEmail: paymentData.clientEmail,
      contractId: paymentData.contractId,
      installmentAmount: paymentData.installmentAmount,
      paymentType: paymentData.paymentType,
      remainingAmount: paymentData.remainingAmount || 0
    };

    console.log('📤 Envoi de la requête de paiement:', body);

    return this.http.post(`${this.baseUrl}/payments/payments`, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  /**
   * Traite un paiement CASH après approbation de l'agent
   * @param paymentData Les données du paiement
   * @returns Observable contenant la réponse du serveur
   */
  processApprovedCashPayment(paymentData: any): Observable<any> {
    const body = {
      clientEmail: paymentData.clientEmail,
      contractId: paymentData.contractId,
      installmentAmount: paymentData.installmentAmount,
      paymentType: 'CASH',
      remainingAmount: paymentData.remainingAmount || 0
    };

    console.log('📤 Traitement paiement CASH approuvé:', body);

    return this.http.post(`${this.baseUrl}/payments/process-approved-cash`, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  /**
   * Demande l'approbation pour un paiement CASH
   * @param requestData Les données de la demande
   * @returns Observable contenant la réponse du serveur
   */
  requestCashApproval(requestData: any): Observable<any> {
    return this.http.post(`${environment.apiUrl}/cash-requests/request`, requestData, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }

  /**
   * Vérifie le statut d'une demande CASH
   * @param paymentId L'ID du paiement
   * @returns Observable contenant le statut
   */
  getCashRequestStatus(paymentId: number): Observable<CashApprovalRequest[]> {
    return this.http.get<CashApprovalRequest[]>(`${environment.apiUrl}/cash-requests/payment/${paymentId}`, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }

  getPaymentHistory(contractId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/payments/history/${contractId}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getRemainingBalance(contractId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/payments/remaining-balance/${contractId}`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  createPaymentIntent(contractId: number, amount?: number): Observable<any> {
    const url = amount 
      ? `${environment.apiUrl}/payments/create-payment-intent/${contractId}?amount=${amount}`
      : `${environment.apiUrl}/payments/create-payment-intent/${contractId}`;
    
    return this.http.post(url, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  confirmPayment(paymentIntentId: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/payments/confirm-payment/${paymentIntentId}`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== RISK ENDPOINTS ==========
  
  getContractRisk(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/risk`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  downloadContractPdf(id: number): Observable<Blob> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    
    return this.http.get(`${this.apiUrl}/${id}/download/pdf`, {
      headers: headers,
      responseType: 'blob'
    }).pipe(catchError(this.handleError));
  }

  // ========== ADMIN/AGENT ENDPOINTS ==========
  
  getAllContracts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/allCont`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  activateContract(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/activate/${id}`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  activateContractWithNotification(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/activate-with-notification/${id}`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  rejectContract(id: number, reason: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/reject/${id}`, { reason }, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getPendingContracts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/pending`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getRejectedContracts(): Observable<any> {
    return this.http.get(`${this.apiUrl}/rejected`, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== SYSTEM ENDPOINTS (Admin only) ==========
  
  checkLatePayments(): Observable<any> {
    // Cet endpoint retourne un String
    return this.http.post(`${this.apiUrl}/check-late-payments`, {}, { 
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      catchError(this.handleError),
      map(response => {
        return {
          success: true,
          message: response,
          remindersSent: this.extractCountFromMessage(response)
        };
      })
    );
  }

  checkContractLatePayments(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-late-payments/${id}`, {}, { 
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      catchError(this.handleError),
      map(response => {
        return {
          success: true,
          message: response,
          contractId: id,
          remindersSent: this.extractCountFromMessage(response)
        };
      })
    );
  }

  simulateLatePayments(id: number, months: number): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/simulate-late-payments/${id}/${months}`, {}, { 
      headers: this.getHeaders() 
    }).pipe(catchError(this.handleError));
  }

  checkCompletedContracts(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-completed`, {}, { 
      headers: this.getHeaders() 
    }).pipe(catchError(this.handleError));
  }

  checkEndOfMonth(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-end-of-month`, {}, { 
      headers: this.getHeaders() 
    }).pipe(catchError(this.handleError));
  }

  // Méthode utilitaire pour extraire le nombre d'emails envoyés du message
  private extractCountFromMessage(message: string): number {
    const matches = message.match(/\d+/);
    return matches ? parseInt(matches[0], 10) : 0;
  }

  // ========== AGENT RISK EVALUATION ENDPOINT (HTML Parser) ==========
  
  getRiskEvaluationFromAgent(contractId: number): Observable<RiskEvaluationDTO> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    
    return this.http.get(`${this.agentApiUrl}/risk/evaluation/${contractId}`, {
      headers: headers,
      responseType: 'text'
    }).pipe(
      map(html => this.parseHtmlToRiskEvaluation(html, contractId)),
      catchError(this.handleError)
    );
  }

  // ========== API REST JSON ENDPOINT (Recommandé) ==========
  
  getRiskEvaluationFromApi(contractId: number): Observable<RiskEvaluationDTO> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
  
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
  
    return this.http.get<{ success: boolean; evaluation: RiskEvaluationDTO }>(
      `http://localhost:8081/api/risk/evaluation/${contractId}`, 
      { headers: headers }
    ).pipe(
      map(response => response.evaluation),
      catchError(this.handleError)
    );
  }

  private parseHtmlToRiskEvaluation(html: string, contractId: number): RiskEvaluationDTO {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const body = doc.body;
    
    const evaluation: RiskEvaluationDTO = {
      contractId: contractId
    };
    
    const findElementByText = (container: Element, textContains: string): Element | null => {
      const elements = container.querySelectorAll('p, h1, h2, div, span');
      for (let i = 0; i < elements.length; i++) {
        if (elements[i].textContent?.includes(textContains)) {
          return elements[i];
        }
      }
      return null;
    };
    
    const contractRefElem = body.querySelector('h1 span');
    if (contractRefElem) {
      evaluation.contractReference = contractRefElem.textContent?.trim() || '';
    }
    
    const clientElem = findElementByText(body, 'Client:');
    if (clientElem) {
      const span = clientElem.querySelector('span');
      evaluation.clientName = span?.textContent?.trim() || '';
    }
    
    const agentElem = findElementByText(body, 'Agent:');
    if (agentElem) {
      const span = agentElem.querySelector('span');
      evaluation.agentName = span?.textContent?.trim() || '';
    }
    
    const dateElem = findElementByText(body, 'Date:');
    if (dateElem) {
      const span = dateElem.querySelector('span');
      evaluation.evaluationDate = span?.textContent?.trim() || '';
    }
    
    const scoreElem = findElementByText(body, 'Score:');
    if (scoreElem) {
      const span = scoreElem.querySelector('span');
      evaluation.globalRiskScore = parseFloat(span?.textContent?.trim() || '0');
    }
    
    const riskLevelElem = findElementByText(body, 'Risk Level:');
    if (riskLevelElem) {
      const span = riskLevelElem.querySelector('span');
      evaluation.globalRiskLevel = span?.textContent?.trim() || '';
    }
    
    const riskClassElem = findElementByText(body, 'Risk Class:');
    if (riskClassElem) {
      const span = riskClassElem.querySelector('span');
      evaluation.globalRiskClass = span?.textContent?.trim() || '';
    }
    
    const recommendationElem = findElementByText(body, 'Recommendation:');
    if (recommendationElem) {
      const span = recommendationElem.querySelector('span');
      evaluation.recommendation = span?.textContent?.trim() || '';
    }
    
    const allParagraphs = body.querySelectorAll('p');
    let autoReject = false;
    for (let i = 0; i < allParagraphs.length; i++) {
      if (allParagraphs[i].textContent?.includes('Contract automatically rejected')) {
        autoReject = true;
        break;
      }
    }
    evaluation.autoReject = autoReject;
    
    const categories: { [key: string]: CategoryRiskDTO } = {};
    const allH2 = body.querySelectorAll('h2');
    let categoriesHeader: Element | null = null;
    
    for (let i = 0; i < allH2.length; i++) {
      if (allH2[i].textContent?.includes('Details by Category')) {
        categoriesHeader = allH2[i];
        break;
      }
    }
    
    if (categoriesHeader) {
      const ul = categoriesHeader.nextElementSibling;
      if (ul && ul.tagName === 'UL') {
        const items = ul.querySelectorAll(':scope > li');
        const itemsArray = Array.from(items);
        
        for (const item of itemsArray) {
          const strongElem = item.querySelector(':scope > strong');
          if (strongElem) {
            const categoryName = strongElem.textContent?.replace(':', '').trim() || '';
            const textContent = item.textContent || '';
            
            let riskLevel = '';
            let score = 0;
            let weight = 0;
            
            const riskLevelMatch = textContent.match(/Risk Level:\s*([^\n,]+)/);
            const scoreMatch = textContent.match(/Score:\s*([0-9.]+)/);
            const weightMatch = textContent.match(/Weight:\s*([0-9.]+)/);
            
            if (riskLevelMatch) riskLevel = riskLevelMatch[1].trim();
            if (scoreMatch) score = parseFloat(scoreMatch[1]);
            if (weightMatch) weight = parseFloat(weightMatch[1]);
            
            const p = item.querySelector(':scope > p');
            const description = p?.textContent?.trim() || '';
            
            const details: string[] = [];
            const subUl = item.querySelector(':scope > ul');
            if (subUl) {
              const detailItems = subUl.querySelectorAll(':scope > li');
              const detailItemsArray = Array.from(detailItems);
              for (const li of detailItemsArray) {
                details.push(li.textContent?.trim() || '');
              }
            }
            
            categories[categoryName] = {
              categoryName,
              score,
              weight,
              riskLevel,
              description,
              details
            };
          }
        }
      }
    }
    evaluation.categories = categories;
    
    const riskFactors: RiskFactorDTO[] = [];
    let riskFactorsHeader: Element | null = null;
    
    for (let i = 0; i < allH2.length; i++) {
      if (allH2[i].textContent?.includes('Risk Factors')) {
        riskFactorsHeader = allH2[i];
        break;
      }
    }
    
    if (riskFactorsHeader) {
      const ul = riskFactorsHeader.nextElementSibling;
      if (ul && ul.tagName === 'UL') {
        const items = ul.querySelectorAll(':scope > li');
        const itemsArray = Array.from(items);
        
        for (const item of itemsArray) {
          const strongElem = item.querySelector(':scope > strong');
          if (strongElem) {
            const factor = strongElem.textContent?.replace(':', '').trim() || '';
            const textContent = item.textContent || '';
            
            let impact = '';
            let points = 0;
            
            const impactMatch = textContent.match(/Impact:\s*([^\n,]+)/);
            const pointsMatch = textContent.match(/Points:\s*([0-9.]+)/);
            
            if (impactMatch) impact = impactMatch[1].trim();
            if (pointsMatch) points = parseFloat(pointsMatch[1]);
            
            const p = item.querySelector(':scope > p');
            const description = p?.textContent?.trim() || '';
            
            riskFactors.push({
              factor,
              impact,
              points,
              description
            });
          }
        }
      }
    }
    evaluation.riskFactors = riskFactors;
    
    const positivePoints: string[] = [];
    let positiveHeader: Element | null = null;
    
    for (let i = 0; i < allH2.length; i++) {
      if (allH2[i].textContent?.includes('Positive Points')) {
        positiveHeader = allH2[i];
        break;
      }
    }
    
    if (positiveHeader) {
      const ul = positiveHeader.nextElementSibling;
      if (ul && ul.tagName === 'UL') {
        const items = ul.querySelectorAll(':scope > li');
        const itemsArray = Array.from(items);
        for (const li of itemsArray) {
          positivePoints.push(li.textContent?.trim() || '');
        }
      }
    }
    evaluation.positivePoints = positivePoints;
    
    const recommendedActions: string[] = [];
    let actionsHeader: Element | null = null;
    
    for (let i = 0; i < allH2.length; i++) {
      if (allH2[i].textContent?.includes('Recommended Actions')) {
        actionsHeader = allH2[i];
        break;
      }
    }
    
    if (actionsHeader) {
      const ul = actionsHeader.nextElementSibling;
      if (ul && ul.tagName === 'UL') {
        const items = ul.querySelectorAll(':scope > li');
        const itemsArray = Array.from(items);
        for (const li of itemsArray) {
          recommendedActions.push(li.textContent?.trim() || '');
        }
      }
    }
    evaluation.recommendedActions = recommendedActions;
    
    let reportHeader: Element | null = null;
    
    for (let i = 0; i < allH2.length; i++) {
      if (allH2[i].textContent?.includes('Detailed Report')) {
        reportHeader = allH2[i];
        break;
      }
    }
    
    if (reportHeader) {
      const p = reportHeader.nextElementSibling;
      if (p && p.tagName === 'P') {
        evaluation.detailedReport = p.textContent?.trim() || '';
      }
    }
    
    console.log('Parsed evaluation:', evaluation);
    return evaluation;
  }

  // ========== CASH REQUESTS METHODS ==========

  // Récupérer les demandes en attente pour un agent
  getPendingCashRequests(agentId: number): Observable<CashApprovalRequest[]> {
    console.log('📡 Appel API: getPendingCashRequests pour agentId:', agentId);
    return this.http.get<CashApprovalRequest[]>(`${environment.apiUrl}/cash-requests/pending/${agentId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError),
      tap(response => console.log('📦 Réponse API:', response))
    );
  }

  // Approuver une demande CASH
  approveCashRequest(requestId: number): Observable<any> {
    console.log('✅ Appel API: approveCashRequest pour requestId:', requestId);
    return this.http.post(`${environment.apiUrl}/cash-requests/${requestId}/approve`, {}, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }

  // Rejeter une demande CASH
  rejectCashRequest(requestId: number, reason: string): Observable<any> {
    console.log('❌ Appel API: rejectCashRequest pour requestId:', requestId);
    return this.http.post(`${environment.apiUrl}/cash-requests/${requestId}/reject`, reason, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }

  // Créer une demande CASH (appelé par le client)
  createCashRequest(requestData: any): Observable<any> {
    return this.http.post(`${environment.apiUrl}/cash-requests/request`, requestData, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }


  getAgentContracts(agentId: number): Observable<InsuranceContract[]> {
    return this.http.get<InsuranceContract[]>(`${this.apiUrl}/agents-assurance/${agentId}/contracts`, { headers: this.getHeaders() });
  }



  /**
    * Vérifier et envoyer les rappels pour un contrat spécifique
    * @param contractId L'ID du contrat
    * @returns Observable contenant la réponse avec le nombre de rappels envoyés
  */
  checkContractReminders(contractId: number): Observable<any> {
    return this.http.post(`${environment.apiUrl}/api/reminders/check-contract/${contractId}`, {}, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError),
      map(response => {
        console.log('Rappels envoyés pour contrat', contractId, response);
        return response;
      })
    );
  }


  sendTestReminder(contractId: number, daysBefore: number): Observable<any> {
    return this.http.post(`http://localhost:8081/api/reminders/test/${contractId}/${daysBefore}`, {}, {
      headers: this.getHeaders()
    }).pipe(catchError(this.handleError));
  }
  // ========== UTILITAIRES ==========
  
  isAdmin(): boolean {
    const role = localStorage.getItem('role');
    return role === 'ADMIN';
  }

  isAgent(): boolean {
    const role = localStorage.getItem('role');
    return role === 'AGENT_ASSURANCE';
  }

  isClient(): boolean {
    const role = localStorage.getItem('role');
    return role === 'CLIENT';
  }
}