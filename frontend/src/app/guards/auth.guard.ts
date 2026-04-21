// guards/auth.guard.ts
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  // ✅ Check if we're in browser environment
  if (typeof window === 'undefined' || typeof localStorage === 'undefined') {
    // During SSR, don't block rendering
    return true;
  }

  const token = localStorage.getItem('token');

  if (!token) {
    router.navigate(['/public/login']);
    return false;
  }

  return true;
};