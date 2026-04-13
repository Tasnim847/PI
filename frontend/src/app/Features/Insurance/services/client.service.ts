// client.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  private baseUrl = 'http://localhost:8081/api';
  private usersUrl = `${this.baseUrl}/users`;
  private clientsUrl = `${this.baseUrl}/clients`;

  constructor(private http: HttpClient) {}

  getUserById(userId: number): Observable<any> {
    return this.http.get(`${this.usersUrl}/${userId}`);
  }

  getClientById(clientId: number): Observable<any> {
    return this.http.get(`${this.clientsUrl}/${clientId}`);
  }

  getAllClients(): Observable<any[]> {
    return this.http.get<any[]>(this.clientsUrl);
  }

  getClientByEmail(email: string): Observable<any> {
    return this.http.get(`${this.clientsUrl}/email/${email}`);
  }
}