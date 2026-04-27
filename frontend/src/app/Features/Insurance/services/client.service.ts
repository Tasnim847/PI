// client.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Client } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  private baseUrl = 'http://localhost:8081/api';
  private usersUrl = `${this.baseUrl}/users`;
  private clientsUrl = `${this.baseUrl}/clients`;

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

  getUserById(userId: number): Observable<any> {
    return this.http.get(`${this.usersUrl}/${userId}`, { headers: this.getHeaders() });
  }

  getClientById(clientId: number): Observable<any> {
    return this.http.get(`${this.clientsUrl}/${clientId}`, { headers: this.getHeaders() });
  }

  getAllClients(): Observable<any[]> {
    return this.http.get<any[]>(this.clientsUrl, { headers: this.getHeaders() });
  }

  getClientByEmail(email: string): Observable<any> {
    return this.http.get(`${this.clientsUrl}/email/${email}`, { headers: this.getHeaders() });
  }

  // ✅ Méthode corrigée - utilise this.clientsUrl au lieu de this.apiUrl
  getClientsByAgent(agentId: number): Observable<Client[]> {
    return this.http.get<Client[]>(`${this.baseUrl}/agents-assurance/${agentId}/clients`, { headers: this.getHeaders() });
  }
}