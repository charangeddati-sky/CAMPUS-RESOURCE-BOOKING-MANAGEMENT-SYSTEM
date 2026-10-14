import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cachedGet } from '../api/client'
import CampusMap from '../components/campus/CampusMap'
import { occupancyColor, errorMessage } from '../utils/status'
import { ErrorBanner, PageHeader, Skeleton } from '../components/ui'

export default function Campus() {
  const [buildings, setBuildings] = useState([])
  const [heat, setHeat] = useState([])
  const [live, setLive] = useState(null)
  const [error, setError] = useState('')

  const load = async () => {
    setError('')
    try {
      const r = await cachedGet('/campus/overview')
      setBuildings(r.data.buildings || [])
      setHeat(r.data.heatmap || [])
      setLive(r.data.live || null)
      return
    } catch {
      // Older backends only expose the split endpoints.
    }
    try {
      const [campus, liveRes, heatRes] = await Promise.all([
        cachedGet('/campus'),
        cachedGet('/analytics/live'),
        cachedGet('/analytics/heatmap'),
      ])
      setBuildings(campus.data || [])
      setLive(liveRes.data || null)
      setHeat(heatRes.data || [])
    } catch (err) {
      setError(errorMessage(err, 'Campus map could not load.'))
    }
  }

  useEffect(() => { load() }, [])

  if (!live && !error && buildings.length === 0) {
    return <div className="space-y-4"><Skeleton className="h-24" /><Skeleton className="h-80" /></div>
  }

  return (
    <div className="w-full space-y-6">
      <PageHeader kicker="Digital twin" title="Campus" subtitle="Live availability is computed from bookings, maintenance and blocks — not hardcoded tiles." />
      {error && <ErrorBanner message={error} onRetry={load} />}
      {live && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
          {[
            ['Available', live.available, '#16a34a'],
            ['Occupied', live.occupied, '#dc2626'],
            ['Maintenance', live.maintenance, '#ca8a04'],
            ['Blocked', live.blocked, '#334155'],
            ['Occupancy', `${live.occupancyPercent}%`, '#d4a017'],
          ].map(([l, v, c]) => (
            <div key={l} className="soft-card p-4">
              <div className="text-xs uppercase tracking-wide text-grove-500">{l}</div>
              <div className="font-display text-3xl tabular-nums" style={{ color: c }}>{v}</div>
            </div>
          ))}
        </div>
      )}
      <CampusMap buildings={buildings} heatmap={heat} />
      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        {buildings.map((b) => (
          <Link key={b.id} to={`/buildings/${b.id}`} className="soft-card p-4 transition hover:-translate-y-0.5 hover:shadow-md">
            <div className="flex items-center justify-between">
              <div className="font-display text-xl">{b.name}</div>
              <span className="h-3 w-3 rounded-full" style={{ background: occupancyColor(b.liveStatus) }} />
            </div>
            <div className="text-xs uppercase tracking-widest text-grove-500">{b.virtueName}</div>
            <p className="mt-2 line-clamp-2 text-sm text-grove-600 dark:text-grove-200">{b.description}</p>
            <div className="mt-3 text-sm">{b.availableNow}/{b.resources} available · {b.floors} floors</div>
          </Link>
        ))}
      </div>
    </div>
  )
}
