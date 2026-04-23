import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
  role: string;
  photo?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = 'http://localhost:8082';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  private getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('token');
    }
    return null;
  }

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.getToken()}`,
      'Content-Type': 'application/json'
    });
  }

  private getFormHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Authorization': `Bearer ${this.getToken()}`
    });
  }

  // ── Clients ──────────────────────────────────────────────
  // Backend: @RequestMapping("/api/clients") → /api/clients/all
  getClients(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/api/clients/all`, { headers: this.getHeaders() });
  }

  addClient(client: FormData): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/api/clients/add`, client, { headers: this.getFormHeaders() });
  }

  updateClient(id: number, client: FormData): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/api/clients/update/${id}`, client, { headers: this.getFormHeaders() });
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/clients/delete/${id}`, { headers: this.getHeaders() });
  }

  // ── Agents Assurance ─────────────────────────────────────
  // Backend: @RequestMapping("/agents-assurance")
  getAgentsAssurance(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/agents-assurance/all`, { headers: this.getHeaders() });
  }

  addAgentAssurance(agent: FormData): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/agents-assurance/add`, agent, { headers: this.getFormHeaders() });
  }

  updateAgentAssurance(id: number, agent: FormData): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/agents-assurance/update/${id}`, agent, { headers: this.getFormHeaders() });
  }

  deleteAgentAssurance(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/agents-assurance/delete/${id}`, { headers: this.getHeaders() });
  }

  // ── Agents Finance ───────────────────────────────────────
  // Backend: @RequestMapping("/agents/finance")
  getAgentsFinance(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/agents/finance/all`, { headers: this.getHeaders() });
  }

  addAgentFinance(agent: FormData): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/agents/finance/add`, agent, { headers: this.getFormHeaders() });
  }

  updateAgentFinance(id: number, agent: FormData): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/agents/finance/update/${id}`, agent, { headers: this.getFormHeaders() });
  }

  deleteAgentFinance(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/agents/finance/delete/${id}`, { headers: this.getHeaders() });
  }

  // ── Admins ───────────────────────────────────────────────
  // Backend: @RequestMapping("/admins")
  getAdmins(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/admins/all`, { headers: this.getHeaders() });
  }

  addAdmin(admin: FormData): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/admins/add`, admin, { headers: this.getFormHeaders() });
  }

  updateAdmin(id: number, admin: FormData): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/admins/update/${id}`, admin, { headers: this.getFormHeaders() });
  }

  deleteAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admins/delete/${id}`, { headers: this.getHeaders() });
  }
}