import { BrowserRouter, Routes, Route, Navigate } from 'react-router';
import { useEffect } from 'react';
import * as signalR from '@microsoft/signalr';
import { AuthProvider } from '@/contexts/AuthContext';
import { ToastProvider, useToast } from '@/contexts/ToastContext';
import { ProtectedRoute, DashboardLayout } from '@/layouts/DashboardLayout';
import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { PotholeListPage } from '@/pages/PotholeListPage';
import { PotholeDetailPage } from '@/pages/PotholeDetailPage';
import { MapPage } from '@/pages/MapPage';
import { VehicleListPage } from '@/pages/VehicleListPage';
import { UserListPage } from '@/pages/UserListPage';
import { SettingsPage } from '@/pages/SettingsPage';

function SignalRListener() {
  const { addToast } = useToast();

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;

    const connection = new signalR.HubConnectionBuilder()
      .withUrl('/hubs/potholes', {
        accessTokenFactory: () => localStorage.getItem('accessToken') ?? '',
      })
      .withAutomaticReconnect()
      .configureLogging(signalR.LogLevel.Warning)
      .build();

    connection.on('NewPothole', (data: { severity: string; confidence: number; latitude: number; longitude: number }) => {
      addToast(
        `New pothole detected`,
        `Severity: ${data.severity} · Confidence: ${Math.round(data.confidence * 100)}% · ${data.latitude.toFixed(4)}, ${data.longitude.toFixed(4)}`,
        'info',
      );
    });

    connection.start().catch(() => {/* silent fail in dev */});

    return () => { connection.stop(); };
  }, [addToast]);

  return null;
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
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
          <SignalRListener />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
