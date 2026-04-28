// src/app/services/news.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';  // ← AJOUTER CET IMPORT
import { News, CreateNews, UpdateNews } from '../shared/models/news.model';

@Injectable({
    providedIn: 'root'
})
export class NewsService {

    private apiUrl = 'http://localhost:8083/api/v1/news';

    constructor(private http: HttpClient) { }

    private getAuthHeaders(): HttpHeaders {
        // ✅ Vérifier si window existe (évite l'erreur SSR)
        if (typeof window === 'undefined') {
            return new HttpHeaders({
                'Content-Type': 'application/json'
            });
        }
        
        const token = localStorage.getItem('token');
        console.log('🔑 Token dans headers:', token ? 'PRÉSENT' : 'ABSENT');
        return new HttpHeaders({
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        });
    }

    getAllNews(): Observable<News[]> {
        console.log('📡 Appel API: GET /api/v1/news');
        return this.http.get<News[]>(this.apiUrl, { headers: this.getAuthHeaders() }).pipe(
            map((response: any) => {
                console.log('🔍 Réponse brute:', response);
                // Si la réponse est un tableau, le retourner directement
                if (Array.isArray(response)) {
                    return response;
                }
                // Si la réponse a une propriété 'news' ou 'content'
                if (response && response.news) {
                    return response.news;
                }
                if (response && response.content) {
                    return response.content;
                }
                return response;
            })
        );
    }

    // POST - Créer une news
    createNews(news: CreateNews): Observable<any> {
        console.log('📡 POST appelé:', this.apiUrl);
        console.log('📦 Payload:', news);
        return this.http.post(`${this.apiUrl}`, news, { headers: this.getAuthHeaders() });
    }

    // PUT - Modifier une news
    updateNews(id: number, news: UpdateNews): Observable<any> {
        console.log(`📡 PUT appelé: ${this.apiUrl}/${id}`);
        return this.http.put(`${this.apiUrl}/${id}`, news, { headers: this.getAuthHeaders() });
    }

    // DELETE - Supprimer une news
    deleteNews(id: number): Observable<any> {
        console.log(`📡 DELETE appelé: ${this.apiUrl}/${id}`);
        return this.http.delete(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
    }

    // PATCH - Publier une news
    publishNews(id: number): Observable<any> {
        console.log(`📡 PATCH publish: ${this.apiUrl}/${id}/publish`);
        return this.http.patch(`${this.apiUrl}/${id}/publish`, {}, { headers: this.getAuthHeaders() });
    }

    // PATCH - Archiver une news
    archiveNews(id: number): Observable<any> {
        console.log(`📡 PATCH archive: ${this.apiUrl}/${id}/archive`);
        return this.http.patch(`${this.apiUrl}/${id}/archive`, {}, { headers: this.getAuthHeaders() });
    }

    // GET - Dernières news
    getLatestNews(limit: number = 5): Observable<News[]> {
        return this.http.get<News[]>(`${this.apiUrl}/latest?limit=${limit}`, { headers: this.getAuthHeaders() });
    }

    // GET - News publiées
    getPublishedNews(): Observable<News[]> {
        return this.http.get<News[]>(`${this.apiUrl}/published`, { headers: this.getAuthHeaders() });
    }

    // GET - Par catégorie
    getNewsByCategory(category: string): Observable<News[]> {
        return this.http.get<News[]>(`${this.apiUrl}/category/${category}`, { headers: this.getAuthHeaders() });
    }

    // GET - Par auteur
    getNewsByAuthor(author: string): Observable<News[]> {
        return this.http.get<News[]>(`${this.apiUrl}/author/${author}`, { headers: this.getAuthHeaders() });
    }

    // GET - Plus vues
    getMostViewedNews(limit: number = 5): Observable<News[]> {
        return this.http.get<News[]>(`${this.apiUrl}/most-viewed?limit=${limit}`, { headers: this.getAuthHeaders() });
    }

    // GET - Statistiques
    getStats(): Observable<{total: number, published: number}> {
        return this.http.get<{total: number, published: number}>(`${this.apiUrl}/stats/count`, { headers: this.getAuthHeaders() });
    }

    // POST - Upload image
    uploadImage(id: number, image: File): Observable<any> {
        const formData = new FormData();
        formData.append('image', image);
        
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
        const headers = new HttpHeaders({
            'Authorization': `Bearer ${token}`
        });
        
        return this.http.post(`${this.apiUrl}/${id}/upload-image`, formData, { headers });
    }

    // DELETE - Supprimer image
    deleteImage(id: number): Observable<any> {
        return this.http.delete(`${this.apiUrl}/${id}/image`, { headers: this.getAuthHeaders() });
    }
}