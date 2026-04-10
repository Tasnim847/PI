import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'  // ça rend le service accessible partout
})
export class AuthService {

  // URL de ton backend
  private API = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) { }

  // --- LOGIN ---
  login(data: { email: string, password: string }): Observable<any> {
    return this.http.post<any>(`${this.API}/login`, data);
  }

  // --- REGISTER avec FormData pour photo ---
  register(formData: FormData): Observable<any> {
    return this.http.post(`${this.API}/register`, formData, {
      responseType: 'text' as 'json'
    });
  }

  // --- STOCKER TOKEN + ROLE ---
  saveSession(token: string, role: string) {
    localStorage.setItem('token', token);
    localStorage.setItem('role', role);
  }

  // --- RECUPERER ROLE ---
  getRole(): string | null {
    return localStorage.getItem('role');
  }

  // --- CHECK LOGIN ---
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  // --- LOGOUT ---
  logout(): void {
    localStorage.clear();
  }
}
