import { useEffect, useState } from 'react';
import { Pencil, Plus, Trash2, X } from 'lucide-react';
import { api } from '@/lib/api';
import type { User } from '@/lib/types';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageLoading } from '@/components/ui/LoadingSpinner';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';

const ROLES = ['viewer', 'operator', 'admin'] as const;

export function UserListPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  // Add form state
  const [showAdd, setShowAdd] = useState(false);
  const [addForm, setAddForm] = useState({ name: '', email: '', password: '', role: 'viewer' });
  const [addSaving, setAddSaving] = useState(false);

  // Edit form state
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [editForm, setEditForm] = useState({ name: '', email: '', role: '' });
  const [editSaving, setEditSaving] = useState(false);

  const fetchUsers = () => {
    setLoading(true);
    api.users
      .list()
      .then(setUsers)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddSaving(true);
    try {
      await api.users.create(addForm);
      setAddForm({ name: '', email: '', password: '', role: 'viewer' });
      setShowAdd(false);
      fetchUsers();
    } finally {
      setAddSaving(false);
    }
  };

  const startEdit = (u: User) => {
    setEditingUser(u);
    setEditForm({ name: u.name, email: u.email, role: u.role });
  };

  const cancelEdit = () => {
    setEditingUser(null);
    setEditForm({ name: '', email: '', role: '' });
  };

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    setEditSaving(true);
    try {
      await api.users.update(editingUser.id, editForm);
      cancelEdit();
      fetchUsers();
    } finally {
      setEditSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteId) return;
    await api.users.delete(deleteId);
    setDeleteId(null);
    fetchUsers();
  };

  if (loading) return <PageLoading />;

  return (
    <>
      <PageHeader title="User Management">
        <button
          onClick={() => { setShowAdd(true); cancelEdit(); }}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark"
        >
          <Plus className="h-4 w-4" /> Add User
        </button>
      </PageHeader>

      {showAdd && (
        <form
          onSubmit={handleAdd}
          className="mb-6 rounded-xl bg-white p-6 shadow-sm"
        >
          <h3 className="mb-4 text-lg font-semibold">New User</h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700">Name</label>
              <input
                value={addForm.name}
                onChange={(e) => setAddForm((f) => ({ ...f, name: e.target.value }))}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
                placeholder="e.g. Jane Smith"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Email</label>
              <input
                type="email"
                value={addForm.email}
                onChange={(e) => setAddForm((f) => ({ ...f, email: e.target.value }))}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
                placeholder="jane@example.com"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Password</label>
              <input
                type="password"
                value={addForm.password}
                onChange={(e) => setAddForm((f) => ({ ...f, password: e.target.value }))}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Role</label>
              <select
                value={addForm.role}
                onChange={(e) => setAddForm((f) => ({ ...f, role: e.target.value }))}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r.charAt(0).toUpperCase() + r.slice(1)}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={addSaving}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-60"
            >
              {addSaving ? 'Creating…' : 'Create'}
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

      {editingUser && (
        <form
          onSubmit={handleEdit}
          className="mb-6 rounded-xl bg-white p-6 shadow-sm"
        >
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold">Edit User</h3>
            <button type="button" onClick={cancelEdit} className="text-gray-400 hover:text-gray-600">
              <X className="h-5 w-5" />
            </button>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-gray-700">Name</label>
              <input
                value={editForm.name}
                onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Email</label>
              <input
                type="email"
                value={editForm.email}
                onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                required
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Role</label>
              <select
                value={editForm.role}
                onChange={(e) => setEditForm((f) => ({ ...f, role: e.target.value }))}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r.charAt(0).toUpperCase() + r.slice(1)}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button
              type="submit"
              disabled={editSaving}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-60"
            >
              {editSaving ? 'Saving…' : 'Save'}
            </button>
            <button
              type="button"
              onClick={cancelEdit}
              className="rounded-lg px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="rounded-xl bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Name
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Email
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Role
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {users.map((u) => (
                <tr
                  key={u.id}
                  className={`hover:bg-gray-50 ${editingUser?.id === u.id ? 'bg-blue-50' : ''}`}
                >
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{u.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{u.email}</td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium capitalize text-gray-700">
                      {u.role}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => { startEdit(u); setShowAdd(false); }}
                        className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm text-blue-600 hover:bg-blue-50"
                      >
                        <Pencil className="h-4 w-4" /> Edit
                      </button>
                      <button
                        onClick={() => setDeleteId(u.id)}
                        className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-sm text-red-600 hover:bg-red-50"
                      >
                        <Trash2 className="h-4 w-4" /> Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-12 text-center text-sm text-gray-500">
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete User"
        message="Are you sure you want to delete this user? This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteId(null)}
      />
    </>
  );
}
