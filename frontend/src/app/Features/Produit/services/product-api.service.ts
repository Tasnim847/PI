import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class ProductApiService {

  private baseUrl = 'http://localhost:8081';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  getActiveProducts(): Observable<any[]> {
    let headers = new HttpHeaders().set('Content-Type', 'application/json');
    
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('token');
      console.log('Token présent:', !!token);
      console.log('Token valeur:', token); // Attention: ne pas log en production
      
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
        console.log('Headers configurés:', headers.get('Authorization'));
      }
    }
    
    return this.http.get<any[]>(`${this.baseUrl}/products/activeProducts`, { headers });
  }
}