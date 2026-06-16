import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAdmin()) {
    return true;
  }
  // Admin-only console: a signed-in non-admin has no destination, so sign them
  // out before bouncing everyone here to the login screen.
  if (auth.isLoggedIn()) {
    auth.logout();
  }
  return router.createUrlTree(["/login"]);
};
