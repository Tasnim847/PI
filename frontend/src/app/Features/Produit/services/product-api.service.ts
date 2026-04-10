// services/product-api.service.ts
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { InsuranceProduct } from '../../../shared';

@Injectable({
  providedIn: 'root'
})
export class ProductApiService {

  private baseUrl = 'http://localhost:8081';

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  // ================= GET ALL ACTIVE =================
  getActiveProducts(): Observable<InsuranceProduct[]> {
    return this.http.get<InsuranceProduct[]>(
      `${this.baseUrl}/products/activeProducts`,
      { headers: this.getAuthHeaders() }
    ).pipe(map(products => this.transformProducts(products)));
  }

  // ================= GET ALL =================
  getAllProducts(): Observable<InsuranceProduct[]> {
    return this.http.get<InsuranceProduct[]>(
      `${this.baseUrl}/products/allProduct`,
      { headers: this.getAuthHeaders() }
    ).pipe(map(products => this.transformProducts(products)));
  }

  // ================= ADD PRODUCT =================
  addProduct(formData: FormData): Observable<InsuranceProduct> {
    return this.http.post<InsuranceProduct>(
      `${this.baseUrl}/products/addProduct`,
      formData,
      { headers: this.getAuthHeaders() }
    ).pipe(map(product => this.transformProduct(product)));
  }

  // ================= UPDATE PRODUCT =================
  updateProduct(formData: FormData): Observable<string> {
    return this.http.put(
      `${this.baseUrl}/products/updateProduct`,
      formData,
      {
        headers: this.getAuthHeaders(),
        responseType: 'text'
      }
    );
  }

  // ================= DELETE PRODUCT =================
  deleteProduct(id: number): Observable<string> {
    return this.http.delete(
      `${this.baseUrl}/products/deleteProduct/${id}`,
      {
        headers: this.getAuthHeaders(),
        responseType: 'text'
      }
    );
  }

  // ================= ACTIVATE =================
  activateProduct(id: number): Observable<string> {
    return this.http.put(
      `${this.baseUrl}/products/activate/${id}`,
      {},
      {
        headers: this.getAuthHeaders(),
        responseType: 'text'
      }
    );
  }

  // ================= DEACTIVATE =================
  deactivateProduct(id: number): Observable<string> {
    return this.http.put(
      `${this.baseUrl}/products/deactivate/${id}`,
      {},
      {
        headers: this.getAuthHeaders(),
        responseType: 'text'
      }
    );
  }

  // ================= TRANSFORM PRODUCT =================
  private transformProduct(product: any): InsuranceProduct {
    let displayImageUrl = 'assets/images/default-product.jpg';
    
    if (product.imageUrl) {
      displayImageUrl = this.transformToAccessibleUrl(product.imageUrl);
    }
    
    return {
      productId: product.productId,
      name: product.name,
      description: product.description,
      basePrice: product.basePrice,
      productType: product.productType,
      status: product.status,
      otherType: product.otherType,
      imageUrl: product.imageUrl,
      displayImageUrl: displayImageUrl,
      price: product.basePrice
    };
  }

  private transformProducts(products: any[]): InsuranceProduct[] {
    if (!products) return [];
    return products.map(product => this.transformProduct(product));
  }

  // ================= IMAGE URL TRANSFORM =================
  private transformToAccessibleUrl(storedPath: string): string {
    if (!storedPath) return 'assets/images/default-product.jpg';
    
    if (storedPath.startsWith('http://') || storedPath.startsWith('https://')) {
      return storedPath;
    }
    
    if (storedPath.match(/^[A-Za-z]:\\/)) {
      let filename = storedPath.replace(/\\/g, '/').split('/').pop();
      return `${this.baseUrl}/products/images/${filename}`;
    }
    
    if (storedPath.includes('uploads') || storedPath.includes('/')) {
      let filename = storedPath.replace(/\\/g, '/').split('/').pop();
      return `${this.baseUrl}/products/images/${filename}`;
    }
    
    return `${this.baseUrl}/products/images/${storedPath}`;
  }

  // ================= AUTH HEADERS =================
  private getAuthHeaders(): HttpHeaders {
    let headers = new HttpHeaders();

    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('token');
      if (token) {
        headers = headers.set('Authorization', `Bearer ${token}`);
      }
    }

    return headers;
  }
}