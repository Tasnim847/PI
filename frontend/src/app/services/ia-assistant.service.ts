import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface ChatMessage {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  response: string;
  suggestions: string[];
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class IAAssistantService {
  private baseUrl = 'http://localhost:8081/api/ia-assistant';
  private sessionId: string | null = null;

  constructor(private http: HttpClient, private authService: AuthService) {}

  sendMessage(message: string): Observable<ChatResponse> {
    if (!this.sessionId) {
      this.sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    }
    
    const currentUser = this.authService.getCurrentUser();
    
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, {
      message: message,
      sessionId: this.sessionId,
      userFirstName: currentUser?.firstName || '',
      userLastName: currentUser?.lastName || ''
    });
  }

  resetConversation(): void {
    this.sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  }
}