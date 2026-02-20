// ── Auth ──────────────────────────────────────────────
export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface AuthTokens {
  access_token: string;
  refresh_token: string;
}

export interface AuthResponse {
  user: User;
  tokens: AuthTokens;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// ── Pothole ──────────────────────────────────────────
export type PotholeStatus = 'unverified' | 'verified' | 'repaired' | 'false_positive';
export type PotholeSeverity = 'low' | 'medium' | 'high';

export interface PotholeListItem {
  id: string;
  latitude: number;
  longitude: number;
  confidence: number;
  image_url: string | null;
  status: string;
  severity: string;
  detected_at: string;
}

export interface PotholeDetail {
  id: string;
  latitude: number;
  longitude: number;
  confidence: number;
  image_url: string | null;
  status: string;
  severity: string;
  confirmationCount: number;
  detected_at: string;
  repaired_at: string | null;
  created_at: string;
  updated_at: string;
  vehicle_id: string;
}

export interface PotholeUpdateRequest {
  status?: string;
  severity?: string;
}

// ── Pagination ───────────────────────────────────────
export interface PaginationMeta {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: PaginationMeta;
}

// ── Vehicle ──────────────────────────────────────────
export interface Vehicle {
  id: string;
  user_id: string;
  name: string;
  serial_number: string;
  is_active: boolean;
  last_active_at: string | null;
  created_at: string;
}

export interface VehicleCreateRequest {
  name: string;
  serialNumber: string;
}

export interface VehicleUpdateRequest {
  name?: string;
  isActive?: boolean;
}

// ── User (admin) ─────────────────────────────────────
export interface UserCreateRequest {
  email: string;
  password: string;
  name: string;
  role?: string;
}

export interface UserUpdateRequest {
  name?: string;
  role?: string;
  email?: string;
}
export interface UpdateProfileRequest {
  name?: string;
  email?: string;
  currentPassword?: string;
  newPassword?: string;
}


// ── Stats ────────────────────────────────────────────
export interface StatsOverview {
  total: number;
  unverified: number;
  verified: number;
  repaired: number;
  falsePositive: number;
  todayCount: number;
}

export interface DailyStat {
  date: string;
  count: number;
}

export interface StatusStat {
  status: string;
  count: number;
}

export interface VehicleStat {
  vehicleId: string;
  vehicleName: string;
  count: number;
}

export interface HeatmapPoint {
  latitude: number;
  longitude: number;
  intensity: number;
}

// ── Query Params ─────────────────────────────────────
export interface PotholeListParams {
  page?: number;
  limit?: number;
  status?: string;
  severity?: string;
  vehicleId?: string;
  startDate?: string;
  endDate?: string;
  sortBy?: string;
  sortOrder?: string;
}
