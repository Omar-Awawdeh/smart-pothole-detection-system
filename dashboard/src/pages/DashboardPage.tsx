import { useEffect, useState } from "react";
import { Link } from "react-router";
import { AlertTriangle, CheckCircle, Wrench, BarChart3 } from "lucide-react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Cell,
} from "recharts";
import { api } from "@/lib/api";
import type {
  StatsOverview,
  DailyStat,
  StatusStat,
  PotholeListItem,
} from "@/lib/types";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { PageLoading } from "@/components/ui/LoadingSpinner";
import { formatDate, truncateId, confidencePercent } from "@/lib/utils";

const STATUS_COLORS: Record<string, string> = {
  unverified: "#EAB308",
  verified: "#22C55E",
  repaired: "#3B82F6",
  false_positive: "#6B7280",
};

export function DashboardPage() {
  const [stats, setStats] = useState<StatsOverview | null>(null);
  const [daily, setDaily] = useState<DailyStat[]>([]);
  const [byStatus, setByStatus] = useState<StatusStat[]>([]);
  const [recent, setRecent] = useState<PotholeListItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.stats.overview(),
      api.stats.daily(30),
      api.stats.byStatus(),
      api.potholes.list({ limit: 5, sortBy: "detected_at", sortOrder: "DESC" }),
    ])
      .then(([s, d, bs, r]) => {
        setStats(s);
        setDaily(d);
        setByStatus(bs);
        setRecent(r.data);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoading />;

  const cards = [
    {
      label: "Total Potholes",
      value: stats?.total ?? 0,
      icon: BarChart3,
      textColor: "text-blue-600",
      bgColor: "bg-blue-50",
    },
    {
      label: "Unverified",
      value: stats?.unverified ?? 0,
      icon: AlertTriangle,
      textColor: "text-yellow-600",
      bgColor: "bg-yellow-50",
    },
    {
      label: "Verified",
      value: stats?.verified ?? 0,
      icon: CheckCircle,
      textColor: "text-green-600",
      bgColor: "bg-green-50",
    },
    {
      label: "Repaired",
      value: stats?.repaired ?? 0,
      icon: Wrench,
      textColor: "text-blue-500",
      bgColor: "bg-blue-50",
    },
  ];

  return (
    <>
      <PageHeader title="Dashboard Overview" />

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {cards.map((card) => (
          <div key={card.label} className="rounded-xl bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{card.label}</p>
                <p className="mt-1 text-3xl font-bold text-gray-900">
                  {card.value}
                </p>
              </div>
              <div className={`rounded-lg ${card.bgColor} p-3`}>
                <card.icon className={`h-6 w-6 ${card.textColor}`} />
              </div>
            </div>
            {card.label === "Total Potholes" && stats && (
              <p className="mt-2 text-xs text-gray-500">
                +{stats.todayCount} today
              </p>
            )}
          </div>
        ))}
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 rounded-xl bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            Detections (Last 30 Days)
          </h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={daily}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area
                  type="monotone"
                  dataKey="count"
                  stroke="#3B82F6"
                  fill="#3B82F6"
                  fillOpacity={0.1}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded-xl bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-gray-900">
            By Status
          </h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={byStatus} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" tick={{ fontSize: 11 }} />
                <YAxis
                  dataKey="status"
                  type="category"
                  tick={{ fontSize: 11 }}
                  width={90}
                />
                <Tooltip />
                <Bar dataKey="count" radius={[0, 4, 4, 0]}>
                  {byStatus.map((entry) => (
                    <Cell
                      key={entry.status}
                      fill={STATUS_COLORS[entry.status] ?? "#9CA3AF"}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="mt-8 rounded-xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">
            Recent Potholes
          </h2>
          <Link
            to="/potholes"
            className="text-sm font-medium text-primary hover:underline"
          >
            View All
          </Link>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead>
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Confidence
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                  Status
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {recent.map((p) => (
                <tr key={p.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-mono text-gray-700">
                    <Link
                      to={`/potholes/${p.id}`}
                      className="hover:text-primary"
                    >
                      {truncateId(p.id)}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {formatDate(p.detected_at)}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {confidencePercent(p.confidence)}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={p.status} />
                  </td>
                </tr>
              ))}
              {recent.length === 0 && (
                <tr>
                  <td
                    colSpan={4}
                    className="px-4 py-8 text-center text-sm text-gray-500"
                  >
                    No potholes detected yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
