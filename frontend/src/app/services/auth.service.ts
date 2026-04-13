import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private API = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) { }

  login(data: { email: string, password: string }): Observable<any> {
    return this.http.post<any>(`${this.API}/login`, data);
  }

  register(formData: FormData): Observable<any> {
    return this.http.post(`${this.API}/register`, formData, {
      responseType: 'text' as 'json'
    });
  }
  getMe(): Observable<any> {
    return this.http.get(`${this.API}/me`);
  }

  updateMe(data: any) {
    return this.http.put(
      'http://localhost:8081/api/auth/update-me',
      data,
      { responseType: 'text' }
    );
  }
  // Vérifier si on est dans le navigateur
  private isBrowser(): boolean {
    return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
  }

  saveSession(token: string, role: string) {
    if (this.isBrowser()) {
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
    }
  }

  getRole(): string | null {
    if (this.isBrowser()) {
      return localStorage.getItem('role');
    }
    return null;
  }

  isLoggedIn(): boolean {
    if (this.isBrowser()) {
      return !!localStorage.getItem('token');
    }
    return false;
  }

  changePassword(role: string | null, data: any) {
    let url = '';

    switch (role) {
      case 'ADMIN':
        url = 'http://localhost:8081/admin/change-password';
        break;

      case 'CLIENT':
        url = 'http://localhost:8081/client/change-password';
        break;

      case 'AGENT_FINANCE':
        url = 'http://localhost:8081/agents/finance/change-password'
        break;

      case 'AGENT_ASSURANCE':
        url = 'http://localhost:8081/agent-assurance/change-password';
        break;

      default:
        throw new Error('Role non supporté');
    }

    return this.http.put(url, data);
  }
  getUserId(): number | null {
    if (this.isBrowser()) {
      const token = localStorage.getItem('token');

      if (!token) return null;

      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.id || null;
      } catch (e) {
        console.error('Token invalide', e);
        return null;
      }
    }
    return null;
  }

  logout(): void {
    if (this.isBrowser()) {
      localStorage.clear();
    }
  }
}
