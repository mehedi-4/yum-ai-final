import { useCallback, useEffect, useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import api from '../api/client'
import { Badge, Card, money } from '../components/ui.jsx'

const REFRESH_MS = 30_000 // FR-04.4: auto-refresh without manual reload

function Kpi({ label, value, accent }) {
  return (
    <Card>
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${accent ?? ''}`}>{value}</p>
    </Card>
  )
}

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [updatedAt, setUpdatedAt] = useState(null)

  const load = useCallback(async () => {
    const { data } = await api.get('/dashboard')
    setData(data)
    setUpdatedAt(new Date())
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, REFRESH_MS)
    return () => clearInterval(id)
  }, [load])

  if (!data) return <p className="text-slate-500">Loading dashboard…</p>

  const { kpis, peakHours, topItems } = data

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Dashboard</h1>
        {updatedAt && (
          <span className="text-xs text-slate-400">
            Auto-refreshes every 30s · updated {updatedAt.toLocaleTimeString()}
          </span>
        )}
      </div>

      {/* FR-04.1 KPIs */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        <Kpi label="Orders Today" value={kpis.totalOrdersToday} />
        <Kpi label="Revenue Today" value={money(kpis.revenueToday)} accent="text-emerald-600" />
        <Kpi label="Revenue This Month" value={money(kpis.revenueThisMonth)} accent="text-emerald-600" />
        <Kpi label="Pending Orders" value={kpis.pendingOrders} accent="text-amber-600" />
        <Kpi
          label="Low-Stock Alerts"
          value={kpis.activeAlerts}
          accent={kpis.activeAlerts > 0 ? 'text-red-600' : 'text-emerald-600'}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* FR-04.3 peak hours */}
        <Card title="Peak Hours (last 30 days)">
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={peakHours.map((p) => ({ ...p, label: `${p.hour}:00` }))}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Bar dataKey="orders" fill="#f97316" name="Orders" />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Top Sellers (last 30 days)">
          <ul className="space-y-3">
            {topItems.map((t, i) => (
              <li key={t.name} className="flex items-center justify-between">
                <span className="flex items-center gap-2">
                  <Badge color={i === 0 ? 'amber' : 'slate'}>#{i + 1}</Badge>
                  {t.name}
                </span>
                <span className="text-sm text-slate-500">
                  {t.quantitySold} sold · {money(t.revenue)}
                </span>
              </li>
            ))}
            {topItems.length === 0 && <p className="text-sm text-slate-400">No sales yet.</p>}
          </ul>
        </Card>
      </div>
    </div>
  )
}
