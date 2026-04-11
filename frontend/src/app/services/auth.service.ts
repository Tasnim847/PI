import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'  // ça rend le service accessible partout
})
export class AuthService {

  // URL de ton backend
  private API = 'http://localhost:8081/api/auth';
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

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
    if (this.isBrowser) {
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
    }
  }

  // --- RECUPERER ROLE ---
  getRole(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('role');
    }
    return null;
  }

  // --- CHECK LOGIN ---
  isLoggedIn(): boolean {
    if (this.isBrowser) {
      return !!localStorage.getItem('token');
    }
    return false;
  }

  // --- LOGOUT ---
  logout(): void {
    if (this.isBrowser) {
      localStorage.clear();
    }
  }
}