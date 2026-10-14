import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import client from '../api/client'
import { errorMessage, formatDate, formatTime } from '../utils/status'
import StatusBadge from '../components/StatusBadge'
import { downloadFile } from '../components/AuthImage'
import { ConfirmDialog, PageHeader, Skeleton } from '../components/ui'

const flow = [
  { key: 'PENDING_PROFESSOR', label: 'Requested', desc: 'Waiting for professor review' },
  { key: 'PENDING_ADMIN', label: 'Professor Approved', desc: 'Waiting for admin confirmation' },
  { key: 'CONFIRMED', label: 'Confirmed', desc: 'Ready for check-in' },
  { key: 'CHECKED_IN', label: 'Checked In', desc: 'Currently in session' },
  { key: 'COMPLETED', label: 'Completed', desc: 'Session finished' },
]

export default function BookingDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [b, setB] = useState(null)
  const [loading, setLoading] = useState(true)
  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [acting, setActing] = useState(false)

  const load = () => {
    setLoading(true)
    client.get(`/bookings/${id}`)
      .then((r) => setB(r.data))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [id])

  if (loading) return <div className="space-y-4"><Skeleton className="h-20" /><Skeleton className="h-64" /><Skeleton className="h-16" /></div>

  if (!b) return (
    <div className="soft-card p-10 text-center">
      <p className="font-display text-2xl">Booking not found</p>
      <Link to="/bookings" className="btn-primary mt-4 inline-flex">Back to My Bookings</Link>
    </div>
  )

  const act = async (path, successMsg) => {
    setActing(true)
    try {
      await client.post(`/bookings/${id}/${path}`)
      toast.success(successMsg)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setActing(false)
    }
  }

  const confirmCancel = async () => {
    setCancelling(true)
    try {
      await client.post(`/bookings/${id}/cancel`)
      toast.success('Booking cancelled successfully')
      setCancelOpen(false)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setCancelling(false)
    }
  }

  const idx = flow.findIndex((f) => f.key === b.status)
  const room = b.resources?.[0]
  const canCancelBooking = !['COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'].includes(b.status)
  const canCheckIn = b.status === 'CONFIRMED'
  const canCheckOut = b.status === 'CHECKED_IN'
  const isTerminal = ['COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW'].includes(b.status)

  return (
    <div className="space-y-6">
      <PageHeader
        kicker={`Booking #${b.id}`}
        title={b.title}
        subtitle={`${room ? `${room.name} · ${room.buildingName}` : ''}`}
        actions={
          <div className="flex flex-wrap gap-2">
            <Link to="/bookings" className="btn-ghost">← My Bookings</Link>
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1.2fr_.8fr]">
        {/* Left: Details */}
        <div className="space-y-6">
          {/* Booking info card */}
          <div className="soft-card p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-display text-xl">Booking Details</h2>
              <StatusBadge status={b.status} />
            </div>
            <div className="space-y-3 text-sm">
              <DetailRow label="Date" value={formatDate(b.date)} />
              <DetailRow label="Time" value={`${formatTime(b.startTime)} – ${formatTime(b.endTime)}`} />
              {room && <DetailRow label="Room" value={room.name} />}
              {room && <DetailRow label="Location" value={`${room.buildingName} – ${room.floorName}`} />}
              {room && <DetailRow label="Room Type" value={room.typeName} />}
              {b.attendees > 0 && <DetailRow label="Attendees" value={b.attendees} />}
              {b.purpose && <DetailRow label="Purpose" value={b.purpose} />}
              {b.requirements && <DetailRow label="Requirements" value={b.requirements} />}
              {b.bookingKind && <DetailRow label="Type" value={b.bookingKind} />}
            </div>
          </div>

          {/* Actions */}
          <div className="soft-card p-5">
            <h2 className="mb-3 font-display text-xl">Actions</h2>
            <div className="flex flex-wrap gap-2">
              {canCheckIn && (
                <button onClick={() => act('check-in', 'Checked in successfully!')} className="btn-primary" disabled={acting}>
                  📍 Check In
                </button>
              )}
              {canCheckOut && (
                <button onClick={() => act('check-out', 'Checked out successfully!')} className="btn-primary" disabled={acting}>
                  ✓ Check Out
                </button>
              )}
              <button onClick={() => downloadFile(`/bookings/${b.id}/calendar`, `booking-${b.id}.ics`)} className="btn-ghost">
                📅 Add to Calendar
              </button>
              {room && (
                <Link to={`/book/${room.id}`} className="btn-ghost">🔄 Book Again</Link>
              )}
              {canCancelBooking && (
                <button onClick={() => setCancelOpen(true)} className="btn-ghost text-brick-500">
                  ✕ Cancel Booking
                </button>
              )}
            </div>
            {isTerminal && (
              <p className="mt-3 text-xs text-grove-500">This booking is {b.status.toLowerCase().replaceAll('_', ' ')}. No further actions are available.</p>
            )}
          </div>
        </div>

        {/* Right: Status timeline */}
        <div className="space-y-6">
          <div className="soft-card p-6">
            <h2 className="mb-4 font-display text-xl">Status Timeline</h2>
            {flow.map((item, i) => {
              const isCompleted = i <= idx || (b.status === 'CONFIRMED' && i <= 2)
              const isCurrent = item.key === b.status
              return (
                <div key={item.key} className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <span className={`flex h-4 w-4 items-center justify-center rounded-full ${
                      isCurrent ? 'bg-grove-600 ring-4 ring-grove-200 dark:ring-grove-700' :
                      isCompleted ? 'bg-grove-600' : 'bg-grove-200 dark:bg-grove-700'
                    }`}>
                      {isCompleted && <span className="h-1.5 w-1.5 rounded-full bg-white" />}
                    </span>
                    {i < flow.length - 1 && <span className={`h-10 w-px ${isCompleted ? 'bg-grove-400' : 'bg-grove-200 dark:bg-grove-700'}`} />}
                  </div>
                  <div className="-mt-1 pb-4">
                    <div className={`font-semibold ${isCurrent ? 'text-grove-700 dark:text-grove-200' : isCompleted ? '' : 'text-grove-400'}`}>{item.label}</div>
                    <div className="text-xs text-grove-500">{item.desc}</div>
                  </div>
                </div>
              )
            })}
            {b.status === 'REJECTED' && (
              <div className="mt-2 rounded-xl bg-brick-500/10 p-3 text-sm text-brick-600">
                <strong>Rejected:</strong> {b.rejectionReason || 'No reason provided'}
              </div>
            )}
            {b.status === 'CANCELLED' && (
              <div className="mt-2 rounded-xl bg-grove-100 p-3 text-sm dark:bg-grove-700/30">
                This booking was cancelled.
              </div>
            )}
          </div>

          {/* Approval history */}
          {b.approvals?.length > 0 && (
            <div className="soft-card p-6">
              <h2 className="mb-3 font-display text-xl">Approval History</h2>
              <div className="space-y-2">
                {b.approvals.map((a) => (
                  <div key={a.id} className="flex items-center justify-between text-sm">
                    <div>
                      <span className="font-semibold">{a.approverName || a.requiredRole}</span>
                      {a.comment && <span className="text-grove-500"> — {a.comment}</span>}
                    </div>
                    <StatusBadge status={a.status} />
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Cancel confirmation dialog */}
      <ConfirmDialog
        open={cancelOpen}
        title="Cancel Booking?"
        danger
        confirmLabel="Cancel Booking"
        busy={cancelling}
        onCancel={() => setCancelOpen(false)}
        onConfirm={confirmCancel}
      >
        <p>Are you sure you want to cancel <strong>"{b.title}"</strong> on {formatDate(b.date)}?</p>
        <p className="mt-2 text-xs text-grove-500">The room will be freed and available for others to book. This action cannot be undone.</p>
      </ConfirmDialog>
    </div>
  )
}

function DetailRow({ label, value }) {
  return (
    <div className="flex justify-between border-b border-cream-200 py-2 dark:border-white/10 last:border-0">
      <span className="text-grove-500">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  )
}
