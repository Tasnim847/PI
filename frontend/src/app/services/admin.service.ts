import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Client } from '../shared/models/client.model';  // ← Importer Client

// Garder User pour les autres types (agents, admins)
export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
  role: string;
  photo?: string;
}

// Interface étendue pour les clients avec leurs agents
export interface ClientWithAgents {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone: string;
  role: string;
  photo?: string;
  // 🔥 Version complète avec les objets agents
  agentFinance?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  } | null;
  agentAssurance?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  } | null;
  // Pour la compatibilité avec le code existant
  agentFinanceId?: number | null;
  agentFinanceName?: string | null;
  agentAssuranceId?: number | null;
  agentAssuranceName?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private apiUrl = 'http://localhost:8081';

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

  // ── Clients (retourne ClientWithAgents pour avoir les agents) ──
  getClients(): Observable<ClientWithAgents[]> {
    // Changez l'URL de '/api/clients/all' vers '/api/clients/all-with-agents'
    return this.http.get<ClientWithAgents[]>(`${this.apiUrl}/api/clients/all-with-agents`, { headers: this.getHeaders() });
  }

  addClient(client: FormData): Observable<Client> {
    return this.http.post<Client>(`${this.apiUrl}/api/clients/add`, client, { headers: this.getFormHeaders() });
  }

  updateClient(id: number, client: FormData): Observable<Client> {
    return this.http.put<Client>(`${this.apiUrl}/api/clients/update/${id}`, client, { headers: this.getFormHeaders() });
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/clients/delete/${id}`, { headers: this.getHeaders() });
  }

  // 🆕 Assigner un agent finance à un client
  assignFinanceAgent(clientId: number, agentId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/admin/clients/${clientId}/assign/finance/${agentId}`, {}, {
      headers: this.getHeaders()
    });
  }

  // 🆕 Assigner un agent assurance à un client
  assignAssuranceAgent(clientId: number, agentId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/api/admin/clients/${clientId}/assign/assurance/${agentId}`, {}, {
      headers: this.getHeaders()
    });
  }

  // ── Agents Assurance ──
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

  // ── Agents Finance ──
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

  // ── Admins ──
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