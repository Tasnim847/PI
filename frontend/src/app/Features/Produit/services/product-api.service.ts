import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class ProductApiService {

  private baseUrl = 'http://localhost:8081';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  getActiveProducts(): Observable<any[]> {
    let headers = new HttpHeaders().set('Content-Type', 'application/json');
    
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('token');
      console.log('Token présent:', !!token);
      
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
        console.log('Headers configurés avec token');
      }
    }
    
    return this.http.get<any[]>(`${this.baseUrl}/products/activeProducts`, { headers }).pipe(
      map(products => {
        console.log('Produits reçus du backend:', products);
        
        return products.map(product => {
          let displayImageUrl = 'assets/images/default-product.jpg';
          
          if (product.imageUrl) {
            console.log('Chemin original de l\'image:', product.imageUrl);
            
            // Utiliser le chemin original tel quel
            // Si c'est un chemin absolu Windows comme C:\Users\...\image.png
            // ou un chemin relatif comme uploads/produits/image.png
            
            displayImageUrl = this.transformToAccessibleUrl(product.imageUrl);
            console.log('URL accessible:', displayImageUrl);
          }
          
          return {
            ...product,
            displayImageUrl: displayImageUrl,
            imageUrl: displayImageUrl
          };
        });
      })
    );
  }
  
  /**
   * Transforme le chemin stocké en URL accessible par le frontend
   */
  private transformToAccessibleUrl(storedPath: string): string {
    if (!storedPath) return 'assets/images/default-product.jpg';
    
    // Si c'est déjà une URL http, la retourner directement
    if (storedPath.startsWith('http://') || storedPath.startsWith('https://')) {
      return storedPath;
    }
    
    // Si c'est un chemin absolu Windows (C:\...)
    if (storedPath.match(/^[A-Za-z]:\\/)) {
      // Option 1: Utiliser l'endpoint backend pour servir le fichier
      // Extraire le nom du fichier
      let filename = storedPath.replace(/\\/g, '/').split('/').pop();
      return `${this.baseUrl}/products/images/${filename}`;
    }
    
    // Si c'est un chemin relatif (uploads/produits/image.png)
    if (storedPath.includes('uploads') || storedPath.includes('/')) {
      // Extraire le nom du fichier
      let filename = storedPath.replace(/\\/g, '/').split('/').pop();
      return `${this.baseUrl}/products/images/${filename}`;
    }
    
    // Sinon, considérer que c'est directement le nom du fichier
    return `${this.baseUrl}/products/images/${storedPath}`;
  }
  
  getImageUrl(imagePath: string): string {
    return this.transformToAccessibleUrl(imagePath);
  }
}