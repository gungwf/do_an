import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AiChatService {
  // Gateway route that forwards to chatbot-service
  private apiUrl = 'http://localhost:8080/chat';

  constructor(private http: HttpClient) {}

  sendMessage(sessionId: string | null, message: string, metadata?: any): Observable<any> {
    const payload: any = { sessionId, message };
    if (metadata) payload.metadata = metadata;
    return this.http.post<any>(this.apiUrl, payload);
  }
}
