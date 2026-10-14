import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { Star } from 'lucide-react'
import client from '../api/client'
import Breadcrumbs from '../components/Breadcrumbs'
import FloorMap from '../components/campus/FloorMap'
import StatusBadge from '../components/StatusBadge'


export default function Resource() {
  const { id } = useParams()
  const [detail, setDetail] = useState(null)
  const [floor, setFloor] = useState(null)
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10))

  const load = () => {
    client.get(`/resources/${id}`, { params: { date } }).then((r) => {
      setDetail(r.data)
      client.get(`/floors/${r.data.resource.floorId}`).then((f) => setFloor(f.data))
    })
  }

  useEffect(load, [id, date])

  if (!detail) return <div className="soft-card h-64 animate-pulse" />
  const r = detail.resource

  const favorite = async () => {
    await client.post(`/favorites/${r.id}`)
    toast.success('Favorites updated')
    load()
  }

  return (
    <div className="space-y-6">
      <Breadcrumbs items={[
        { label: 'Campus', to: '/campus' },
        { label: r.buildingName, to: `/buildings/${r.buildingId}` },
        { label: r.floorName, to: `/floors/${r.floorId}` },
        { label: r.name },
      ]} />
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-grove-500">{r.buildingName} · {r.floorName}</p>
          <h1 className="font-display text-4xl">{r.name}</h1>
          <p className="text-sm">{r.code} · {r.typeName} · Capacity {r.capacity}</p>
        </div>
        <div className="flex gap-2">
          <button onClick={favorite} className="rounded-full border px-3 py-2" aria-label="Favorite">
            <Star className={r.favorite ? 'fill-gold-500 text-gold-500' : ''} />
          </button>
          <Link to={`/book/${r.id}`} className="rounded-full bg-grove-700 px-5 py-2 font-semibold text-white">Book now</Link>
        </div>
      </div>
      <StatusBadge status={r.status} />
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="soft-card p-5">
          <h2 className="font-display text-xl">Facilities</h2>
          <ul className="mt-3 grid grid-cols-2 gap-2 text-sm">
            {r.facilities?.map((f) => <li key={f}>✓ {f}</li>)}
            {r.projector && <li>✓ Projector</li>}
            {r.smartBoard && <li>✓ Smart board</li>}
            {r.airConditioned && <li>✓ AC</li>}
            {r.wifi && <li>✓ Wi-Fi</li>}
          </ul>
          <p className="mt-4 text-sm text-grove-600">{r.description}</p>
        </div>
        <div>
          <label className="text-sm font-medium">Availability date
            <input type="date" className="ml-2 rounded-lg border px-2 py-1 dark:bg-grove-800" value={date} onChange={(e) => setDate(e.target.value)} />
          </label>
          <div className="mt-3 space-y-1">
            {detail.timeline.map((slot) => (
              <div key={slot.hour} className="flex items-center gap-3 text-sm">
                <span className="w-16 font-mono">{slot.hour}</span>
                <div className="h-2 flex-1 rounded-full" style={{ background: slot.available ? '#16a34a33' : '#dc262633' }} />
                <span>{slot.available ? 'AVAILABLE' : slot.reason || 'BOOKED'}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      {floor && (
        <div>
          <h2 className="mb-2 font-display text-xl">Floor map</h2>
          <FloorMap resources={floor.resources} selectedId={r.id} />
        </div>
      )}
      <button
        className="rounded-full border px-4 py-2 text-sm"
        onClick={async () => {
          try {
            await client.post('/bookings/waitlist', {
              resourceId: r.id, date, startTime: '10:00', endTime: '12:00', attendees: r.capacity, purpose: 'Waitlist',
            })
            toast.success('Joined waitlist')
          } catch (err) {
            toast.error(err.response?.data?.message || 'Could not join waitlist')
          }
        }}
      >
        Join waitlist
      </button>
    </div>
  )
}
