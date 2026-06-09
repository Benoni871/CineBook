import { Component, inject } from "@angular/core";
import { LucideFilm, LucideTicket, LucideUser } from "@lucide/angular";
import { AuthService } from "../../core/services/auth.service";

@Component({
  selector: "app-home",
  standalone: true,
  imports: [LucideFilm, LucideTicket, LucideUser],
  templateUrl: "./home.html",
  styleUrl: "./home.css"
})
export class HomeComponent {
  readonly auth = inject(AuthService);
}
