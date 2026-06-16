import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import {
  LucideArmchair,
  LucideEye,
  LucideEyeOff,
  LucideFilm,
  LucideLoader,
  LucideLock,
  LucidePopcorn,
  LucideStar,
  LucideTicket,
  LucideUser
} from "@lucide/angular";
import { AuthService } from "../../../core/services/auth.service";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    LucideArmchair,
    LucideEye,
    LucideEyeOff,
    LucideFilm,
    LucideLoader,
    LucideLock,
    LucidePopcorn,
    LucideStar,
    LucideTicket,
    LucideUser
  ],
  templateUrl: "./login.html",
  styleUrl: "./login.css"
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  username = "";
  password = "";
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  submit(): void {
    if (!this.username || !this.password) {
      this.error.set("Please enter your username and password");
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        // Admin-only console — non-admin accounts have no destination here.
        if (this.auth.isAdmin()) {
          this.router.navigate(["/manage-movies"]);
        } else {
          this.auth.logout();
          this.error.set("This console is for theater admins only.");
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? "Login failed");
        this.loading.set(false);
      }
    });
  }
}
