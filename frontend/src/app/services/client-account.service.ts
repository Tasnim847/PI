import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClientAccount {
  accountId: number;
  rip: string;
  balance: number;
  type: string;
  status: string;
  createdAt: string;
  dailyLimit: number;
  monthlyLimit: number;
  dailyTransferLimit: number;
}

@Injectable({ providedIn: 'root' })
export class ClientAccountService {
  private baseUrl = 'http://localhost:8083/api/client';

  constructor(private http: HttpClient) {}

  getMyAccounts(): Observable<ClientAccount[]> {
    return this.http.get<ClientAccount[]>(`${this.baseUrl}/accounts`);
  }

  getAccountById(accountId: number): Observable<ClientAccount> {
    return this.http.get<ClientAccount>(`${this.baseUrl}/accounts/${accountId}`);
  }

  getAccountByRip(rip: string): Observable<ClientAccount> {
    return this.http.get<ClientAccount>(`${this.baseUrl}/accounts/by-rip/${rip}`);
  }
}