import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import client, { asItems } from '../api/client'
import { errorMessage } from '../utils/status'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, PageHeader, Spinner } from '../components/ui'

export default function Book() {
  const { resourceId } = useParams()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [buildings, setBuildings] = useState([])
  const [rooms, setRooms] = useState([])
  const [resource, setResource] = useState(null)
  const [timeline, setTimeline] = useState([])
  const [availability, setAvailability] = useState(null)
  const [saving, setSaving] = useState(false)
  const [loadingRooms, setLoadingRooms] = useState(true)
  const [filters, setFilters] = useState({ q: '', typeCode: '', buildingId: '', minCapacity: '' })
  const [form, setForm] = useState({
    date: params.get('date') || new Date(Date.now() + 86400000).toISOString().slice(0, 10),
    startTime: (params.get('start') || '10:00').slice(0, 5),
    endTime: (params.get('end') || '11:00').slice(0, 5),
    purpose: '',
    attendees: 20,
  })

  const loadRooms = () => {
    setLoadingRooms(true)
    client.get('/resources', {
      params: {
        q: filters.q || undefined,
        typeCode: filters.typeCode || undefined,
        buildingId: filters.buildingId || undefined,
        minCapacity: filters.minCapacity || undefined,
      },
    }).then((r) => setRooms(asItems(r.data))).finally(() => setLoadingRooms(false))
  }

  useEffect(() => {
    client.get('/campus').then((r) => setBuildings(r.data.filter((b) => b.bookable)))
  }, [])

  useEffect(() => { loadRooms() }, [filters.q, filters.typeCode, filters.buildingId, filters.minCapacity])

  useEffect(() => {
    const id = resourceId || resource?.id
    if (!id) return
    client.get(`/resources/${id}`, { params: { date: form.date } }).then((r) => {
      setResource(r.data.resource)
      setTimeline(r.data.timeline || [])
    })
  }, [resourceId, resource?.id, form.date])

  useEffect(() => {
    if (!resource) return
    client.get(`/resources/${resource.id}/availability`, {
      params: { date: form.date, startTime: form.startTime, endTime: form.endTime },
    }).then((r) => setAvailability(r.data)).catch(() => setAvailability(null))
  }, [resource, form.date, form.startTime, form.endTime])

  const pickSlot = (hour, available) => {
    if (!available) return
    const start = (hour || '').slice(0, 5)
    const [h, m] = start.split(':').map(Number)
    setForm((f) => ({ ...f, startTime: start, endTime: `${String(h + 1).padStart(2, '0')}:${String(m).padStart(2, '0')}` }))
  }

  // Validation
  const attendeesNum = Number(form.attendees) || 0
  const capacityWarning = resource && attendeesNum > resource.capacity
  const timeValid = form.startTime < form.endTime
  const dateValid = form.date >= new Date().toISOString().slice(0, 10)

  const submit = async () => {
    if (!resource) return toast.error('Pick a room first')
    if (!timeValid) return toast.error('End time must be after start time')
    if (!dateValid) return toast.error('Cannot book in the past')
    if (availability && !availability.available) return toast.error(availability.reason || 'That time slot is taken')
    if (!form.purpose.trim()) return toast.error('Please enter the purpose for your booking')
    setSaving(true)
    try {
      const { data } = await client.post('/bookings', {
        resourceIds: [resource.id],
        date: form.date,
        startTime: form.startTime.slice(0, 5),
        endTime: form.endTime.slice(0, 5),
        title: form.purpose || resource.name,
        purpose: form.purpose,
        attendees: attendeesNum,
      })
      toast.success('Booking created!')
      navigate(`/bookings/${data.id}/confirmation`)
    } catch (err) {
      toast.error(errorMessage(err, 'Could not create booking'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="w-full">
      <PageHeader
        title="Book a Room"
        subtitle="Select a room, choose your time, and book. Availability is checked live."
        actions={
          <div className="flex gap-2">
            <button onClick={() => navigate('/ai-chat')} className="btn-ghost">🤖 AI Assistant</button>
          </div>
        }
      />
      <div className="grid gap-6 xl:grid-cols-[1.05fr_.95fr]">
        {/* Left: Room picker */}
        <section className="soft-card p-4">
          <div className="mb-3 grid gap-2 md:grid-cols-4">
            <input className="field md:col-span-2" placeholder="Search rooms…" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
            <select className="field" value={filters.buildingId} onChange={(e) => setFilters({ ...filters, buildingId: e.target.value })}>
              <option value="">All blocks</option>
              {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
            </select>
            <select className="field" value={filters.typeCode} onChange={(e) => setFilters({ ...filters, typeCode: e.target.value })}>
              <option value="">All types</option>
              <option>CLASSROOM</option><option>LABORATORY</option><option>SEMINAR_HALL</option><option>LIBRARY</option>
            </select>
          </div>
          <div className="max-h-[62vh] space-y-2 overflow-y-auto pr-1">
            {loadingRooms && (
              <div className="flex items-center justify-center py-12">
                <Spinner size={24} className="text-grove-500" />
              </div>
            )}
            {!loadingRooms && rooms.map((r) => (
              <button
                key={r.id}
                onClick={() => { setResource(r); navigate(`/book/${r.id}`, { replace: true }) }}
                className={`w-full rounded-2xl border p-3 text-left transition ${resource?.id === r.id ? 'border-grove-700 bg-grove-50 dark:bg-grove-800' : 'border-cream-200 hover:border-grove-400 dark:border-white/10'}`}
              >
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="font-semibold">{r.name}</div>
                    <div className="text-xs text-grove-500">{r.buildingName} · {r.floorName} · {r.capacity} seats</div>
                  </div>
                  <StatusBadge status={r.status} />
                </div>
              </button>
            ))}
            {!loadingRooms && rooms.length === 0 && (
              <EmptyState title="No rooms match" icon="🔍">
                Adjust your filters or search to find available rooms.
              </EmptyState>
            )}
          </div>
        </section>

        {/* Right: Booking form */}
        <section className="soft-card p-5">
          {!resource && (
            <div className="flex h-64 items-center justify-center text-center">
              <div>
                <div className="mx-auto mb-3 grid h-14 w-14 place-items-center rounded-full bg-grove-100 text-2xl dark:bg-grove-800">📅</div>
                <p className="font-display text-xl">Select a room</p>
                <p className="mt-1 text-sm text-grove-500">Choose from the list to see availability and book.</p>
              </div>
            </div>
          )}
          {resource && (
            <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
              <div>
                <div className="text-xs uppercase tracking-[0.2em] text-grove-500">{resource.buildingName}</div>
                <h2 className="font-display text-3xl">{resource.name}</h2>
                <p className="text-sm">{resource.floorName} · Capacity <strong>{resource.capacity}</strong> seats</p>
              </div>

              <label className="block text-sm font-medium">Date
                <input type="date" className="field mt-1" value={form.date} min={new Date().toISOString().slice(0, 10)} onChange={(e) => setForm({ ...form, date: e.target.value })} />
              </label>

              {!dateValid && (
                <div className="rounded-xl bg-brick-500/10 p-2 text-sm text-brick-600">⚠️ Cannot book in the past. Select a future date.</div>
              )}

              <div>
                <div className="mb-2 text-sm font-medium">Tap a free hour to auto-fill time</div>
                <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
                  {timeline.map((slot) => (
                    <button
                      key={slot.hour}
                      type="button"
                      disabled={!slot.available}
                      onClick={() => pickSlot(slot.hour, slot.available)}
                      className={`rounded-xl px-2 py-2 text-sm transition ${slot.available ? 'bg-green-100 text-green-900 hover:bg-green-200' : 'cursor-not-allowed bg-red-50 text-red-400 line-through'} ${form.startTime === (slot.hour || '').slice(0, 5) ? 'ring-2 ring-grove-700' : ''}`}
                    >
                      {(slot.hour || '').slice(0, 5)}
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <label className="text-sm font-medium">From<input type="time" className="field mt-1" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} /></label>
                <label className="text-sm font-medium">To<input type="time" className="field mt-1" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} /></label>
              </div>

              {!timeValid && (
                <div className="rounded-xl bg-brick-500/10 p-2 text-sm text-brick-600">⚠️ End time must be after start time.</div>
              )}

              {availability && (
                <div className={`rounded-xl p-3 text-sm ${availability.available ? 'bg-green-100 text-green-900' : 'bg-red-100 text-red-800'}`}>
                  {availability.available ? `✅ Free on ${form.date} from ${form.startTime} to ${form.endTime}` : `❌ ${availability.reason}`}
                </div>
              )}

              <label className="block text-sm font-medium">Purpose *
                <input className="field mt-1" placeholder="Why do you need this room?" value={form.purpose} onChange={(e) => setForm({ ...form, purpose: e.target.value })} />
              </label>

              <label className="block text-sm font-medium">Number of attendees
                <input type="number" className="field mt-1" min={1} value={form.attendees} onChange={(e) => setForm({ ...form, attendees: e.target.value })} />
              </label>

              {/* Capacity warning */}
              {capacityWarning && (
                <div className="rounded-xl bg-gold-400/15 p-3 text-sm text-grove-700 dark:text-grove-200">
                  ⚠️ <strong>Capacity warning:</strong> You've entered {attendeesNum} attendees but this room holds {resource.capacity}. Consider selecting a larger room.
                </div>
              )}

              <button
                className="btn-primary w-full py-3 text-base"
                disabled={saving || (availability && !availability.available) || !timeValid || !dateValid || !form.purpose.trim()}
                onClick={submit}
              >
                {saving ? (
                  <span className="flex items-center gap-2"><Spinner size={16} /> Creating booking…</span>
                ) : (
                  '📅 Confirm Booking'
                )}
              </button>

              {(!form.purpose.trim()) && (
                <p className="text-center text-xs text-grove-500">Enter a purpose to enable booking</p>
              )}
            </motion.div>
          )}
        </section>
      </div>
    </div>
  )
}
