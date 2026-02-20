import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { MapContainer, TileLayer, CircleMarker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { ArrowLeft, Trash2 } from 'lucide-react';
import { api } from '@/lib/api';
import type { PotholeDetail } from '@/lib/types';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  formatDateTime,
  confidencePercent,
  markerColor,
  statusLabel,
} from '@/lib/utils';

export function PotholeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [pothole, setPothole] = useState<PotholeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [editStatus, setEditStatus] = useState('');
  const [editSeverity, setEditSeverity] = useState('');

  useEffect(() => {
    if (!id) return;
    api.potholes
      .get(id)
      .then((p) => {
        setPothole(p);
        setEditStatus(p.status);
        setEditSeverity(p.severity);
      })
      .finally(() => setLoading(false));
  }, [id]);

  const handleSave = async () => {
    if (!id) return;
    setSaving(true);
    try {
      const updated = await api.potholes.update(id, {
        status: editStatus,
        severity: editSeverity,
      });
      setPothole(updated);
    } finally {
      setSaving(false);
    }
  };

  const quickAction = async (status: string) => {
    if (!id) return;
    setSaving(true);
    try {
      const updated = await api.potholes.update(id, { status });
      setPothole(updated);
      setEditStatus(updated.status);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    await api.potholes.delete(id);
    navigate('/potholes');
  };

  if (loading) return <PageLoading />;
  if (!pothole) return <p className="text-center text-gray-500">Pothole not found.</p>;

  return (
    <>
      <button
        onClick={() => navigate('/potholes')}
        className="mb-4 inline-flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900"
      >
        <ArrowLeft className="h-4 w-4" /> Back to List
      </button>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          {pothole.image_url && (
            <div className="overflow-hidden rounded-xl bg-white shadow-sm">
              <img
                src={pothole.image_url}
                alt="Pothole"
                className="h-64 w-full object-cover"
              />
            </div>
          )}

          <div className="h-64 overflow-hidden rounded-xl shadow-sm">
            <MapContainer
              center={[pothole.latitude, pothole.longitude]}
              zoom={16}
              scrollWheelZoom={false}
              className="h-full w-full"
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <CircleMarker
                center={[pothole.latitude, pothole.longitude]}
                radius={10}
                pathOptions={{
                  color: markerColor(pothole.status),
                  fillColor: markerColor(pothole.status),
                  fillOpacity: 0.8,
                }}
              />
            </MapContainer>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              disabled={saving || pothole.status === 'verified'}
              onClick={() => quickAction('verified')}
              className="rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-40"
            >
              Verify
            </button>
            <button
              disabled={saving || pothole.status === 'repaired'}
              onClick={() => quickAction('repaired')}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-40"
            >
              Mark Repaired
            </button>
            <button
              disabled={saving || pothole.status === 'false_positive'}
              onClick={() => quickAction('false_positive')}
              className="rounded-lg bg-gray-600 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-40"
            >
              False Positive
            </button>
            <button
              onClick={() => setDeleteOpen(true)}
              className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              <Trash2 className="inline h-4 w-4" /> Delete
            </button>
          </div>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-sm">
          <h2 className="mb-6 text-lg font-semibold text-gray-900">Details</h2>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-500">Status</label>
              <select
                value={editStatus}
                onChange={(e) => setEditStatus(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              >
                <option value="unverified">Unverified</option>
                <option value="verified">Verified</option>
                <option value="repaired">Repaired</option>
                <option value="false_positive">False Positive</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-500">Severity</label>
              <select
                value={editSeverity}
                onChange={(e) => setEditSeverity(e.target.value)}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              >
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
              </select>
            </div>

            <button
              onClick={handleSave}
              disabled={saving}
              className="w-full rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-60"
            >
              {saving ? 'Saving…' : 'Save Changes'}
            </button>

            <hr className="my-4" />

            <dl className="space-y-3 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-500">Confidence</dt>
                <dd className="font-medium">{confidencePercent(pothole.confidence)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Detected</dt>
                <dd className="font-medium">{formatDateTime(pothole.detected_at)}</dd>
              </div>
              {pothole.repaired_at && (
                <div className="flex justify-between">
                  <dt className="text-gray-500">Repaired</dt>
                  <dd className="font-medium">{formatDateTime(pothole.repaired_at)}</dd>
                </div>
              )}
              <div className="flex justify-between">
                <dt className="text-gray-500">Confirmations</dt>
                <dd className="font-medium">{pothole.confirmationCount}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Status</dt>
                <dd className="font-medium">{statusLabel(pothole.status)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Coordinates</dt>
                <dd className="font-mono text-xs">
                  {pothole.latitude.toFixed(6)}, {pothole.longitude.toFixed(6)}
                </dd>
              </div>
            </dl>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={deleteOpen}
        title="Delete Pothole"
        message="Are you sure you want to delete this pothole record? This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteOpen(false)}
      />
    </>
  );
}
