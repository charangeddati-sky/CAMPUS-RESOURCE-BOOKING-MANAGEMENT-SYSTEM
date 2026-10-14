import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import client from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { errorMessage, formatDate, formatTime } from '../utils/status'
import { ConfirmDialog, EmptyState, PageHeader, Skeleton } from '../components/ui'

const TABS = [
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'pending', label: 'Waiting Approval' },
  { id: 'past', label: 'Past' },
  { id: 'all', label: 'All' },
]

function bucket(b) {
  const status = b.status
  if (['PENDING_PROFESSOR', 'PENDING_ADMIN'].includes(status)) return 'pending'
  if (['COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'].includes(status)) return 'past'
  return 'upcoming'
}

function canCancel(b) {
  return !['COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'].includes(b.status)
}

export default function Bookings() {
  const [rows, setRows] = useState([])
  const [tab, setTab] = useState('upcoming')
  const [q, setQ] = useState('')
  const [loading, setLoading] = useState(true)
  const [cancelTarget, setCancelTarget] = useState(null)
  const [cancelling, setCancelling] = useState(false)

  const load = () => {
    setLoading(true)
    client.get('/bookings/my')
      .then((r) => setRows(r.data || []))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const counts = useMemo(() => ({
    upcoming: rows.filter((b) => bucket(b) === 'upcoming').length,
    pending: rows.filter((b) => bucket(b) === 'pending').length,
    past: rows.filter((b) => bucket(b) === 'past').length,
    all: rows.length,
  }), [rows])

  const visible = useMemo(() => {
    return rows.filter((b) => {
      if (tab !== 'all' && bucket(b) !== tab) return false
      if (!q) return true
      const hay = `${b.title} ${b.purpose || ''} ${(b.resources || []).map((r) => r.name).join(' ')}`.toLowerCase()
      return hay.includes(q.toLowerCase())
    })
  }, [rows, tab, q])

  const act = async (id, path, ok) => {
    try {
      await client.post(`/bookings/${id}/${path}`)
      toast.success(ok)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    }
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

  return (
    <div className="w-full">
      <PageHeader
        title="My Bookings"
        subtitle="See what's coming, cancel what you no longer need, and check in on the day."
        actions={
          <div className="flex gap-2">
            <Link to="/ai-chat" className="btn-ghost">AI Assistant</Link>
            <Link to="/book" className="btn-primary">+ New Booking</Link>
          </div>
        }
      />

      {/* Tabs with counts */}
      <div className="mb-4 flex flex-wrap items-center gap-2">
        {TABS.map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)} className={`rounded-full px-4 py-1.5 text-sm transition ${tab === t.id ? 'bg-grove-700 text-white shadow-sm' : 'bg-white hover:bg-grove-50 dark:bg-grove-800 dark:hover:bg-grove-700'}`}>
            {t.label}
            {counts[t.id] > 0 && <span className="ml-1.5 rounded-full bg-white/20 px-1.5 text-xs">{counts[t.id]}</span>}
          </button>
        ))}
        <input className="field ml-auto max-w-xs rounded-full" placeholder="Search your bookings…" value={q} onChange={(e) => setQ(e.target.value)} />
      </div>

      {/* Loading state */}
      {loading && (
        <div className="grid gap-3 lg:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-36" />)}
        </div>
      )}

      {/* Booking cards */}
      {!loading && (
        <div className="grid gap-3 lg:grid-cols-2">
          {visible.map((b) => (
            <article key={b.id} className="soft-card p-5 transition hover:shadow-md">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <Link to={`/bookings/${b.id}`} className="font-display text-2xl hover:underline">{b.title}</Link>
                  <div className="mt-1 text-sm text-grove-600 dark:text-grove-200">
                    📅 {formatDate(b.date)} · 🕐 {formatTime(b.startTime)} – {formatTime(b.endTime)}
                  </div>
                  <div className="mt-0.5 text-xs text-grove-500">
                    {b.resources?.map((r) => `${r.name} · ${r.buildingName}`).join(', ')}
                  </div>
                </div>
                <StatusBadge status={b.status} />
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link to={`/bookings/${b.id}`} className="btn-ghost text-xs" data-tooltip="View booking details">View Details</Link>
                {b.status === 'CONFIRMED' && (
                  <button className="btn-primary text-xs" onClick={() => act(b.id, 'check-in', 'Checked in successfully')}>
                    📍 Check In
                  </button>
                )}
                {b.status === 'CHECKED_IN' && (
                  <button className="btn-primary text-xs" onClick={() => act(b.id, 'check-out', 'Checked out successfully')}>
                    ✓ Check Out
                  </button>
                )}
                {canCancel(b) && (
                  <button className="btn-ghost text-xs text-brick-500" onClick={() => setCancelTarget(b)}>
                    Cancel Booking
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && visible.length === 0 && (
        <EmptyState
          title={tab === 'all' ? "You don't have any bookings yet" : `No ${tab} bookings`}
          icon={tab === 'upcoming' ? '📅' : tab === 'pending' ? '⏳' : tab === 'past' ? '✔️' : '📋'}
          action={
            <div className="flex flex-wrap justify-center gap-2">
              <Link to="/book" className="btn-primary">+ Create Booking</Link>
              <Link to="/ai-chat" className="btn-ghost">Ask AI Assistant</Link>
            </div>
          }
        >
          {tab === 'upcoming' ? 'Book a room to see your upcoming reservations here.' :
           tab === 'pending' ? 'Bookings waiting for professor or admin approval will appear here.' :
           tab === 'past' ? 'Your completed and cancelled bookings will appear here.' :
           'Start by booking a room or asking the AI assistant.'}
        </EmptyState>
      )}

      {/* Cancel confirmation dialog */}
      <ConfirmDialog
        open={!!cancelTarget}
        title="Cancel Booking?"
        danger
        confirmLabel="Cancel Booking"
        busy={cancelling}
        onCancel={() => setCancelTarget(null)}
        onConfirm={confirmCancel}
      >
        <p>Are you sure you want to cancel <strong>"{cancelTarget?.title}"</strong> on {formatDate(cancelTarget?.date)}?</p>
        <p className="mt-2 text-xs text-grove-500">The room will be freed and available for others to book. This action cannot be undone.</p>
      </ConfirmDialog>
    </div>
  )
}
