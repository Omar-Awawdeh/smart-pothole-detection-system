export function LoadingSpinner({ className = 'h-8 w-8' }: { className?: string }) {
  return (
    <div
      className={`animate-spin rounded-full border-4 border-primary border-t-transparent ${className}`}
    />
  );
}

export function PageLoading() {
  return (
    <div className="flex h-64 items-center justify-center">
      <LoadingSpinner />
    </div>
  );
}
