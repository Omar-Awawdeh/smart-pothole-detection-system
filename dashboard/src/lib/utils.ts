import { clsx, type ClassValue } from 'clsx';

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function truncateId(id: string): string {
  return id.length > 8 ? `${id.slice(0, 8)}…` : id;
}

export function confidencePercent(confidence: number): string {
  return `${Math.round(confidence * 100)}%`;
}

export function statusColor(status: string): string {
  switch (status) {
    case 'unverified':
      return 'bg-yellow-100 text-yellow-800';
    case 'verified':
      return 'bg-green-100 text-green-800';
    case 'repaired':
      return 'bg-blue-100 text-blue-800';
    case 'false_positive':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-600';
  }
}

export function statusLabel(status: string): string {
  switch (status) {
    case 'unverified':
      return 'Unverified';
    case 'verified':
      return 'Verified';
    case 'repaired':
      return 'Repaired';
    case 'false_positive':
      return 'False Positive';
    default:
      return status;
  }
}

export function severityColor(severity: string): string {
  switch (severity) {
    case 'high':
      return 'bg-red-100 text-red-800';
    case 'medium':
      return 'bg-orange-100 text-orange-800';
    case 'low':
      return 'bg-green-100 text-green-800';
    default:
      return 'bg-gray-100 text-gray-600';
  }
}

export function markerColor(status: string): string {
  switch (status) {
    case 'unverified':
      return '#EF4444';
    case 'verified':
      return '#22C55E';
    case 'repaired':
      return '#3B82F6';
    case 'false_positive':
      return '#6B7280';
    default:
      return '#9CA3AF';
  }
}
