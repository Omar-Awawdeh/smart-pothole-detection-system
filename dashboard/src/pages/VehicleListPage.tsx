import { useEffect, useState } from 'react';
import { Plus, Power, Trash2 } from 'lucide-react';
import { api } from '@/lib/api';
import type { Vehicle } from '@/lib/types';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { formatDate } from '@/lib/utils';

export function VehicleListPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [serial, setSerial] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchVehicles = () => {
    setLoading(true);
    api.vehicles
      .list()
      .then(setVehicles)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.vehicles.create({ name, serialNumber: serial });
      setName('');
      setSerial('');
      setShowAdd(false);
      fetchVehicles();
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (v: Vehicle) => {
    await api.vehicles.update(v.id, { isActive: !v.is_active });
    fetchVehicles();
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    await api.vehicles.delete(deleteId);
    setDeleteId(null);
    fetchVehicles();
  };

  if (loading) return <PageLoading />;

  return (
    <>
      <PageHeader title="Vehicle Management">
        <button
          onClick={() => setShowAdd(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark"
        >
          <Plus className="h-4 w-4" /> Add Vehicle
        </button>
      </PageHeader>

      {showAdd && (
        <form
          onSubmit={handleCreate}
          className="mb-6 rounded-xl bg-white p-6 shadow-sm"
        >
          <h3 className="mb-4 text-lg font-semibold">New Vehicle</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700">Name</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
                placeholder="e.g. Ford Transit"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Serial Number
              </label>
              <input
                value={serial}
                onChange={(e) => setSerial(e.target.value)}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
                placeholder="e.g. VIN-123456"
              />
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-60"
            >
              {saving ? 'Creating…' : 'Create'}
            </button>
            <button
              type="button"
              onClick={() => setShowAdd(false)}
              className="rounded-lg px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {vehicles.map((v) => (
          <div key={v.id} className="rounded-xl bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-gray-900">{v.name}</h3>
                <p className="mt-1 font-mono text-xs text-gray-500">{v.serial_number}</p>
              </div>
              <span
                className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                  v.is_active
                    ? 'bg-green-100 text-green-700'
                    : 'bg-gray-100 text-gray-500'
                }`}
              >
                {v.is_active ? 'Active' : 'Inactive'}
              </span>
            </div>

            {v.last_active_at && (
              <p className="mt-3 text-xs text-gray-500">
                Last active: {formatDate(v.last_active_at)}
              </p>
            )}
            <p className="text-xs text-gray-400">
              Added: {formatDate(v.created_at)}
            </p>

            <div className="mt-4 flex gap-2">
              <button
                onClick={() => toggleActive(v)}
                className="inline-flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50"
              >
                <Power className="h-3 w-3" /> {v.is_active ? 'Deactivate' : 'Activate'}
              </button>
              <button
                onClick={() => setDeleteId(v.id)}
                className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50"
              >
                <Trash2 className="h-3 w-3" /> Delete
              </button>
            </div>
          </div>
        ))}
        {vehicles.length === 0 && (
          <p className="col-span-full py-12 text-center text-sm text-gray-500">
            No vehicles registered yet.
          </p>
        )}
      </div>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Vehicle"
        message="Are you sure you want to delete this vehicle? This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
}
