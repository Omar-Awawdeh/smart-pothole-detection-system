import axios from 'axios';
import type {
  AuthResponse,
  AuthTokens,
  DailyStat,
  HeatmapPoint,
  LoginRequest,
  PaginatedResponse,
  PotholeDetail,
  PotholeListItem,
  PotholeListParams,
  PotholeUpdateRequest,
  StatsOverview,
  StatusStat,
  User,
  UserUpdateRequest,
  Vehicle,
  VehicleCreateRequest,
  VehicleStat,
  VehicleUpdateRequest,
} from './types';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
});

// ── Token helpers ────────────────────────────────────
function getAccessToken(): string | null {
  return localStorage.getItem('accessToken');
}

function getRefreshToken(): string | null {
  return localStorage.getItem('refreshToken');
}

export function setTokens(access: string, refresh: string) {
  localStorage.setItem('accessToken', access);
  localStorage.setItem('refreshToken', refresh);
}

export function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

// ── Request interceptor ──────────────────────────────
client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor (refresh on 401) ────────────
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => {
    if (error) p.reject(error);
    else if (token) p.resolve(token);
  });
  failedQueue = [];
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(client(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refresh = getRefreshToken();
      if (!refresh) {
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post<AuthTokens>('/api/auth/refresh', {
          refresh_token: refresh,
        });
        setTokens(data.access_token, data.refresh_token);
        processQueue(null, data.access_token);
        originalRequest.headers.Authorization = `Bearer ${data.access_token}`;
        return client(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

// ── API methods ──────────────────────────────────────
export const api = {
  auth: {
    login: (data: LoginRequest) =>
      client.post<AuthResponse>('/api/auth/login', data).then((r) => r.data),
    me: () => client.get<User>('/api/auth/me').then((r) => r.data),
    refresh: (refreshToken: string) =>
      client
        .post<AuthTokens>('/api/auth/refresh', { refresh_token: refreshToken })
        .then((r) => r.data),
  },

  potholes: {
    list: (params?: PotholeListParams) =>
      client
        .get<PaginatedResponse<PotholeListItem>>('/api/potholes', { params })
        .then((r) => r.data),
    get: (id: string) =>
      client.get<PotholeDetail>(`/api/potholes/${id}`).then((r) => r.data),
    update: (id: string, data: PotholeUpdateRequest) =>
      client.patch<PotholeDetail>(`/api/potholes/${id}`, data).then((r) => r.data),
    delete: (id: string) =>
      client.delete(`/api/potholes/${id}`).then(() => undefined),
  },

  vehicles: {
    list: () =>
      client.get<Vehicle[]>('/api/vehicles').then((r) => r.data),
    create: (data: VehicleCreateRequest) =>
      client.post<Vehicle>('/api/vehicles', data).then((r) => r.data),
    update: (id: string, data: VehicleUpdateRequest) =>
      client.patch<Vehicle>(`/api/vehicles/${id}`, data).then((r) => r.data),
    delete: (id: string) =>
      client.delete(`/api/vehicles/${id}`).then(() => undefined),
    potholes: (id: string, params?: PotholeListParams) =>
      client
        .get<PaginatedResponse<PotholeListItem>>(`/api/vehicles/${id}/potholes`, {
          params,
        })
        .then((r) => r.data),
  },

  users: {
    list: () => client.get<User[]>('/api/users').then((r) => r.data),
    update: (id: string, data: UserUpdateRequest) =>
      client.patch<User>(`/api/users/${id}`, data).then((r) => r.data),
    delete: (id: string) =>
      client.delete(`/api/users/${id}`).then(() => undefined),
  },

  stats: {
    overview: () =>
      client.get<StatsOverview>('/api/stats/overview').then((r) => r.data),
    daily: (days = 30) =>
      client
        .get<DailyStat[]>('/api/stats/daily', { params: { days } })
        .then((r) => r.data),
    byStatus: () =>
      client.get<StatusStat[]>('/api/stats/by-status').then((r) => r.data),
    byVehicle: () =>
      client.get<VehicleStat[]>('/api/stats/by-vehicle').then((r) => r.data),
    heatmap: () =>
      client.get<HeatmapPoint[]>('/api/stats/heatmap').then((r) => r.data),
  },
};
