import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:8082'; // Spring Boot

  constructor(private http: HttpClient) {}

  // Exemple : récupérer les produits actifs
  getActiveProducts() {
    return this.http.get(`${this.baseUrl}/products/activeProducts`);
  }
}