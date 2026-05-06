// src/app/services/news.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { News, CreateNews, UpdateNews } from '../shared/models/news.model';

@Injectable({ providedIn: 'root' })
export class NewsService {

  // ✅ URL DIRECTE - SANS API_CONFIG
  private apiUrl = 'http://localhost:8083/api/v1/news';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    if (typeof window === 'undefined') {
      return new HttpHeaders({ 'Content-Type': 'application/json' });
    }
    const token = localStorage.getItem('token');
    console.log('🔑 Token:', token ? 'PRÉSENT' : 'ABSENT');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getAllNews(): Observable<News[]> {
    console.log('📡 GET:', this.apiUrl);
    return this.http.get<News[]>(this.apiUrl, {
      headers: this.getAuthHeaders()
    }).pipe(
      map((res: any) => {
        if (Array.isArray(res)) return res;
        if (res?.content) return res.content;
        if (res?.news) return res.news;
        return res;
      })
    );
  }

  getNewsById(id: number): Observable<News> {
    return this.http.get<News>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  createNews(news: CreateNews): Observable<News> {
    console.log('📡 POST:', this.apiUrl);
    console.log('📦 Payload:', news);
    return this.http.post<News>(this.apiUrl, news, {
      headers: this.getAuthHeaders()
    });
  }

  updateNews(id: number, news: UpdateNews): Observable<News> {
    return this.http.put<News>(`${this.apiUrl}/${id}`, news, {
      headers: this.getAuthHeaders()
    });
  }

  deleteNews(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  publishNews(id: number): Observable<News> {
    return this.http.patch<News>(
      `${this.apiUrl}/${id}/publish`, {},
      { headers: this.getAuthHeaders() }
    );
  }

  archiveNews(id: number): Observable<News> {
    return this.http.patch<News>(
      `${this.apiUrl}/${id}/archive`, {},
      { headers: this.getAuthHeaders() }
    );
  }

  searchNews(keyword: string): Observable<News[]> {
    const params = new HttpParams().set('keyword', keyword);
    return this.http.get<News[]>(`${this.apiUrl}/search`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getPublishedNews(): Observable<News[]> {
    return this.http.get<News[]>(`${this.apiUrl}/published`, {
      headers: this.getAuthHeaders()
    });
  }

  getLatestNews(limit: number = 5): Observable<News[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<News[]>(`${this.apiUrl}/latest`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getMostViewedNews(limit: number = 5): Observable<News[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<News[]>(`${this.apiUrl}/most-viewed`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getNewsByCategory(category: string): Observable<News[]> {
    return this.http.get<News[]>(`${this.apiUrl}/category/${category}`, {
      headers: this.getAuthHeaders()
    });
  }

  getNewsByAuthor(author: string): Observable<News[]> {
    return this.http.get<News[]>(`${this.apiUrl}/author/${author}`, {
      headers: this.getAuthHeaders()
    });
  }

  getPagedNews(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'publishDate')
      .set('direction', 'desc');
    return this.http.get(`${this.apiUrl}/paged`, {
      headers: this.getAuthHeaders(),
      params
    });
  }

  getStats(): Observable<{ total: number; published: number }> {
    return forkJoin({
      total: this.http.get<number>(`${this.apiUrl}/stats/count`, {
        headers: this.getAuthHeaders()
      }),
      published: this.http.get<number>(`${this.apiUrl}/stats/published`, {
        headers: this.getAuthHeaders()
      })
    });
  }

  uploadImage(id: number, image: File): Observable<{ message: string; url: string }> {
    const formData = new FormData();
    formData.append('image', image);
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
    return this.http.post<{ message: string; url: string }>(
      `${this.apiUrl}/${id}/upload-image`,
      formData,
      { headers }
    );
  }

  deleteImage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/image`, {
      headers: this.getAuthHeaders()
    });
  }
}