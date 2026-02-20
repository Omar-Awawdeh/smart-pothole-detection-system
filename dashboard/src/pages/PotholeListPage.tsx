import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { api } from '@/lib/api';
import type { PotholeListItem, PaginationMeta } from '@/lib/types';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { formatDate, truncateId, confidencePercent } from '@/lib/utils';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export function PotholeListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [data, setData] = useState<PotholeListItem[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);

  const page = Number(searchParams.get('page') || '1');
  const status = searchParams.get('status') || '';
  const severity = searchParams.get('severity') || '';

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.potholes.list({
        page,
        limit: 20,
        status: status || undefined,
        severity: severity || undefined,
        sortBy: 'detected_at',
        sortOrder: 'DESC',
      });
      setData(res.data);
      setPagination(res.pagination);
    } finally {
      setLoading(false);
    }
  }, [page, status, severity]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const updateFilter = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams);
    if (value) {
      params.set(key, value);
    } else {
      params.delete(key);
    }
    params.set('page', '1');
    setSearchParams(params);
  };

  return (
    <>
      <PageHeader title="Pothole Management" />

      <div className="mb-6 flex flex-wrap items-center gap-3 rounded-xl bg-white p-4 shadow-sm">
        <select
          value={status}
          onChange={(e) => updateFilter('status', e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          <option value="">All Statuses</option>
          <option value="unverified">Unverified</option>
          <option value="verified">Verified</option>
          <option value="repaired">Repaired</option>
          <option value="false_positive">False Positive</option>
        </select>

        <select
          value={severity}
          onChange={(e) => updateFilter('severity', e.target.value)}
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
        >
          <option value="">All Severities</option>
          <option value="high">High</option>
          <option value="medium">Medium</option>
          <option value="low">Low</option>
        </select>
      </div>

      {loading ? (
        <PageLoading />
      ) : (
        <div className="rounded-xl bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    ID
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    Detected
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    Location
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    Confidence
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    Status
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                    Severity
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.map((p) => (
                  <tr
                    key={p.id}
                    onClick={() => navigate(`/potholes/${p.id}`)}
                    className="cursor-pointer hover:bg-gray-50"
                  >
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      {truncateId(p.id)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {formatDate(p.detected_at)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {p.latitude.toFixed(4)}, {p.longitude.toFixed(4)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {confidencePercent(p.confidence)}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="px-4 py-3 text-sm capitalize text-gray-600">
                      {p.severity}
                    </td>
                  </tr>
                ))}
                {data.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-sm text-gray-500">
                      No potholes found matching your filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {pagination && pagination.totalPages > 1 && (
            <div className="flex items-center justify-between border-t px-4 py-3">
              <p className="text-sm text-gray-600">
                Showing {(pagination.page - 1) * pagination.limit + 1}–
                {Math.min(pagination.page * pagination.limit, pagination.total)} of{' '}
                {pagination.total}
              </p>
              <div className="flex gap-2">
                <button
                  disabled={pagination.page <= 1}
                  onClick={() => {
                    const params = new URLSearchParams(searchParams);
                    params.set('page', String(pagination.page - 1));
                    setSearchParams(params);
                  }}
                  className="inline-flex items-center rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </button>
                <button
                  disabled={pagination.page >= pagination.totalPages}
                  onClick={() => {
                    const params = new URLSearchParams(searchParams);
                    params.set('page', String(pagination.page + 1));
                    setSearchParams(params);
                  }}
                  className="inline-flex items-center rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40"
                >
                  Next <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </>
  );
}
