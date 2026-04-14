// services/admin.service.ts
import { Injectable } from '@angular/core';
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
  // ✅ Enlever /api - URL correcte
  private apiUrl = 'http://localhost:8081';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // Clients
  getClients(): Observable<User[]> {
    const headers = this.getHeaders();
    console.log('📡 Appel API:', `${this.apiUrl}/clients/all`);
    return this.http.get<User[]>(`${this.apiUrl}/clients/all`, { headers });
  }

  addClient(client: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post<User>(`${this.apiUrl}/clients/add`, client, { headers });
  }

  updateClient(id: number, client: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.put<User>(`${this.apiUrl}/clients/update/${id}`, client, { headers });
  }

  deleteClient(id: number): Observable<void> {
    const headers = this.getHeaders();
    return this.http.delete<void>(`${this.apiUrl}/clients/delete/${id}`, { headers });
  }

  // Agents Assurance
  getAgentsAssurance(): Observable<User[]> {
    const headers = this.getHeaders();
    console.log('📡 Appel API:', `${this.apiUrl}/agents-assurance/all`);
    return this.http.get<User[]>(`${this.apiUrl}/agents-assurance/all`, { headers });
  }

  addAgentAssurance(agent: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post<User>(`${this.apiUrl}/agents-assurance/add`, agent, { headers });
  }

  updateAgentAssurance(id: number, agent: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.put<User>(`${this.apiUrl}/agents-assurance/update/${id}`, agent, { headers });
  }

  deleteAgentAssurance(id: number): Observable<void> {
    const headers = this.getHeaders();
    return this.http.delete<void>(`${this.apiUrl}/agents-assurance/delete/${id}`, { headers });
  }

  // Agents Finance
  getAgentsFinance(): Observable<User[]> {
    const headers = this.getHeaders();
    console.log('📡 Appel API:', `${this.apiUrl}/agents/finance/all`);
    return this.http.get<User[]>(`${this.apiUrl}/agents/finance/all`, { headers });
  }

  addAgentFinance(agent: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post<User>(`${this.apiUrl}/agents/finance/add`, agent, { headers });
  }

  updateAgentFinance(id: number, agent: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.put<User>(`${this.apiUrl}/agents/finance/update/${id}`, agent, { headers });
  }

  deleteAgentFinance(id: number): Observable<void> {
    const headers = this.getHeaders();
    return this.http.delete<void>(`${this.apiUrl}/agents/finance/delete/${id}`, { headers });
  }

  // Admins
  getAdmins(): Observable<User[]> {
    const headers = this.getHeaders();
    console.log('📡 Appel API:', `${this.apiUrl}/admins/all`);
    return this.http.get<User[]>(`${this.apiUrl}/admins/all`, { headers });
  }

  addAdmin(admin: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.post<User>(`${this.apiUrl}/admins/add`, admin, { headers });
  }

  updateAdmin(id: number, admin: FormData): Observable<User> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
    return this.http.put<User>(`${this.apiUrl}/admins/update/${id}`, admin, { headers });
  }

  deleteAdmin(id: number): Observable<void> {
    const headers = this.getHeaders();
    return this.http.delete<void>(`${this.apiUrl}/admins/delete/${id}`, { headers });
  }
}
