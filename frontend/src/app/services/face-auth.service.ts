// services/face-auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FaceAuthService {
  private API = 'http://localhost:8083/api/auth/face';

  constructor(private http: HttpClient) {}

  registerFace(faceImage: File): Observable<any> {
    const formData = new FormData();
    formData.append('faceImage', faceImage);
    return this.http.post(`${this.API}/register`, formData);
  }

  loginWithFace(faceImage: File): Observable<any> {
    const formData = new FormData();
    formData.append('faceImage', faceImage);
    return this.http.post(`${this.API}/login`, formData);
  }

  // ✅ NOUVELLE MÉTHODE - Vérifier sans authentification
  checkFaceExists(email: string): Observable<any> {
    return this.http.get(`${this.API}/check/${email}`);
  }

  getFaceStatus(): Observable<any> {
    return this.http.get(`${this.API}/status`);
  }

  deleteFace(): Observable<any> {
    return this.http.delete(`${this.API}/delete`);
  }
}