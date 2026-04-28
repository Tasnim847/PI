import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account } from '../shared/models/account.model';

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  private baseUrl = 'http://localhost:8083/api/accounts';

  constructor(private http: HttpClient) {}

  // ===== GET ALL — ADMIN only =====
  getAllAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/allaccount`);
  }

  // ===== GET BY ID — ADMIN / AGENT_FINANCE / AGENT_ASSURANCE / CLIENT =====
  getAccountById(id: number): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`);
  }

  // ===== CREATE — ADMIN only =====
  createAccount(account: Account): Observable<Account> {
    return this.http.post<Account>(`${this.baseUrl}/addaccount`, account);
  }

  // ===== UPDATE — ADMIN only =====
  updateAccount(id: number, account: Account): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/${id}`, account);
  }

  // ===== DELETE — ADMIN only =====
  deleteAccount(id: number): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${id}`, { responseType: 'text' });
  }

  // ===== SET WITHDRAWAL LIMITS — ADMIN only =====
  setLimits(id: number, dailyLimit: number, monthlyLimit: number): Observable<Account> {
    const params = new HttpParams()
      .set('dailyLimit', dailyLimit.toString())
      .set('monthlyLimit', monthlyLimit.toString());

    return this.http.put<Account>(
      `${this.baseUrl}/${id}/limits`,
      null,
      { params }
    );
  }

  // 🆕 SET TRANSFER LIMIT — ADMIN only
  setTransferLimit(id: number, dailyTransferLimit: number): Observable<Account> {
    const params = new HttpParams()
      .set('dailyTransferLimit', dailyTransferLimit.toString());

    return this.http.put<Account>(
      `${this.baseUrl}/${id}/transfer-limit`,
      null,
      { params }
    );
  }

  // ===== GET BY CLIENT ID =====
  getAccountsByClientId(clientId: number): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/client/${clientId}`);
  }

  // ===== GET BY STATUS =====
  getAccountsByStatus(status: string): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/status/${status}`);
  }

  // ===== GET BY RIP =====
  getAccountByRip(rip: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/by-rip/${rip}`);
  }
}