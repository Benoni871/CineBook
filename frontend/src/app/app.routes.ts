import { Routes } from "@angular/router";
import { adminGuard } from "./core/guards/admin.guard";
import { authGuard } from "./core/guards/auth.guard";
import { guestGuard } from "./core/guards/guest.guard";

export const routes: Routes = [
  {
    path: "login",
    canActivate: [guestGuard],
    loadComponent: () =>
      import("./features/auth/login/login").then((m) => m.LoginComponent)
  },
  {
    path: "register",
    canActivate: [guestGuard],
    loadComponent: () =>
      import("./features/auth/register/register").then((m) => m.RegisterComponent)
  },
  {
    path: "manage-movies",
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import("./features/admin/manage-movies/manage-movies").then((m) => m.ManageMoviesComponent)
  },
  {
    path: "manage-shows",
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import("./features/admin/manage-shows/manage-shows").then((m) => m.ManageShowsComponent)
  },
  {
    path: "manage-bookings",
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import("./features/admin/manage-bookings/manage-bookings").then(
        (m) => m.ManageBookingsComponent
      )
  },
  {
    path: "analytics",
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import("./features/admin/analytics/analytics").then((m) => m.AnalyticsComponent)
  },
  // Admin-only console: the landing page is the movie manager. The admin guard on
  // that route handles anonymous/non-admin callers (sent to /login).
  { path: "", redirectTo: "manage-movies", pathMatch: "full" },
  { path: "**", redirectTo: "manage-movies" }
];
