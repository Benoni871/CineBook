import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { NavbarComponent } from "./shared/navbar/navbar";
import { SidebarComponent } from "./shared/sidebar/sidebar";

@Component({
  selector: "app-root",
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  templateUrl: "./app.html",
  styleUrl: "./app.css"
})
export class AppComponent {}
