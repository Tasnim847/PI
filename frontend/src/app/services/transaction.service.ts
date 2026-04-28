import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction } from '../shared/models/transaction.model';
import { AccountStatisticsDTO } from '../shared/dto/account-statistics.dto';
@Injectable({ providedIn: 'root' })
export class TransactionService {

  private baseUrl = 'http://localhost:8081/api/transactions';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // GET ALL
  getAllTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/alltransaction`, {
      headers: this.getHeaders()
    });
  }

  // GET BY ID
  getTransactionById(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`, {
      headers: this.getHeaders()
    });
  }

  // GET BY ACCOUNT
  getTransactionsByAccount(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/account/${accountId}`, {
      headers: this.getHeaders()
    });
  }

  // CREATE
  createTransaction(accountId: number, transaction: Transaction): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/account/${accountId}`, transaction, {
      headers: this.getHeaders()
    });
  }

  // UPDATE
  updateTransaction(id: number, transaction: Transaction): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${id}`, transaction, {
      headers: this.getHeaders()
    });
  }

  // DELETE
  deleteTransaction(id: number): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${id}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    });
  }

  // TRANSFER
  transfer(fromAccountId: number, toAccountId: number, amount: number): Observable<string> {
    const params = new HttpParams()
      .set('fromAccountId', fromAccountId.toString())
      .set('toAccountId', toAccountId.toString())
      .set('amount', amount.toString());

    return this.http.post(`${this.baseUrl}/transfer`, null, {
      headers: this.getHeaders(),
      params,
      responseType: 'text'
    });
  }
  //stats 
  getAccountStatistics(accountId: number): Observable<AccountStatisticsDTO> {
  return this.http.get<AccountStatisticsDTO>(`${this.baseUrl}/statistics/${accountId}`, {
    headers: this.getHeaders()
  });
}
//pdf 
// Dans transaction.service.ts
getAccountStatement(accountId: number): Observable<Blob> {
  return this.http.get(`${this.baseUrl}/statement/${accountId}`, {
    headers: this.getHeaders(),
    responseType: 'blob'
  });
}
// Ajoutez cette méthode à votre TransactionService existant

transferByRip(sourceRip: string, targetRip: string, amount: number, description: string): Observable<string> {
  const body = {
    sourceRip: sourceRip,
    targetRip: targetRip,
    amount: amount,
    description: description
  };
  return this.http.post(`${this.baseUrl}/transfer/by-rip`, body, {
    responseType: 'text'
  });
}
}