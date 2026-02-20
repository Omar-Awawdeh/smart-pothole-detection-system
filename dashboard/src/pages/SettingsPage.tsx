import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/contexts/ToastContext';
import { api } from '@/lib/api';
import { PageHeader } from '@/components/ui/PageHeader';

export function SettingsPage() {
  const { user } = useAuth();
  const { addToast } = useToast();

  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.auth.updateProfile({
        name: name !== user?.name ? name : undefined,
        email: email !== user?.email ? email : undefined,
        currentPassword: currentPassword || undefined,
        newPassword: newPassword || undefined,
      });
      addToast('Profile updated', undefined, 'success');
      setCurrentPassword('');
      setNewPassword('');
    } catch {
      addToast('Failed to update profile', 'Check your current password', 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Settings" />

      <form onSubmit={handleSave} className="max-w-2xl space-y-6">
        <div className="rounded-xl bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">Profile</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Name</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div className="flex justify-between py-1">
              <span className="text-sm text-gray-500">Role</span>
              <span className="text-sm capitalize text-gray-900">{user?.role}</span>
            </div>
          </div>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-sm">
          <h2 className="mb-1 text-lg font-semibold text-gray-900">Change Password</h2>
          <p className="mb-4 text-sm text-gray-500">Leave blank to keep your current password.</p>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">New Password</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none"
              />
            </div>
          </div>
        </div>

        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-primary px-6 py-2 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-60"
        >
          {saving ? 'Saving…' : 'Save Changes'}
        </button>
      </form>
    </>
  );
}
