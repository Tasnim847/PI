// src/app/Features/services/complaint.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ComplaintService {

  private apiUrl = 'http://localhost:8082/complaints';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  // Utiliser le nouvel endpoint /all-simple
  getComplaints(): Observable<any[]> {
    console.log('📡 Appel API: GET /complaints/all-simple');
    return this.http.get(`${this.apiUrl}/all-simple`, { 
      headers: this.getAuthHeaders()
    }).pipe(
      map((response: any) => {
        console.log('🔍 Réponse brute de l\'API:', response);
        
        // Extraire le tableau des réclamations
        let complaintsArray = [];
        
        if (response && response.complaints && Array.isArray(response.complaints)) {
          complaintsArray = response.complaints;
          console.log(`✅ ${complaintsArray.length} réclamation(s) trouvée(s) dans response.complaints`);
        } else if (Array.isArray(response)) {
          complaintsArray = response;
          console.log(`✅ ${complaintsArray.length} réclamation(s) trouvée(s) dans le tableau direct`);
        } else {
          console.warn('⚠️ Format non reconnu');
          complaintsArray = [];
        }
        
        return complaintsArray;
      })
    );
  }

  createComplaint(complaint: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/addComplaint`, complaint, { 
      headers: this.getAuthHeaders()
    });
  }

  updateComplaint(id: number, complaint: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/updateComplaint/${id}`, complaint, { 
      headers: this.getAuthHeaders()
    });
  }

  deleteComplaint(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/deleteComplaint/${id}`, { 
      headers: this.getAuthHeaders()
    });
  }
}