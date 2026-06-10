export type Role = "USER" | "ADMIN";

export interface AuthUser {
  id: number;
  username: string;
  role: Role;
  theaterId: number | null;
  token: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface RegisterAdminRequest {
  username: string;
  password: string;
  theaterName: string;
  theaterLocation: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}
