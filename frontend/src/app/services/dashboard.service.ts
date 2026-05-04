import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class DashboardService {

    private apiUrl = 'http://localhost:8081';

    constructor(private http: HttpClient) {}

    private getAuthHeaders(): HttpHeaders {
        const token = localStorage.getItem('token');
        return new HttpHeaders({
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        });
    }

    // Récupérer toutes les statistiques en un seul appel groupé
    getAllStats(): Observable<any> {
        return forkJoin({
            // News stats
            newsTotal: this.http.get(`${this.apiUrl}/api/v1/news/stats/count`, { headers: this.getAuthHeaders() }),
            newsPublished: this.http.get(`${this.apiUrl}/api/v1/news/stats/published`, { headers: this.getAuthHeaders() }),
            
            // Complaint KPI
            complaintKpi: this.http.get(`${this.apiUrl}/complaints/kpi/dashboard`, { headers: this.getAuthHeaders() })
        }).pipe(
            map((results: any) => {
                return {
                    news: {
                        total: results.newsTotal || 0,
                        published: results.newsPublished || 0,
                        draft: (results.newsTotal || 0) - (results.newsPublished || 0)
                    },
                    complaint: results.complaintKpi
                };
            })
        );
    }
}