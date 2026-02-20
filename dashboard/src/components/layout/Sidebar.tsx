import { NavLink } from 'react-router';
import { LayoutDashboard, AlertTriangle, Map, Car, Users, Settings, X } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/lib/utils';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/potholes', icon: AlertTriangle, label: 'Potholes' },
  { to: '/map', icon: Map, label: 'Map View' },
  { to: '/vehicles', icon: Car, label: 'Vehicles' },
  { to: '/settings', icon: Settings, label: 'Settings' },
];

export function Sidebar({ open, onClose }: SidebarProps) {
  const { isAdmin } = useAuth();

  const links = isAdmin
    ? [...navItems.slice(0, 4), { to: '/users', icon: Users, label: 'Users' }, navItems[4]]
    : navItems;

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col bg-white shadow-lg transition-transform lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center justify-between border-b px-6">
          <span className="text-xl font-bold text-primary">Pothole AI</span>
          <button
            onClick={onClose}
            className="rounded-md p-1 hover:bg-gray-100 lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 px-3 py-4">
          {links.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
                )
              }
            >
              <Icon className="h-5 w-5" />
              {label}
            </NavLink>
          ))}
        </nav>
      </aside>
    </>
  );
}
