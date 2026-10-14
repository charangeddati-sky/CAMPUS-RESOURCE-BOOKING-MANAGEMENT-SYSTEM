import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import client, { asItems } from '../api/client'
import { errorMessage } from '../utils/status'

export default function Events() {
  const [events, setEvents] = useState([])
  const [resources, setResources] = useState([])
  const [form, setForm] = useState({
    name: 'Tech event', date: new Date().toISOString().slice(0, 10), startTime: '10:00', endTime: '13:00', expectedAttendees: 80, resourceIds: [], description: '',
  })
  useEffect(() => {
    client.get('/events').then((r) => setEvents(r.data))
    client.get('/resources', { params: { typeCode: 'SEMINAR_HALL', size: 100 } }).then((r) => setResources(asItems(r.data)))
  }, [])
  const submit = async (e) => {
    e.preventDefault()
    try {
      await client.post('/events', form)
      toast.success('Event requested')
      const { data } = await client.get('/events')
      setEvents(data)
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }
  return (
    <div className="grid gap-8 lg:grid-cols-2">
      <form onSubmit={submit} className="soft-card space-y-3 p-5">
        <h1 className="font-display text-3xl">Event booking</h1>
        <input className="w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input type="date" className="w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
        <div className="grid grid-cols-2 gap-2">
          <input type="time" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
          <input type="time" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
        </div>
        <select multiple className="h-32 w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.resourceIds.map(String)} onChange={(e) => setForm({ ...form, resourceIds: [...e.target.selectedOptions].map((o) => Number(o.value)) })}>
          {resources.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
        </select>
        <button className="rounded-full bg-grove-700 px-4 py-2 text-white">Create event</button>
      </form>
      <div className="space-y-2">
        {events.map((ev) => <div key={ev.id} className="soft-card p-4">{ev.name} · {ev.eventDate}</div>)}
      </div>
    </div>
  )
}
