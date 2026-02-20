import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { api } from '@/lib/api';
import type { PotholeListItem } from '@/lib/types';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { markerColor, confidencePercent, truncateId } from '@/lib/utils';

export function MapPage() {
  const [potholes, setPotholes] = useState<PotholeListItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.potholes
      .list({ limit: 1000 })
      .then((res) => setPotholes(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoading />;

  return (
    <div className="flex h-[calc(100vh-7rem)] flex-col">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Map View</h1>
        <div className="flex items-center gap-4 text-xs">
          <span className="flex items-center gap-1">
            <span className="inline-block h-3 w-3 rounded-full bg-red-500" /> Unverified
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-3 w-3 rounded-full bg-green-500" /> Verified
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-3 w-3 rounded-full bg-blue-500" /> Repaired
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block h-3 w-3 rounded-full bg-gray-500" /> False Positive
          </span>
        </div>
      </div>

      <div className="flex-1 overflow-hidden rounded-xl shadow-sm">
        <MapContainer
          center={[31.95, 35.91]}
          zoom={8}
          className="h-full w-full"
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {potholes.map((p) => (
            <CircleMarker
              key={p.id}
              center={[p.latitude, p.longitude]}
              radius={8}
              pathOptions={{
                color: markerColor(p.status),
                fillColor: markerColor(p.status),
                fillOpacity: 0.7,
              }}
            >
              <Popup>
                <div className="min-w-[160px]">
                  <p className="font-mono text-xs text-gray-500">{truncateId(p.id)}</p>
                  <p className="mt-1 text-sm font-medium">
                    Confidence: {confidencePercent(p.confidence)}
                  </p>
                  <div className="mt-1">
                    <StatusBadge status={p.status} />
                  </div>
                  <Link
                    to={`/potholes/${p.id}`}
                    className="mt-2 inline-block text-xs font-medium text-primary hover:underline"
                  >
                    View Details →
                  </Link>
                </div>
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
}
