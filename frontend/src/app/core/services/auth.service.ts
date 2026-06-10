import { HttpClient } from "@angular/common/http";
import { Injectable, computed, signal } from "@angular/core";
import { Observable, tap } from "rxjs";
import { environment } from "../../../environments/environment";
import {
  AuthUser,
  LoginRequest,
  RegisterAdminRequest,
  RegisterRequest
} from "../models/user.model";

const STORAGE_KEY = "cinebook.user";

@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly base = `${environment.apiUrl}/auth`;

  /** Currently signed-in user, restored from localStorage on load. */
  readonly currentUser = signal<AuthUser | null>(this.restore());

  readonly isLoggedIn = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.role === "ADMIN");

  /** Current JWT, or null when signed out. Used by the auth interceptor. */
  readonly token = computed(() => this.currentUser()?.token ?? null);

  constructor(private http: HttpClient) {}

  register(body: RegisterRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.base}/register`, body)
      .pipe(tap((user) => this.persist(user)));
  }

  registerAdmin(body: RegisterAdminRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.base}/register-admin`, body)
      .pipe(tap((user) => this.persist(user)));
  }

  login(body: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.base}/login`, body)
      .pipe(tap((user) => this.persist(user)));
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.currentUser.set(null);
  }

  private persist(user: AuthUser): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private restore(): AuthUser | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
