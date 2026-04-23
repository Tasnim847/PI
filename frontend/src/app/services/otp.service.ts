import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class OtpService {

  private API = 'http://localhost:8082/api/otp';

  constructor(private http: HttpClient) {}

  // envoyer OTP - gérer la réponse texte
  sendOtp(email: string): Observable<any> {
    // Spécifier que l'on attend du texte
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'text/plain, application/json'
    });

    return this.http.post(`${this.API}/send?email=${email}`, {}, {
      headers: headers,
      responseType: 'text' // ← Important : spécifier responseType 'text'
    });
  }

  // vérifier OTP + reset password
  verifyOtp(data: any): Observable<any> {
    return this.http.post(
      `${this.API}/verify?email=${data.email}&otp=${data.otp}&newPassword=${data.newPassword}`,
      {},
      { responseType: 'text' } // ← Spécifier responseType 'text'
    );
  }
}
