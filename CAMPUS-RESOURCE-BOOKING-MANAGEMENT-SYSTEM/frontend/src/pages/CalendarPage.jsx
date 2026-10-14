import { useEffect, useMemo, useState } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import { PageHeader } from '../components/ui'

function toHHmm(date) {
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function localDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export default function CalendarPage() {
  const [events, setEvents] = useState([])
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const load = () => {
    const from = new Date()
    from.setMonth(from.getMonth() - 1)
    const to = new Date()
    to.setMonth(to.getMonth() + 2)
    client.get('/calendar/events', {
      params: { from: localDate(from), to: localDate(to) },
    }).then((r) => {
      setEvents((r.data || []).map((e) => ({
        id: String(e.id),
        title: e.title,
        start: e.start,
        end: e.end,
        backgroundColor: e.color,
        borderColor: e.color,
        extendedProps: e,
      })))
      setError('')
    }).catch((err) => setError(err.response?.data?.message || 'Could not load calendar'))
  }

  useEffect(load, [])

  const legend = useMemo(() => ([
    ['#146c4a', 'Confirmed'],
    ['#ea580c', 'Pending'],
    ['#0f3d2e', 'Checked in'],
    ['#64748b', 'Completed'],
  ]), [])

  return (
    <div>
      <PageHeader
        kicker="Schedule"
        title="Campus calendar"
        subtitle="Click an empty slot between 08:00 and 18:00 to start a booking. Click an event to open it."
      />
      <div className="mb-4 flex flex-wrap gap-3 text-xs">
        {legend.map(([c, l]) => (
          <span key={l} className="inline-flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-full" style={{ background: c }} />{l}</span>
        ))}
      </div>
      {error && <div className="mb-3 rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      <div className="soft-card p-3 md:p-5">
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
          initialView="timeGridWeek"
          headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' }}
          events={events}
          height="auto"
          slotMinTime="08:00:00"
          slotMaxTime="18:00:00"
          allDaySlot={false}
          nowIndicator
          selectable
          selectMirror
          eventClick={(info) => navigate(`/bookings/${info.event.id}`)}
          dateClick={(info) => {
            const d = info.date
            navigate(`/book?date=${localDate(d)}&start=${toHHmm(d)}`)
          }}
          select={(info) => {
            const d = info.start
            const end = info.end
            navigate(`/book?date=${localDate(d)}&start=${toHHmm(d)}&end=${toHHmm(end)}`)
          }}
        />
      </div>
    </div>
  )
}
