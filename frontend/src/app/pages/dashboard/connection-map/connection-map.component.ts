import { Component, AfterViewInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule, isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-connection-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './connection-map.component.html',
  styleUrls: ['./connection-map.component.css']
})
export class ConnectionMapComponent implements AfterViewInit {
  locations: any[] = [];
  loading = true;
  error: string | null = null;
  private map: any;
  private L: any;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngAfterViewInit() {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initMapAndLoad(), 300);
    } else {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private async initMapAndLoad() {
    try {
      this.L = await import('leaflet');
      const L = this.L;

      // ✅ Fix icônes Leaflet
      delete (L.Icon.Default.prototype as any)._getIconUrl;
      L.Icon.Default.mergeOptions({
        iconUrl: 'assets/marker-icon.png',
        iconRetinaUrl: 'assets/marker-icon-2x.png',
        shadowUrl: 'assets/marker-shadow.png',
      });

      // ✅ Vue toute la Tunisie
      this.map = L.map('connectionMap', {
        center: [33.8869, 9.5375], // ✅ Centre géographique de la Tunisie
        zoom: 6,                    // ✅ Zoom pour voir toute la Tunisie
        zoomControl: true,
      });

      // ✅ Tuiles OpenStreetMap
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 18,
      }).addTo(this.map);

      // ✅ Fix taille carte
      setTimeout(() => this.map.invalidateSize(), 200);

      this.loadLocations();

    } catch (err) {
      console.error('Map init error:', err);
      this.error = 'Failed to load map';
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  private loadLocations() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    this.http.get<any[]>('http://localhost:8083/api/admin/connection-locations', { headers })
      .subscribe({
        next: (data) => {
          console.log('✅ Locations received:', data.length, 'items');
          console.log('📍 Data:', data);
          this.locations = data;
          this.loading = false;
          this.cdr.detectChanges();

          setTimeout(() => {
            this.map.invalidateSize();
            if (data.length > 0) {
              this.addMarkers(data);
            }
          }, 100);
        },
        error: (err) => {
          console.error('❌ Status:', err.status);
          console.error('❌ Error:', err.error);

          if (err.status === 401) this.error = 'Not authorized (401)';
          else if (err.status === 403) this.error = 'Forbidden (403)';
          else if (err.status === 404) this.error = 'Endpoint not found (404)';
          else this.error = `Error ${err.status}: ${err.message}`;

          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  private addMarkers(locations: any[]) {
    const L = this.L;
    const validLocations = locations.filter(loc => loc.lat && loc.lon);
    console.log('📍 Valid markers:', validLocations.length);

    const colors = [
      '#667eea', '#e74c3c', '#2ecc71', '#f39c12',
      '#9b59b6', '#1abc9c', '#e67e22', '#3498db',
      '#e91e63', '#00bcd4', '#ff5722', '#607d8b'
    ];

    // ✅ Grouper par position identique
    const grouped: { [key: string]: any[] } = {};
    validLocations.forEach(loc => {
      const key = `${loc.lat.toFixed(4)}_${loc.lon.toFixed(4)}`;
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(loc);
    });

    let globalIndex = 0;

    // ✅ Pour chaque groupe, décaler les marqueurs en spirale
    Object.values(grouped).forEach((group: any[]) => {
      group.forEach((loc, indexInGroup) => {
        const color = colors[globalIndex % colors.length];
        globalIndex++;

        // ✅ Décaler si même position
        const offset = 0.015; // offset plus grand pour zoom Tunisie
        const angle = indexInGroup * (Math.PI * 2 / Math.max(group.length, 8));
        const lat = indexInGroup === 0 ? loc.lat :
          loc.lat + offset * Math.cos(angle);
        const lon = indexInGroup === 0 ? loc.lon :
          loc.lon + offset * Math.sin(angle);

        // ✅ Icône pin personnalisée
        const icon = L.divIcon({
          html: `
            <div style="
              position: relative;
              width: 32px;
              height: 32px;
            ">
              <div style="
                background: ${color};
                width: 32px;
                height: 32px;
                border-radius: 50% 50% 50% 0;
                transform: rotate(-45deg);
                border: 3px solid white;
                box-shadow: 0 3px 10px rgba(0,0,0,0.4);
              "></div>
            </div>
          `,
          className: '',
          iconSize: [32, 32],
          iconAnchor: [16, 32],
          popupAnchor: [0, -35]
        });

        // ✅ Popup stylisé
        const formattedDate = loc.date ?
          new Date(loc.date).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          }) : 'Unknown date';

        const popup = `
          <div style="
            min-width: 220px;
            font-family: 'Segoe UI', sans-serif;
            line-height: 1.8;
            border-radius: 10px;
            overflow: hidden;
          ">
            <div style="
              background: linear-gradient(135deg, ${color}, #764ba2);
              color: white;
              padding: 10px 14px;
              margin: -13px -20px 12px -20px;
              font-size: 13px;
              font-weight: bold;
            ">
              👤 ${loc.email}
            </div>
            <div style="font-size: 13px; color: #2c3e50; margin-bottom: 4px;">
              📍 <b>${loc.city}</b>, ${loc.country}
            </div>
            <div style="font-size: 12px; color: #95a5a6;">
              🕐 ${formattedDate}
            </div>
            ${group.length > 1 ? `
              <div style="
                margin-top: 8px;
                padding: 5px 10px;
                background: #fff3cd;
                border-left: 3px solid #f39c12;
                border-radius: 4px;
                font-size: 11px;
                color: #d35400;
              ">
                👥 ${group.length} utilisateurs depuis cette ville
              </div>
            ` : ''}
          </div>
        `;

        L.marker([lat, lon], { icon })
          .addTo(this.map)
          .bindPopup(popup, { maxWidth: 280 });
      });
    });

    // ✅ Adapter le zoom pour montrer tous les marqueurs
    if (validLocations.length > 0) {
      const bounds = L.latLngBounds(
        validLocations.map((loc: any) => [loc.lat, loc.lon])
      );

      // ✅ Vérifier si tous les points sont en Tunisie
      const allInTunisia = validLocations.every((loc: any) =>
        loc.country === 'Tunisia' || loc.country === 'Tunisie'
      );

      if (allInTunisia) {
        // ✅ Vue complète de la Tunisie
        this.map.fitBounds(bounds, {
          padding: [60, 60],
          maxZoom: 9  // ✅ Ne pas trop zoomer pour voir toute la Tunisie
        });
      } else {
        // Vue monde si utilisateurs internationaux
        this.map.fitBounds(bounds, {
          padding: [50, 50],
          maxZoom: 10
        });
      }
    }
  }
}
