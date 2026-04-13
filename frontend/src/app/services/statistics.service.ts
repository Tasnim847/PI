// services/statistics.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private apiUrl = 'http://localhost:8081';

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<any> {
    const token = localStorage.getItem('token');

    console.log('🔑 Token:', token ? 'Présent' : 'Absent');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    // ✅ CORRECTION: Ajouter /api/ au début du chemin
    const url = `${this.apiUrl}/api/admin/dashboard/stats`;
    console.log('📡 URL appelée:', url);

    return this.http.get(url, { headers });
  }
}
