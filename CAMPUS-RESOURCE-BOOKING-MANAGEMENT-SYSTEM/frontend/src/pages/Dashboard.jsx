import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import toast from 'react-hot-toast'
import { occupancyColor } from '../utils/status'
import { useAuth } from '../context/AuthContext'
import client, { asItems, cachedGet } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import CampusMap from '../components/campus/CampusMap'
import { ConfirmDialog, EmptyState, ErrorBanner, PageHeader, Skeleton, StatCard } from '../components/ui'
import { errorMessage } from '../utils/status'
import useReducedMotion from '../hooks/useReducedMotion'

export default function Dashboard() {
  const { user } = useAuth()
  const reduced = useReducedMotion()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [results, setResults] = useState([])
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelling, setCancelling] = useState(false)
  const [quick, setQuick] = useState({
    typeCode: 'CLASSROOM',
    date: new Date().toISOString().slice(0, 10),
    startTime: '10:00',
    endTime: '12:00',
    minCapacity: 30,
  })

  const load = () => {
    setError('')
    cachedGet('/dashboard')
      .then((r) => setData(r.data))
      .catch((err) => setError(errorMessage(err, 'Dashboard could not load.')))
  }

  useEffect(() => { load() }, [])

  const find = async (e) => {
    e.preventDefault()
    const { data: page } = await client.get('/resources', { params: quick })
    setResults(asItems(page).slice(0, 8))
  }

  const confirmCancel = async () => {
    if (!cancelTarget) return
    setCancelling(true)
    try {
      await client.post(`/bookings/${cancelTarget.id}/cancel`)
      toast.success('Booking cancelled successfully')
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setCancelling(false)
      setCancelTarget(null)
    }
  }

  if (!data && !error) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-24" />
        <div className="grid grid-cols-2 gap-3 md:grid-cols-6">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-24" />)}</div>
        <Skeleton className="h-72" />
      </div>
    )
  }

  if (error && !data) {
    return <ErrorBanner message={error} onRetry={load} />
  }

  const live = data.live || {}
  const heat = data.heatmap || []
  const buildings = data.buildings || []
  const now = data.availableNow || []
  const mine = data.recentBookings || []
  const first = user.fullName.split(' ')[0]
  const activeMine = mine.filter((b) => !['CANCELLED', 'REJECTED', 'COMPLETED', 'NO_SHOW'].includes(b.status)).slice(0, 4)
  return (
    <div className="w-full space-y-6">
      <PageHeader
        kicker="Command deck"
        title={`Welcome back, ${first}`}
        subtitle="Live occupancy, your bookings, and a one-tap room search — all from SQL."
        actions={<div className="flex gap-2"><Link to="/bookings" className="btn-ghost">My bookings</Link><Link to="/book" className="btn-primary">Book a room</Link></div>}
      />
      {error && <ErrorBanner message={error} onRetry={load} />}

      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        <StatCard label="Free now" value={live.available ?? '—'} accent="#16a34a" hint={live.asOf ? 'Live campus snapshot' : null} />
        <StatCard label="In use" value={live.occupied ?? '—'} accent="#c14a32" />
        <StatCard label="Occupancy" value={`${live.occupancyPercent || 0}%`} />
        <StatCard label="Your pending" value={data.pending} />
        <StatCard label="Confirmed" value={data.confirmed} />
        <StatCard label={user.role === 'STUDENT' ? 'Completed' : 'Approvals'} value={user.role === 'STUDENT' ? data.completed : (data.pendingApprovals ?? 0)} />
      </div>

      {data.upcoming && (
        <Link to={`/bookings/${data.upcoming.id}`} className="block overflow-hidden rounded-[1.4rem] bg-grove-800 p-6 text-cream-50 shadow-lg transition hover:shadow-xl">
          <div className="text-xs uppercase tracking-[0.22em] text-gold-400">Next booking</div>
          <div className="mt-1 font-display text-3xl md:text-5xl">{data.upcoming.title}</div>
          <div className="mt-2 text-cream-200">{data.upcoming.date} · {data.upcoming.startTime}–{data.upcoming.endTime} · {data.upcoming.resources?.[0]?.name}</div>
        </Link>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.35fr_.65fr]">
        <section className="min-w-0">
          <CampusMap buildings={buildings} heatmap={heat} />
        </section>
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display text-2xl">Your bookings</h2>
            <Link to="/bookings" className="text-sm text-grove-600">View all</Link>
          </div>
          {activeMine.map((b) => (
            <div key={b.id} className="soft-card p-4">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <Link to={`/bookings/${b.id}`} className="font-semibold hover:underline">{b.title}</Link>
                  <div className="text-xs text-grove-500">{b.date} · {b.startTime}–{b.endTime}</div>
                </div>
                <StatusBadge status={b.status} />
              </div>
              <div className="mt-3 flex gap-2">
                <Link to={`/bookings/${b.id}`} className="btn-ghost text-xs">Open</Link>
                {b.status !== 'CHECKED_IN' && b.status !== 'COMPLETED' && b.status !== 'CANCELLED' && (
                  <button className="text-xs text-brick-500" onClick={() => setCancelTarget(b)}>Cancel</button>
                )}
              </div>
            </div>
          ))}
          {activeMine.length === 0 && (
            <EmptyState title="No active bookings" action={<Link className="btn-primary" to="/book">Book a room</Link>}>
              Search the campus or ask the assistant to reserve a room.
            </EmptyState>
          )}
          <div>
            <h2 className="mb-2 font-display text-2xl">Free right now</h2>
            <div className="space-y-2">
              {now.map((r) => (
                <Link key={r.id} to={`/book/${r.id}`} className="block rounded-xl border border-cream-200 px-3 py-2 text-sm transition hover:bg-white dark:border-white/10">
                  {r.name} <span className="text-grove-500">· {r.buildingName}</span>
                </Link>
              ))}
              {now.length === 0 && <p className="text-sm text-grove-500">Nothing free this minute.</p>}
            </div>
          </div>
        </section>
      </div>

      {heat.length > 0 && (
        <section>
          <h2 className="mb-3 font-display text-2xl">Block load</h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {heat.map((h) => (
              <Link key={h.buildingId} to={`/buildings/${h.buildingId}`} className="soft-card block p-4 transition hover:-translate-y-0.5 hover:shadow-md">
                <div className="flex items-center justify-between">
                  <span className="font-semibold">{h.name}</span>
                  <span className="h-2.5 w-2.5 rounded-full" style={{ background: occupancyColor(h.level) }} />
                </div>
                <div className="font-display text-3xl">{h.percent}%</div>
                <div className="text-xs text-grove-500">{h.available}/{h.total} free</div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {user.role === 'ADMIN' && (
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="soft-card p-4">
            <div className="mb-2 flex items-center justify-between">
              <div className="font-display text-xl">Booking trend</div>
              <Link to="/admin/analytics" className="text-xs text-grove-600">Full analytics</Link>
            </div>
            <div className="h-52">
              <ResponsiveContainer>
                <AreaChart data={data.bookingTrends || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e8dfcc" />
                  <XAxis dataKey="date" hide />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="count" stroke="#146c4a" fill="#146c4a33" isAnimationActive={!reduced} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="soft-card p-4">
            <div className="mb-2 font-display text-xl">Peak hours</div>
            <div className="h-52">
              <ResponsiveContainer>
                <AreaChart data={data.peakHours || []}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e8dfcc" />
                  <XAxis dataKey="hour" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Area type="monotone" dataKey="count" stroke="#c14a32" fill="#c14a3222" isAnimationActive={!reduced} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      <form onSubmit={find} className="soft-card grid gap-3 p-4 md:grid-cols-6">
        <label className="sr-only" htmlFor="quick-type">Type</label>
        <select id="quick-type" className="field" value={quick.typeCode} onChange={(e) => setQuick({ ...quick, typeCode: e.target.value })}>
          <option>CLASSROOM</option><option>LABORATORY</option><option>SEMINAR_HALL</option><option>LIBRARY</option>
        </select>
        <input type="date" aria-label="Date" className="field" value={quick.date} onChange={(e) => setQuick({ ...quick, date: e.target.value })} />
        <input type="time" aria-label="Start" className="field" value={quick.startTime} onChange={(e) => setQuick({ ...quick, startTime: e.target.value })} />
        <input type="time" aria-label="End" className="field" value={quick.endTime} onChange={(e) => setQuick({ ...quick, endTime: e.target.value })} />
        <input type="number" aria-label="Minimum capacity" className="field" value={quick.minCapacity} onChange={(e) => setQuick({ ...quick, minCapacity: e.target.value })} />
        <button className="btn-primary">Find free rooms</button>
      </form>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        {results.map((r) => (
          <Link key={r.id} to={`/book/${r.id}`} className="soft-card p-4 transition hover:-translate-y-0.5 hover:shadow-md">
            <div className="font-semibold">{r.name}</div>
            <div className="text-sm text-grove-500">{r.buildingName} · cap {r.capacity}</div>
          </Link>
        ))}
      </div>

      <ConfirmDialog
        open={!!cancelTarget}
        title="Cancel Booking?"
        danger
        confirmLabel="Cancel Booking"
        busy={cancelling}
        onCancel={() => setCancelTarget(null)}
        onConfirm={confirmCancel}
      >
        Are you sure you want to cancel <strong>"{cancelTarget?.title}"</strong>? The room will be freed for others.
      </ConfirmDialog>
    </div>
  )
}
