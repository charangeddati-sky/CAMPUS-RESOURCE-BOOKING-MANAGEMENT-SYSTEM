import { useEffect, useState } from 'react'
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import toast from 'react-hot-toast'
import { cachedGet } from '../../api/client'
import client from '../../api/client'
import { downloadFile } from '../../components/AuthImage'
import { ErrorBanner, PageHeader, Skeleton, StatCard } from '../../components/ui'
import { errorMessage } from '../../utils/status'
import useReducedMotion from '../../hooks/useReducedMotion'

const COLORS = ['#146c4a', '#c14a32', '#d4a017', '#0f3d2e', '#64748b', '#ea580c', '#16a34a', '#334155']

function isoDaysAgo(n) {
  return new Date(Date.now() - n * 86400000).toISOString().slice(0, 10)
}

export default function AnalyticsDashboard() {
  const reduced = useReducedMotion()
  const [from, setFrom] = useState(isoDaysAgo(29))
  const [to, setTo] = useState(isoDaysAgo(0))
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      try {
        const r = await cachedGet('/analytics/overview', { params: { from, to } }, 8_000)
        setData(r.data)
        return
      } catch {
        // Compose from endpoints that exist on older backends.
      }
      const [dash, heat, live, util] = await Promise.all([
        cachedGet('/dashboard'),
        cachedGet('/analytics/heatmap'),
        cachedGet('/analytics/live'),
        client.get('/analytics/utilization', { params: { from, to } }).catch(() => ({ data: [] })),
      ])
      const d = dash.data || {}
      const utilization = (util.data || []).map((u) => ({
        resourceId: u.resourceId,
        name: u.name,
        building: u.building,
        bookedHours: u.bookedHours,
        utilizationPercent: u.utilization,
      }))
      setData({
        kpis: {
          totalBookings: d.bookingsToday,
          activeUsers: d.user ? 1 : 0,
          availableNow: live.data?.available ?? d.availableNow ?? d.live?.available,
          occupancyPercent: live.data?.occupancyPercent ?? d.occupancyPercent ?? d.live?.occupancyPercent,
          avgUtilizationPercent: 0,
          cancellationRatePercent: 0,
          noShowRatePercent: 0,
          pendingApprovals: d.pendingApprovals,
          totalResources: d.totalResources,
          openIssues: d.openIssues,
        },
        live: live.data || d.live,
        bookingTrends: d.bookingTrends || [],
        peakHours: d.peakHours || [],
        peakDays: [],
        statusMix: d.statusMix || {},
        mostBooked: [],
        leastBooked: [],
        utilization,
        buildingPerformance: (heat.data || []).map((h) => ({ id: h.buildingId, name: h.name, count: h.percent })),
        heatmap: heat.data || d.heatmap || [],
        predictions: null,
      })
    } catch (err) {
      setError(errorMessage(err, 'Analytics could not load.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [from, to])

  const exportPdf = async () => {
    try {
      await downloadFile(`/admin/reports/pdf?from=${from}&to=${to}`, `campusos-analytics-${from}-to-${to}.pdf`)
      toast.success('PDF downloaded')
    } catch (err) {
      toast.error(errorMessage(err, 'PDF export failed.'))
    }
  }

  if (loading && !data) {
    return <div className="space-y-3"><Skeleton className="h-20" /><div className="grid grid-cols-2 gap-3 md:grid-cols-4">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} />)}</div></div>
  }

  const kpis = data?.kpis || {}
  const statusMix = Object.entries(data?.statusMix || {}).map(([name, count]) => ({ name: name.replaceAll('_', ' '), count }))
  const predictions = data?.predictions

  return (
    <div className="space-y-6">
      <PageHeader
        kicker="Administration"
        title="Campus analytics"
        subtitle="Every chart is aggregated from SQL. Forecast cards are labeled so they are never confused with live occupancy."
        actions={
          <div className="flex flex-wrap items-end gap-2">
            <label className="text-xs">From<input type="date" className="field mt-1" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
            <label className="text-xs">To<input type="date" className="field mt-1" value={to} onChange={(e) => setTo(e.target.value)} /></label>
            <button type="button" className="btn-primary" onClick={exportPdf}>Export PDF</button>
          </div>
        }
      />
      {error && <ErrorBanner message={error} onRetry={load} />}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-8">
        <StatCard label="Bookings" value={kpis.totalBookings} />
        <StatCard label="Active users" value={kpis.activeUsers} />
        <StatCard label="Available now" value={kpis.availableNow} accent="#16a34a" hint="Live, not historical" />
        <StatCard label="Occupancy" value={`${kpis.occupancyPercent ?? 0}%`} hint="Live snapshot" />
        <StatCard label="Avg utilization" value={`${kpis.avgUtilizationPercent ?? 0}%`} />
        <StatCard label="Cancel rate" value={`${kpis.cancellationRatePercent ?? 0}%`} />
        <StatCard label="No-show rate" value={`${kpis.noShowRatePercent ?? 0}%`} />
        <StatCard label="Pending" value={kpis.pendingApprovals} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <ChartCard title="Booking trend (actual)">
          <ResponsiveContainer>
            <AreaChart data={data?.bookingTrends || []}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e8dfcc" />
              <XAxis dataKey="date" hide />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Area type="monotone" dataKey="count" stroke="#146c4a" fill="#146c4a33" isAnimationActive={!reduced} />
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title="Peak hours (actual)">
          <ResponsiveContainer>
            <BarChart data={data?.peakHours || []}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e8dfcc" />
              <XAxis dataKey="hour" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#c14a32" radius={6} isAnimationActive={!reduced} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title="Peak days (actual)">
          <ResponsiveContainer>
            <BarChart data={data?.peakDays || []}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e8dfcc" />
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#0f3d2e" radius={6} isAnimationActive={!reduced} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title="Booking status">
          <ResponsiveContainer>
            <PieChart>
              <Pie data={statusMix} dataKey="count" nameKey="name" innerRadius={50} outerRadius={80} isAnimationActive={!reduced}>
                {statusMix.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <RankTable title="Most booked resources" rows={data?.mostBooked} />
        <RankTable title="Least booked resources" rows={data?.leastBooked} />
      </div>

      <div className="soft-card overflow-x-auto p-4">
        <h2 className="font-display text-xl">Resource utilization</h2>
        <table className="mt-3 w-full text-left text-sm">
          <thead><tr className="text-xs uppercase tracking-wide text-grove-500"><th className="py-2">Resource</th><th>Building</th><th>Hours</th><th>Utilization</th></tr></thead>
          <tbody>
            {(data?.utilization || []).map((r) => (
              <tr key={r.resourceId} className="border-t border-cream-200 dark:border-white/10">
                <td className="py-2">{r.name}</td>
                <td>{r.building}</td>
                <td>{r.bookedHours}</td>
                <td>{r.utilizationPercent}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="soft-card overflow-x-auto p-4">
        <h2 className="font-display text-xl">Campus / building performance</h2>
        <table className="mt-3 w-full text-left text-sm">
          <thead><tr className="text-xs uppercase tracking-wide text-grove-500"><th className="py-2">Building</th><th>Bookings</th></tr></thead>
          <tbody>
            {(data?.buildingPerformance || []).map((b) => (
              <tr key={b.id || b.name} className="border-t border-cream-200 dark:border-white/10">
                <td className="py-2">{b.name}</td>
                <td>{b.count}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {predictions && (
        <section className="rounded-[1.4rem] border border-gold-500/40 bg-gold-500/5 p-5">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <div>
              <p className="stat-kicker text-gold-500">Forecast · not live data</p>
              <h2 className="font-display text-2xl">Predictions</h2>
            </div>
            <span className="rounded-full bg-gold-500/20 px-3 py-1 text-xs font-semibold uppercase tracking-wide">{predictions.kind}</span>
          </div>
          <p className="mt-2 max-w-3xl text-sm text-grove-600 dark:text-grove-200">{predictions.disclaimer}</p>
          <p className="mt-1 text-xs text-grove-500">{predictions.method} Sample: {predictions.sampleFrom} to {predictions.sampleTo} ({predictions.sampleBookings} resource-bookings).</p>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <StatCard label="Predicted peak hour" value={predictions.peakHour?.value} hint={`${predictions.peakHour?.confidence} confidence · ${predictions.peakHour?.basis}`} />
            <StatCard label="Predicted peak day" value={predictions.peakDayOfWeek?.value} hint={`${predictions.peakDayOfWeek?.confidence} confidence · ${predictions.peakDayOfWeek?.basis}`} />
          </div>
          <h3 className="mt-5 font-display text-xl">Frequently booked (forecast)</h3>
          <div className="mt-2 grid gap-2 md:grid-cols-2">
            {(predictions.frequentResources || []).map((r) => (
              <div key={r.resourceId} className="rounded-xl border border-cream-200 p-3 text-sm dark:border-white/10">
                <div className="font-semibold">{r.name}</div>
                <div className="text-grove-500">{r.building} · ~{r.expectedBookingsPerWeek}/week · {r.confidence}</div>
              </div>
            ))}
          </div>
          <h3 className="mt-5 font-display text-xl">Next 7 days demand (forecast)</h3>
          <div className="mt-2 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead><tr className="text-xs uppercase tracking-wide text-grove-500"><th className="py-2">Date</th><th>Day</th><th>Expected bookings</th><th>Confidence</th></tr></thead>
              <tbody>
                {(predictions.nextSevenDays || []).map((d) => (
                  <tr key={d.date} className="border-t border-cream-200 dark:border-white/10">
                    <td className="py-2">{d.date}</td>
                    <td>{d.dayOfWeek}</td>
                    <td>{d.expectedBookings}</td>
                    <td>{d.confidence}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function ChartCard({ title, children }) {
  return (
    <div className="soft-card h-72 p-4">
      <div className="mb-2 font-display text-xl">{title}</div>
      <div className="h-56">{children}</div>
    </div>
  )
}

function RankTable({ title, rows }) {
  return (
    <div className="soft-card overflow-x-auto p-4">
      <h2 className="font-display text-xl">{title}</h2>
      <table className="mt-3 w-full text-left text-sm">
        <thead><tr className="text-xs uppercase tracking-wide text-grove-500"><th className="py-2">Resource</th><th>Building</th><th>Bookings</th></tr></thead>
        <tbody>
          {(rows || []).map((r) => (
            <tr key={r.resourceId} className="border-t border-cream-200 dark:border-white/10">
              <td className="py-2">{r.name}</td>
              <td>{r.building}</td>
              <td>{r.bookings}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
