// services/auth.service.ts - Complete working version

import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private API = 'http://localhost:8081/api/auth';
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  login(data: { email: string, password: string }): Observable<any> {
    return this.http.post<any>(`${this.API}/login`, data).pipe(
      catchError(this.handleError)
    );
  }

  register(formData: FormData): Observable<any> {
    localStorage.removeItem('token');
    return this.http.post(`${this.API}/register`, formData, {
      responseType: 'text' as 'json'
    }).pipe(catchError(this.handleError));
  }

  getMe(): Observable<any> {
    if (!this.isBrowser || !this.getToken()) {
      return throwError(() => new Error('No token found'));
    }
    return this.http.get(`${this.API}/me`).pipe(catchError(this.handleError));
  }

  updateMe(data: any): Observable<any> {
    return this.http.put(
      'http://localhost:8081/api/auth/update-me',
      data,
      { responseType: 'text' }
    ).pipe(catchError(this.handleError));
  }

  // ✅ FIXED: Change password based on role
  changePassword(role: string | null, data: any): Observable<any> {
    let url = '';

    switch (role) {
      case 'ADMIN':
        // Admin: Use path variable for ID
        url = `http://localhost:8081/admins/change-password/${data.id}`;
        const params = new URLSearchParams();
        params.set('oldPassword', data.oldPassword);
        params.set('newPassword', data.newPassword);

        console.log('Admin change password URL:', url);
        console.log('Admin params:', params.toString());

        return this.http.post(url, params.toString(), {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Authorization': `Bearer ${this.getToken()}`
          },
          responseType: 'text'
        }).pipe(catchError(this.handleError));

      case 'CLIENT':
        url = 'http://localhost:8081/api/clients/change-password';
        return this.http.put(url, {
          id: data.id,
          oldPassword: data.oldPassword,
          newPassword: data.newPassword
        }, {
          headers: { 'Authorization': `Bearer ${this.getToken()}` },
          responseType: 'text'
        }).pipe(catchError(this.handleError));

      case 'AGENT_FINANCE':
        url = 'http://localhost:8081/agents/finance/change-password';
        return this.http.put(url, {
          id: data.id,
          oldPassword: data.oldPassword,
          newPassword: data.newPassword
        }, {
          headers: { 'Authorization': `Bearer ${this.getToken()}` },
          responseType: 'text'
        }).pipe(catchError(this.handleError));

      case 'AGENT_ASSURANCE':
        url = 'http://localhost:8081/agents-assurance/change-password';
        return this.http.put(url, {
          id: data.id,
          oldPassword: data.oldPassword,
          newPassword: data.newPassword
        }, {
          headers: { 'Authorization': `Bearer ${this.getToken()}` },
          responseType: 'text'
        }).pipe(catchError(this.handleError));

      default:
        throw new Error('Role non supporté');
    }
  }

  updateProfile(userId: number, formData: FormData): Observable<any> {
    const role = this.getRole();
    let url = '';

    switch (role) {
      case 'CLIENT':
        url = `http://localhost:8081/api/clients/update/${userId}`;
        break;
      case 'AGENT_FINANCE':
        url = `http://localhost:8081/agents/finance/update/${userId}`;
        break;
      case 'AGENT_ASSURANCE':
        url = `http://localhost:8081/agents-assurance/update/${userId}`;
        break;
      case 'ADMIN':
        url = `http://localhost:8081/admins/update/${userId}`;
        break;
      default:
        throw new Error('Role non supporté');
    }

    return this.http.put(url, formData, {
      headers: { 'Authorization': `Bearer ${this.getToken()}` }
    }).pipe(catchError(this.handleError));
  }

  uploadPhoto(userId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('photo', file);
    return this.updateProfile(userId, formData);
  }

  private handleError(error: any) {
    console.error('API Error:', error);
    return throwError(() => error);
  }

  // Session management
  saveSession(token: string, role: string, userId?: number | null, firstName?: string, lastName?: string, email?: string) {
    if (this.isBrowser) {
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      if (userId) {
        localStorage.setItem('userId', userId.toString());
      }
      if (firstName) {
        localStorage.setItem('firstName', firstName);
      }
      if (lastName) {
        localStorage.setItem('lastName', lastName);
      }
      if (email) {
        localStorage.setItem('userEmail', email);
      }
    }
  }

  getToken(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('token');
    }
    return null;
  }

  getRole(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('role');
    }
    return null;
  }

  getUserId(): number | null {
    if (this.isBrowser) {
      // First try localStorage
      const storedUserId = localStorage.getItem('userId');
      if (storedUserId && !isNaN(parseInt(storedUserId))) {
        return parseInt(storedUserId);
      }

      // Then try from token
      const token = this.getToken();
      if (!token) return null;

      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.id || payload.userId || null;
      } catch (e) {
        console.error('Error extracting userId:', e);
        return null;
      }
    }
    return null;
  }

  getUserFirstName(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('firstName');
    }
    return null;
  }

  getUserLastName(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('lastName');
    }
    return null;
  }

  getUserEmail(): string | null {
    if (this.isBrowser) {
      return localStorage.getItem('userEmail');
    }
    return null;
  }

  isLoggedIn(): boolean {
    if (this.isBrowser) {
      return !!this.getToken();
    }
    return false;
  }

  logout(): void {
    if (this.isBrowser) {
      localStorage.clear();
    }
  }
}