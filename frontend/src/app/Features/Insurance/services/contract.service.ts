import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { RiskEvaluationDTO } from '../../../shared/dto/risk-evaluation.dto';
import { RiskFactorDTO } from '../../../shared/dto/risk-factor.dto';
import { CategoryRiskDTO } from '../../../shared/dto/category-risk.dto';
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
  // 🔥 URL de base unique - CORRIGÉE sur le port 8083
  private readonly API_BASE_URL = 'http://localhost:8083';
  
  private apiUrl = `${this.API_BASE_URL}/contrats`;
  private agentApiUrl = `${this.API_BASE_URL}/agent`;

  constructor(private http: HttpClient) {
    console.log('🔧 Service initialisé avec API_BASE_URL:', this.API_BASE_URL);
  }

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
    const url = `${this.API_BASE_URL}/payments/contract/${contractId}`;
    console.log('📡 getPaymentsByContract:', url);
    return this.http.get<any[]>(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  makePayment(paymentData: any): Observable<any> {
    const body = {
      clientEmail: paymentData.clientEmail,
      contractId: paymentData.contractId,
      installmentAmount: paymentData.installmentAmount,
      paymentType: paymentData.paymentType,
      remainingAmount: paymentData.remainingAmount || 0
    };

    const url = `${this.API_BASE_URL}/payments/payments`;
    console.log('📤 makePayment:', url);
    return this.http.post(url, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  processApprovedCashPayment(paymentData: any): Observable<any> {
    const body = {
      clientEmail: paymentData.clientEmail,
      contractId: paymentData.contractId,
      installmentAmount: paymentData.installmentAmount,
      paymentType: 'CASH',
      remainingAmount: paymentData.remainingAmount || 0
    };

    const url = `${this.API_BASE_URL}/payments/process-approved-cash`;
    console.log('📤 processApprovedCashPayment:', url);
    return this.http.post(url, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  requestCashApproval(requestData: any): Observable<any> {
    const url = `${this.API_BASE_URL}/cash-requests/request`;
    return this.http.post(url, requestData, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getCashRequestStatus(paymentId: number): Observable<CashApprovalRequest[]> {
    const url = `${this.API_BASE_URL}/cash-requests/payment/${paymentId}`;
    return this.http.get<CashApprovalRequest[]>(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getPaymentHistory(contractId: number): Observable<any[]> {
    const url = `${this.API_BASE_URL}/payments/history/${contractId}`;
    return this.http.get<any[]>(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  getRemainingBalance(contractId: number): Observable<any> {
    const url = `${this.API_BASE_URL}/payments/remaining-balance/${contractId}`;
    return this.http.get(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // 🔥 CORRIGÉ - Utilise le bon port 8083
  createPaymentIntent(contractId: number, amount?: number): Observable<any> {
    const url = amount 
      ? `${this.API_BASE_URL}/payments/create-payment-intent/${contractId}?amount=${amount}`
      : `${this.API_BASE_URL}/payments/create-payment-intent/${contractId}`;
    
    console.log('📤 createPaymentIntent URL:', url);
    return this.http.post(url, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // 🔥 CORRIGÉ - Utilise le bon port 8083
  confirmPayment(paymentIntentId: string): Observable<any> {
    const url = `${this.API_BASE_URL}/payments/confirm-payment/${paymentIntentId}`;
    console.log('📤 confirmPayment URL:', url);
    return this.http.post(url, {}, { headers: this.getHeaders() })
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
    const url = `${this.apiUrl}/activate/${id}`;
    console.log('📡 activateContract:', url);
    return this.http.put(url, {}, { 
      headers: this.getHeaders(),
      responseType: 'json'
    }).pipe(
      tap(response => console.log('✅ Réponse activation:', response)),
      catchError((error) => {
        if (error.status === 403) {
          return throwError(() => new Error('Vous n\'avez pas les droits pour activer un contrat. Vérifiez que vous êtes connecté en tant qu\'agent assurance.'));
        }
        if (error.status === 404) {
          return throwError(() => new Error(`Contrat ${id} non trouvé`));
        }
        if (error.status === 400) {
          const errorMessage = error.error?.message || error.error || 'Impossible d\'activer ce contrat';
          return throwError(() => new Error(errorMessage));
        }
        return this.handleError(error);
      })
    );
  }

  activateContractWithNotification(id: number): Observable<any> {
    const url = `${this.apiUrl}/activate-with-notification/${id}`;
    return this.http.put(url, {}, { headers: this.getHeaders() })
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

  getAgentContracts(agentId: number): Observable<InsuranceContract[]> {
    return this.http.get<InsuranceContract[]>(`${this.apiUrl}/agents-assurance/${agentId}/contracts`, { headers: this.getHeaders() });
  }

  // ========== SYSTEM ENDPOINTS ==========
  
  checkLatePayments(): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-late-payments`, {}, { 
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      catchError(this.handleError),
      map(response => ({
        success: true,
        message: response,
        remindersSent: this.extractCountFromMessage(response)
      }))
    );
  }

  checkContractLatePayments(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/check-late-payments/${id}`, {}, { 
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      catchError(this.handleError),
      map(response => ({
        success: true,
        message: response,
        contractId: id,
        remindersSent: this.extractCountFromMessage(response)
      }))
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

  checkContractReminders(contractId: number): Observable<any> {
    const url = `${this.API_BASE_URL}/api/reminders/check-contract/${contractId}`;
    return this.http.post(url, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  sendTestReminder(contractId: number, daysBefore: number): Observable<any> {
    const url = `${this.API_BASE_URL}/api/reminders/test/${contractId}/${daysBefore}`;
    return this.http.post(url, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== CASH REQUESTS METHODS ==========

  getPendingCashRequests(agentId: number): Observable<CashApprovalRequest[]> {
    const url = `${this.API_BASE_URL}/cash-requests/pending/${agentId}`;
    console.log('📡 getPendingCashRequests:', url);
    return this.http.get<CashApprovalRequest[]>(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  approveCashRequest(requestId: number): Observable<any> {
    const url = `${this.API_BASE_URL}/cash-requests/${requestId}/approve`;
    console.log('✅ approveCashRequest:', url);
    return this.http.post(url, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  rejectCashRequest(requestId: number, reason: string): Observable<any> {
    const url = `${this.API_BASE_URL}/cash-requests/${requestId}/reject`;
    console.log('❌ rejectCashRequest:', url);
    return this.http.post(url, reason, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  createCashRequest(requestData: any): Observable<any> {
    const url = `${this.API_BASE_URL}/cash-requests/request`;
    console.log('📤 createCashRequest:', url);
    return this.http.post(url, requestData, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== BANK TRANSFER ==========

  checkBankBalance(rip: string, amountToPay: number): Observable<any> {
    const url = `${this.API_BASE_URL}/payments/check-balance/${rip}?amount=${amountToPay}`;
    return this.http.get(url, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  payByBankTransfer(paymentData: any): Observable<any> {
    const body = {
      clientEmail: paymentData.clientEmail,
      contractId: paymentData.contractId,
      installmentAmount: paymentData.installmentAmount,
      paymentType: 'BANK_TRANSFER',
      remainingAmount: paymentData.remainingAmount || 0,
      sourceRip: paymentData.sourceRip
    };
  
    const url = `${this.API_BASE_URL}/payments/pay-by-bank-transfer`;
    console.log('📤 payByBankTransfer:', url);
    return this.http.post(url, body, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  // ========== AGENT RISK EVALUATION ==========
  
  getRiskEvaluationFromAgent(contractId: number): Observable<RiskEvaluationDTO> {
    const url = `${this.agentApiUrl}/risk/evaluation/${contractId}`;
    return this.http.get(url, {
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      map(html => this.parseHtmlToRiskEvaluation(html, contractId)),
      catchError(this.handleError)
    );
  }

  getRiskEvaluationFromApi(contractId: number): Observable<RiskEvaluationDTO> {
    const url = `${this.API_BASE_URL}/api/risk/evaluation/${contractId}`;
    return this.http.get<{ success: boolean; evaluation: RiskEvaluationDTO }>(url, { headers: this.getHeaders() })
      .pipe(
        map(response => response.evaluation),
        catchError(this.handleError)
      );
  }

  // ========== UTILITAIRES ==========
  
  private extractCountFromMessage(message: string): number {
    const matches = message.match(/\d+/);
    return matches ? parseInt(matches[0], 10) : 0;
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

  isAdmin(): boolean {
    return localStorage.getItem('role') === 'ADMIN';
  }

  isAgent(): boolean {
    return localStorage.getItem('role') === 'AGENT_ASSURANCE';
  }

  isClient(): boolean {
    return localStorage.getItem('role') === 'CLIENT';
  }
}