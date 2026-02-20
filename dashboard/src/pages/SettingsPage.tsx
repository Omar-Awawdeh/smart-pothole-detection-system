import { useAuth } from '@/contexts/AuthContext';
import { PageHeader } from '@/components/ui/PageHeader';

export function SettingsPage() {
  const { user } = useAuth();

  return (
    <>
      <PageHeader title="Settings" />

      <div className="max-w-2xl rounded-xl bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-gray-900">Profile</h2>

        <dl className="divide-y divide-gray-100">
          <div className="flex justify-between py-3">
            <dt className="text-sm font-medium text-gray-500">Name</dt>
            <dd className="text-sm text-gray-900">{user?.name}</dd>
          </div>
          <div className="flex justify-between py-3">
            <dt className="text-sm font-medium text-gray-500">Email</dt>
            <dd className="text-sm text-gray-900">{user?.email}</dd>
          </div>
          <div className="flex justify-between py-3">
            <dt className="text-sm font-medium text-gray-500">Role</dt>
            <dd className="text-sm capitalize text-gray-900">{user?.role}</dd>
          </div>
        </dl>
      </div>

      <div className="mt-6 max-w-2xl rounded-xl bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-gray-900">Preferences</h2>
        <p className="text-sm text-gray-500">
          Additional settings like notification preferences and display options will be available in a future update.
        </p>
      </div>
    </>
  );
}
