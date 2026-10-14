import { Link, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Check } from 'lucide-react'
import client from '../api/client'
import { downloadFile } from '../components/AuthImage'
import StatusBadge from '../components/StatusBadge'
import { formatDate, formatTime } from '../utils/status'

export default function Confirmation() {
  const { id } = useParams()
  const [b, setB] = useState(null)

  useEffect(() => {
    client.get(`/bookings/${id}`).then((r) => setB(r.data))
  }, [id])

  if (!b) return <div className="soft-card mx-auto h-64 max-w-lg animate-pulse" />

  const room = b.resources?.[0]

  return (
    <>
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="mx-auto max-w-lg text-center">
        {/* Success check */}
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-600 text-white shadow-lg">
          <Check size={32} strokeWidth={2.5} />
        </div>

        <h1 className="mt-5 font-display text-4xl">Your Booking is Confirmed!</h1>
        <p className="mt-2 text-grove-600 dark:text-grove-200">Your room has been reserved. Here are the details.</p>

        {/* Booking details card */}
        <div className="soft-card mt-6 p-6 text-left">
          <div className="mb-4 flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wide text-grove-500">Booking #{b.id}</span>
            <StatusBadge status={b.status} />
          </div>

          <div className="space-y-3 text-sm">
            <DetailRow label="Room" value={room?.name || b.title} />
            <DetailRow label="Location" value={`${room?.buildingName || '—'} – ${room?.floorName || '—'}`} />
            <DetailRow label="Date" value={formatDate(b.date)} />
            <DetailRow label="Time" value={`${formatTime(b.startTime)} – ${formatTime(b.endTime)}`} />
            {b.attendees > 0 && <DetailRow label="Attendees" value={b.attendees} />}
            {b.purpose && <DetailRow label="Purpose" value={b.purpose} />}
          </div>

          {b.status === 'PENDING_PROFESSOR' && (
            <div className="mt-4 rounded-xl bg-gold-400/10 p-3 text-sm text-grove-700 dark:text-grove-200">
              ⏳ Your booking is awaiting professor approval. You'll be notified once it's reviewed.
            </div>
          )}
          {b.status === 'CONFIRMED' && (
            <div className="mt-4 rounded-xl bg-grove-100 p-3 text-sm text-grove-700 dark:bg-grove-700/30 dark:text-grove-200">
              ✅ Your booking is confirmed! Remember to check in when you arrive.
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <Link to={`/bookings/${b.id}`} className="btn-primary">View Booking</Link>
          <button onClick={() => downloadFile(`/bookings/${b.id}/calendar`, `booking-${b.id}.ics`)} className="btn-ghost">
            📅 Add to Calendar
          </button>
          <Link to="/bookings" className="btn-ghost">My Bookings</Link>
          <Link to="/book" className="btn-ghost">Book Another</Link>
        </div>
      </motion.div>
    </>
  )
}

function DetailRow({ label, value }) {
  return (
    <div className="flex justify-between border-b border-cream-200 pb-2 dark:border-white/10">
      <span className="text-grove-500">{label}</span>
      <span className="font-semibold text-right">{value}</span>
    </div>
  )
}
