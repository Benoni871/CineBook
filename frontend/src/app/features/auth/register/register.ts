import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import {
  LucideArmchair,
  LucideBuilding,
  LucideClapperboard,
  LucideEye,
  LucideEyeOff,
  LucideFilm,
  LucideLoader,
  LucideLock,
  LucideMapPin,
  LucidePopcorn,
  LucideSparkles,
  LucideStar,
  LucideUser
} from "@lucide/angular";
import { AuthService } from "../../../core/services/auth.service";

type Mode = "user" | "admin";

@Component({
  selector: "app-register",
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    LucideArmchair,
    LucideBuilding,
    LucideClapperboard,
    LucideEye,
    LucideEyeOff,
    LucideFilm,
    LucideLoader,
    LucideLock,
    LucideMapPin,
    LucidePopcorn,
    LucideSparkles,
    LucideStar,
    LucideUser
  ],
  templateUrl: "./register.html",
  styleUrl: "./register.css"
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly mode = signal<Mode>("user");
  username = "";
  password = "";
  theaterName = "";
  theaterLocation = "";
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  submit(): void {
    if (!this.username || !this.password) {
      this.error.set("Username and password are required");
      return;
    }
    if (this.mode() === "admin" && !this.theaterName) {
      this.error.set("Theater name is required for theater owners");
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const request$ =
      this.mode() === "admin"
        ? this.auth.registerAdmin({
            username: this.username,
            password: this.password,
            theaterName: this.theaterName,
            theaterLocation: this.theaterLocation
          })
        : this.auth.register({ username: this.username, password: this.password });

    request$.subscribe({
      next: () => {
        // Admin-only console — only theater-owner accounts can enter.
        if (this.auth.isAdmin()) {
          this.router.navigate(["/manage-movies"]);
        } else {
          this.auth.logout();
          this.error.set("This console is for theater admins only. Please register as a theater owner.");
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? "Registration failed");
        this.loading.set(false);
      }
    });
  }
}
