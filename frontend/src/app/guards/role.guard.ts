import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const roleGuard: CanActivateFn = (route, state) => {

  const router = inject(Router);
  const role = localStorage.getItem('role');

  const allowedRoles = route.data['roles'];

  if (!role || !allowedRoles.includes(role)) {
    router.navigate(['/public/home']);
    return false;
  }

  return true;
};
