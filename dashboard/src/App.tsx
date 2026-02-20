import { BrowserRouter, Routes, Route, Navigate } from 'react-router';
import { AuthProvider } from '@/contexts/AuthContext';
import { ProtectedRoute, DashboardLayout } from '@/layouts/DashboardLayout';
import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { PotholeListPage } from '@/pages/PotholeListPage';
import { PotholeDetailPage } from '@/pages/PotholeDetailPage';
import { MapPage } from '@/pages/MapPage';
import { VehicleListPage } from '@/pages/VehicleListPage';
import { UserListPage } from '@/pages/UserListPage';
import { SettingsPage } from '@/pages/SettingsPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<DashboardLayout />}>
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardPage />} />
              <Route path="potholes" element={<PotholeListPage />} />
              <Route path="potholes/:id" element={<PotholeDetailPage />} />
              <Route path="map" element={<MapPage />} />
              <Route path="vehicles" element={<VehicleListPage />} />
              <Route path="users" element={<UserListPage />} />
              <Route path="settings" element={<SettingsPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
