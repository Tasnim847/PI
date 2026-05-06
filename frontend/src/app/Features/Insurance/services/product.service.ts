// src/app/Features/Produit/services/product.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private apiUrl = 'http://localhost:8083/products';

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

  // Récupérer tous les produits
  getAllProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/all`, { headers: this.getHeaders() });
  }

  // Récupérer uniquement les produits actifs
  getActiveProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/activeProducts`, { headers: this.getHeaders() });
  }

  // Alternative: filtrer côté frontend
  getActiveProductsFrontend(): Observable<any[]> {
    return this.getAllProducts().pipe(
      map(products => products.filter(product => product.status === 'ACTIVE' || product.active === true))
    );
  }
}