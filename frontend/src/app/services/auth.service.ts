import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

export interface UserInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  telephone?: string;
  photo?: string;
}

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

  // ✅ Ajout de clientLat et clientLon optionnels
  login(data: {
    email: string;
    password: string;
    clientLat?: number | null;
    clientLon?: number | null;
  }): Observable<any> {
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

  // ✅ NOUVELLE MÉTHODE getCurrentUser
  getCurrentUser(): UserInfo | null {
    if (!this.isBrowser) return null;
    
    // Essayer de récupérer depuis localStorage d'abord
    const storedUser = localStorage.getItem('user_info');
    if (storedUser) {
      try {
        return JSON.parse(storedUser);
      } catch (e) {
        console.error('Error parsing user info', e);
      }
    }
    
    // Sinon, construire à partir des données stockées
    const token = this.getToken();
    const userId = this.getUserId();
    const firstName = this.getUserFirstName();
    const lastName = this.getUserLastName();
    const email = this.getUserEmail();
    const role = this.getRole();
    
    if (token && userId) {
      return {
        id: userId,
        firstName: firstName || '',
        lastName: lastName || '',
        email: email || '',
        role: role || 'CLIENT'
      };
    }
    
    return null;
  }
  
  // ✅ NOUVELLE MÉTHODE pour mettre à jour l'utilisateur dans le storage
  setCurrentUser(user: UserInfo): void {
    if (this.isBrowser) {
      localStorage.setItem('user_info', JSON.stringify(user));
      localStorage.setItem('userId', user.id.toString());
      localStorage.setItem('firstName', user.firstName);
      localStorage.setItem('lastName', user.lastName);
      localStorage.setItem('userEmail', user.email);
      localStorage.setItem('role', user.role);
    }
  }

  updateMe(data: any): Observable<any> {
    return this.http.put(
      'http://localhost:8081/api/auth/update-me',
      data,
      { responseType: 'text' }
    ).pipe(catchError(this.handleError));
  }

  changePassword(role: string | null, data: any): Observable<any> {
    let url = '';

    switch (role) {
      case 'ADMIN':
        url = `http://localhost:8081/admins/change-password/${data.id}`;
        const params = new URLSearchParams();
        params.set('oldPassword', data.oldPassword);
        params.set('newPassword', data.newPassword);
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

  saveSession(token: string, role: string, userId?: number | null, firstName?: string, lastName?: string, email?: string) {
    if (this.isBrowser) {
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      if (userId) localStorage.setItem('userId', userId.toString());
      if (firstName) localStorage.setItem('firstName', firstName);
      if (lastName) localStorage.setItem('lastName', lastName);
      if (email) localStorage.setItem('userEmail', email);
      
      // ✅ Sauvegarder aussi l'objet complet
      const userInfo: UserInfo = {
        id: userId || 0,
        firstName: firstName || '',
        lastName: lastName || '',
        email: email || '',
        role: role
      };
      localStorage.setItem('user_info', JSON.stringify(userInfo));
    }
  }

  getToken(): string | null {
    return this.isBrowser ? localStorage.getItem('token') : null;
  }

  getRole(): string | null {
    return this.isBrowser ? localStorage.getItem('role') : null;
  }

  getUserId(): number | null {
    if (this.isBrowser) {
      const storedUserId = localStorage.getItem('userId');
      if (storedUserId && !isNaN(parseInt(storedUserId))) {
        return parseInt(storedUserId);
      }
      const token = this.getToken();
      if (!token) return null;
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.id || payload.userId || null;
      } catch (e) {
        return null;
      }
    }
    return null;
  }

  getUserFirstName(): string | null {
    return this.isBrowser ? localStorage.getItem('firstName') : null;
  }

  getUserLastName(): string | null {
    return this.isBrowser ? localStorage.getItem('lastName') : null;
  }

  getUserEmail(): string | null {
    return this.isBrowser ? localStorage.getItem('userEmail') : null;
  }

  isLoggedIn(): boolean {
    return this.isBrowser ? !!this.getToken() : false;
  }

  logout(): void {
    if (this.isBrowser) localStorage.clear();
  }
}