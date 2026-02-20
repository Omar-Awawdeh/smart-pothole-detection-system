import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router';
import { MapContainer, TileLayer, CircleMarker, Popup, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { Layers } from 'lucide-react';
import { api } from '@/lib/api';
import type { PotholeListItem, HeatmapPoint } from '@/lib/types';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { markerColor, confidencePercent, truncateId } from '@/lib/utils';

const PAGE_SIZE = 100;

interface MapViewportControllerProps {
  points: PotholeListItem[];
  selectedPoint: PotholeListItem | null;
}

function MapViewportController({ points, selectedPoint }: MapViewportControllerProps) {
  const map = useMap();

  useEffect(() => {
    if (points.length === 0) {
      return;
    }

    if (selectedPoint) {
      map.flyTo([selectedPoint.latitude, selectedPoint.longitude], Math.max(map.getZoom(), 15), {
        animate: true,
        duration: 0.35,
      });
      return;
    }

    const bounds = L.latLngBounds(points.map((p) => [p.latitude, p.longitude] as [number, number]));
    map.fitBounds(bounds.pad(0.2), { animate: true });
  }, [map, points, selectedPoint]);

  return null;
}

export function MapPage() {
  const [potholes, setPotholes] = useState<PotholeListItem[]>([]);
  const [heatmap, setHeatmap] = useState<HeatmapPoint[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showHeatmap, setShowHeatmap] = useState(false);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);

  const fetchAllPoints = useCallback(async () => {
    setLoading(true);
    try {
      let currentPage = 1;
      let totalItems = 0;
      let fetched: PotholeListItem[] = [];

      while (true) {
        const res = await api.potholes.list({
          page: currentPage,
          limit: PAGE_SIZE,
          sortBy: 'detected_at',
          sortOrder: 'DESC',
        });

        if (currentPage === 1) {
          totalItems = res.pagination.total;
        }

        fetched = [...fetched, ...res.data];

        if (fetched.length >= totalItems || res.data.length === 0) {
          break;
        }

        currentPage += 1;
      }

      setPotholes(fetched);
      setTotal(totalItems);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllPoints();
    api.stats.heatmap().then(setHeatmap);
  }, [fetchAllPoints]);

  const selectedPoint = selectedPointId
    ? potholes.find((point) => point.id === selectedPointId) ?? null
    : null;

  if (loading) return <PageLoading />;

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold text-gray-900">Map View</h1>
          <span className="text-sm text-gray-500">Showing {potholes.length} of {total} points</span>
        </div>
        <div className="flex flex-wrap items-center gap-3 lg:gap-4">
          <button
            onClick={() => setShowHeatmap((v) => !v)}
            className={`inline-flex items-center gap-2 rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors ${
              showHeatmap
                ? 'border-orange-400 bg-orange-50 text-orange-700'
                : 'border-gray-300 bg-white text-gray-600 hover:bg-gray-50'
            }`}
          >
            <Layers className="h-4 w-4" />
            Heatmap
          </button>
          <div className="flex flex-wrap items-center gap-3 text-xs">
            <span className="flex items-center gap-1"><span className="inline-block h-3 w-3 rounded-full bg-red-500" /> Unverified</span>
            <span className="flex items-center gap-1"><span className="inline-block h-3 w-3 rounded-full bg-green-500" /> Verified</span>
            <span className="flex items-center gap-1"><span className="inline-block h-3 w-3 rounded-full bg-blue-500" /> Repaired</span>
            <span className="flex items-center gap-1"><span className="inline-block h-3 w-3 rounded-full bg-gray-500" /> False Positive</span>
          </div>
        </div>
      </div>

      <div className="min-h-[320px] flex-1 overflow-hidden rounded-xl shadow-sm">
        <MapContainer center={[31.50, 74.35]} zoom={12} className="h-full w-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapViewportController points={potholes} selectedPoint={selectedPoint} />

          {showHeatmap && heatmap.map((h, i) => (
            <CircleMarker
              key={`heat-${i}`}
              center={[h.latitude, h.longitude]}
              radius={30}
              pathOptions={{
                color: 'transparent',
                fillColor: h.intensity > 5 ? '#EF4444' : h.intensity > 2 ? '#F97316' : '#EAB308',
                fillOpacity: Math.min(0.15 + h.intensity * 0.04, 0.5),
              }}
            />
          ))}

          {potholes.map((p) => (
            <CircleMarker
              key={p.id}
              center={[p.latitude, p.longitude]}
              radius={8}
              eventHandlers={{
                click: () => setSelectedPointId(p.id),
              }}
              pathOptions={{
                color: markerColor(p.status),
                fillColor: markerColor(p.status),
                fillOpacity: 0.7,
              }}
            >
              <Popup minWidth={200}>
                <div className="min-w-[200px]">
                  {p.image_url && (
                    <img src={p.image_url} alt="Pothole" loading="lazy" className="mb-2 h-28 w-full rounded bg-gray-100 object-contain" />
                  )}
                  <p className="font-mono text-xs text-gray-500">{truncateId(p.id)}</p>
                  <p className="mt-1 text-sm font-medium">Confidence: {confidencePercent(p.confidence)}</p>
                  <div className="mt-1"><StatusBadge status={p.status} /></div>
                  <Link to={`/potholes/${p.id}`} className="mt-2 inline-block text-xs font-medium text-primary hover:underline">
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
