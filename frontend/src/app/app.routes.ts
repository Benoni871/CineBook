import { Routes } from "@angular/router";
import { adminGuard } from "./core/guards/admin.guard";
import { authGuard } from "./core/guards/auth.guard";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () =>
      import("./features/auth/login/login").then((m) => m.LoginComponent)
  },
  {
    path: "register",
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
    path: "",
    canActivate: [authGuard],
    loadComponent: () => import("./features/user/home/home").then((m) => m.HomeComponent)
  },
  { path: "**", redirectTo: "" }
];
