// contract.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ContractService {
  private apiUrl = 'http://localhost:8081/contrats';

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
  
  checkLatePayments(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-late-payments`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  checkContractLatePayments(id: number): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-late-payments/${id}`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  simulateLatePayments(id: number, months: number): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/simulate-late-payments/${id}/${months}`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  checkCompletedContracts(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-completed`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
  }

  checkEndOfMonth(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/check-end-of-month`, {}, { headers: this.getHeaders() })
      .pipe(catchError(this.handleError));
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