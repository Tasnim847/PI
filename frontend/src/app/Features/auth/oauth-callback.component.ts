import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="text-align:center;padding:2rem">
      <div *ngIf="!error; else errorBlock">
        <h2>Connexion en cours...</h2>
        <div class="spinner"></div>
      </div>
      <ng-template #errorBlock>
        <h2>Erreur de connexion</h2>
        <p>{{error}}</p>
        <button (click)="router.navigate([''])">Retour à la connexion</button>
      </ng-template>
    </div>
  `,
  styles: [`
    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #3498db;
      border-radius: 50%;
      width: 40px;
      height: 40px;
      animation: spin 1s linear infinite;
      margin: 20px auto;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `]
})
export class OauthCallbackComponent implements OnInit {
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object  // ADD THIS
  ) {}

  ngOnInit() {
    // Guard: only run in browser, not during SSR
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        this.error = 'Authentication failed: ' + error;
        setTimeout(() => this.router.navigate(['']), 3000);
      } else if (token) {
        // Save token first so getMe() can use it
        localStorage.setItem('token', token);

        this.authService.getMe().subscribe({
          next: (user) => {
            this.authService.saveSession(
              token, user.role, user.id,
              user.firstName, user.lastName, user.email
            );
            this.router.navigate(['/public/home']);
          },
          error: () => {
            // Token already saved, just navigate
            this.router.navigate(['/public/home']);
          }
        });
      } else {
        this.router.navigate(['']);
      }
    });
  }
}
