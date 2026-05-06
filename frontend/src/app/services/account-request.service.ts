import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccountRequest {
  id: number;
  clientId: number;
  clientName: string;
  requestedType: string;
  status: string;
  rejectionReason: string | null;
  requestDate: string;
  processedDate: string | null;
}

export interface CreateAccountRequest {
  type: string;
}

@Injectable({ providedIn: 'root' })
export class AccountRequestService {
  private baseUrl = 'http://localhost:8083/api/account-requests';

  constructor(private http: HttpClient) {}

  createRequest(request: CreateAccountRequest): Observable<AccountRequest> {
    return this.http.post<AccountRequest>(this.baseUrl, request);
  }

  getMyRequests(): Observable<AccountRequest[]> {
    return this.http.get<AccountRequest[]>(`${this.baseUrl}/my-requests`);
  }

  getPendingRequests(): Observable<AccountRequest[]> {
    return this.http.get<AccountRequest[]>(`${this.baseUrl}/pending`);
  }

  approveRequest(requestId: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/${requestId}/approve`, {});
  }

  rejectRequest(requestId: number, reason: string): Observable<any> {
    const params = new HttpParams().set('reason', reason);
    return this.http.put(`${this.baseUrl}/${requestId}/reject`, null, { params });
  }
}