import { Component, computed, inject } from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { NavigationEnd, Router, RouterLink } from "@angular/router";
import { LucideFilm, LucideLogOut, LucideMapPin, LucideUser } from "@lucide/angular";
import { filter, map } from "rxjs";
import { AuthService } from "../../core/services/auth.service";
import { LocationService } from "../../core/services/location.service";

@Component({
  selector: "app-navbar",
  standalone: true,
  imports: [RouterLink, LucideFilm, LucideLogOut, LucideMapPin, LucideUser],
  templateUrl: "./navbar.html",
  styleUrl: "./navbar.css"
})
export class NavbarComponent {
  readonly auth = inject(AuthService);
  readonly location = inject(LocationService);
  private readonly router = inject(Router);

  /** Current URL, kept reactive so the auth-switch thumb can slide on navigation. */
  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url)
    ),
    { initialValue: this.router.url }
  );

  readonly onRegister = computed(() => this.currentUrl().startsWith("/register"));

  /** Login/register share a pre-auth context — hide theater chrome (e.g. location) there. */
  readonly onAuthPage = computed(
    () => this.currentUrl().startsWith("/login") || this.currentUrl().startsWith("/register")
  );

  logout(): void {
    this.auth.logout();
    this.router.navigate(["/login"]);
  }
}
