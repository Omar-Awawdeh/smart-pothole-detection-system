import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

interface Toast {
  id: number;
  message: string;
  sub?: string;
  type: 'info' | 'success' | 'error';
}

interface ToastContextType {
  toasts: Toast[];
  addToast: (message: string, sub?: string, type?: Toast['type']) => void;
}

const ToastContext = createContext<ToastContextType | null>(null);
let _id = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((message: string, sub?: string, type: Toast['type'] = 'info') => {
    const id = ++_id;
    setToasts((prev) => [...prev, { id, message, sub, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 5000);
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, addToast }}>
      {children}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`min-w-[280px] max-w-sm rounded-xl px-4 py-3 shadow-lg text-white transition-all ${
              t.type === 'error' ? 'bg-red-600' : t.type === 'success' ? 'bg-green-600' : 'bg-gray-900'
            }`}
          >
            <p className="text-sm font-medium">{t.message}</p>
            {t.sub && <p className="mt-0.5 text-xs opacity-80">{t.sub}</p>}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
