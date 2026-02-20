import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { Download, Trash2, CheckCircle, Wrench, XCircle } from 'lucide-react';
import { api } from '@/lib/api';
import { useToast } from '@/contexts/ToastContext';
import type { PotholeListItem, PaginationMeta } from '@/lib/types';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { formatDate, truncateId, confidencePercent } from '@/lib/utils';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export function PotholeListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { addToast } = useToast();
  const [data, setData] = useState<PotholeListItem[]>([]);
  const [pagination, setPagination] = useState<PaginationMeta | null>(null);
  const [loading, setLoading] = useState(true);
   const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkSaving, setBulkSaving] = useState(false);
  const [confirmBulk, setConfirmBulk] = useState<{ action: string; label: string } | null>(null);

  const page = Number(searchParams.get('page') || '1');
  const status = searchParams.get('status') || '';
  const severity = searchParams.get('severity') || '';

  const fetchData = useCallback(async () => {
    setLoading(true);
    setSelected(new Set());
    try {
      const res = await api.potholes.list({
        page,
        limit: 10,
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

  useEffect(() => { fetchData(); }, [fetchData]);

  const updateFilter = (key: string, value: string) => {
    const params = new URLSearchParams(searchParams);
    if (value) params.set(key, value); else params.delete(key);
    params.set('page', '1');
    setSearchParams(params);
  };

  const toggleSelect = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    if (selected.size === data.length) setSelected(new Set());
    else setSelected(new Set(data.map((p) => p.id)));
  };

  const handleBulkAction = async () => {
    if (!confirmBulk) return;
    setBulkSaving(true);
    try {
      const result = await api.potholes.bulkAction({ ids: [...selected], action: confirmBulk.action });
      addToast(`${confirmBulk.label}: ${result.affected} potholes updated`, undefined, 'success');
      setSelected(new Set());
      fetchData();
    } catch {
      addToast('Bulk action failed', undefined, 'error');
    } finally {
      setBulkSaving(false);
      setConfirmBulk(null);
    }
  };

  const handleExport = () => {
    api.potholes.exportCsv({ status: status || undefined, severity: severity || undefined });
  };

  return (
    <>
      <PageHeader title="Pothole Management">
        <button
          onClick={handleExport}
          className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          <Download className="h-4 w-4" /> Export CSV
        </button>
      </PageHeader>

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

      {selected.size > 0 && (
        <div className="mb-4 flex flex-wrap items-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3">
          <span className="text-sm font-medium text-blue-700">{selected.size} selected</span>
          <div className="ml-auto flex gap-2">
            <button
              onClick={() => setConfirmBulk({ action: 'verify', label: 'Verified' })}
              disabled={bulkSaving}
              className="inline-flex items-center gap-1 rounded-lg bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
            >
              <CheckCircle className="h-3.5 w-3.5" /> Verify
            </button>
            <button
              onClick={() => setConfirmBulk({ action: 'repair', label: 'Repaired' })}
              disabled={bulkSaving}
              className="inline-flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              <Wrench className="h-3.5 w-3.5" /> Repair
            </button>
            <button
              onClick={() => setConfirmBulk({ action: 'false_positive', label: 'False Positive' })}
              disabled={bulkSaving}
              className="inline-flex items-center gap-1 rounded-lg bg-gray-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-gray-700 disabled:opacity-50"
            >
              <XCircle className="h-3.5 w-3.5" /> False Positive
            </button>
            <button
              onClick={() => setConfirmBulk({ action: 'delete', label: 'Deleted' })}
              disabled={bulkSaving}
              className="inline-flex items-center gap-1 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
            >
              <Trash2 className="h-3.5 w-3.5" /> Delete
            </button>
            <button
              onClick={() => setSelected(new Set())}
              className="rounded-lg px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-100"
            >
              Clear
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <PageLoading />
      ) : (
        <div className="rounded-xl bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3">
                    <input
                      type="checkbox"
                      checked={selected.size === data.length && data.length > 0}
                      onChange={toggleAll}
                      className="rounded"
                    />
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Detected</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Location</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Confidence</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Severity</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.map((p) => (
                  <tr
                    key={p.id}
                    className={`hover:bg-gray-50 ${selected.has(p.id) ? 'bg-blue-50' : ''}`}
                  >
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        checked={selected.has(p.id)}
                        onChange={() => toggleSelect(p.id)}
                        className="rounded"
                      />
                    </td>
                    <td
                      className="cursor-pointer px-4 py-3 font-mono text-sm text-gray-700"
                      onClick={() => navigate(`/potholes/${p.id}`)}
                    >
                      {truncateId(p.id)}
                    </td>
                    <td className="cursor-pointer px-4 py-3 text-sm text-gray-600" onClick={() => navigate(`/potholes/${p.id}`)}>
                      {formatDate(p.detected_at)}
                    </td>
                    <td className="cursor-pointer px-4 py-3 text-sm text-gray-600" onClick={() => navigate(`/potholes/${p.id}`)}>
                      {p.latitude.toFixed(4)}, {p.longitude.toFixed(4)}
                    </td>
                    <td className="cursor-pointer px-4 py-3 text-sm text-gray-600" onClick={() => navigate(`/potholes/${p.id}`)}>
                      {confidencePercent(p.confidence)}
                    </td>
                    <td className="cursor-pointer px-4 py-3" onClick={() => navigate(`/potholes/${p.id}`)}>
                      <StatusBadge status={p.status} />
                    </td>
                    <td className="cursor-pointer px-4 py-3 text-sm capitalize text-gray-600" onClick={() => navigate(`/potholes/${p.id}`)}>
                      {p.severity}
                    </td>
                  </tr>
                ))}
                {data.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-12 text-center text-sm text-gray-500">
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
                {Math.min(pagination.page * pagination.limit, pagination.total)} of {pagination.total}
              </p>
              <div className="flex gap-2">
                <button
                  disabled={pagination.page <= 1}
                  onClick={() => { const p = new URLSearchParams(searchParams); p.set('page', String(pagination.page - 1)); setSearchParams(p); }}
                  className="inline-flex items-center rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40"
                >
                  <ChevronLeft className="h-4 w-4" /> Prev
                </button>
                <button
                  disabled={pagination.page >= pagination.totalPages}
                  onClick={() => { const p = new URLSearchParams(searchParams); p.set('page', String(pagination.page + 1)); setSearchParams(p); }}
                  className="inline-flex items-center rounded-lg border px-3 py-1.5 text-sm disabled:opacity-40"
                >
                  Next <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <ConfirmDialog
        open={!!confirmBulk}
        title={`Bulk ${confirmBulk?.label}`}
        message={`Apply "${confirmBulk?.label}" to ${selected.size} selected potholes?`}
        onConfirm={handleBulkAction}
        onCancel={() => setConfirmBulk(null)}
      />
    </>
  );
}
