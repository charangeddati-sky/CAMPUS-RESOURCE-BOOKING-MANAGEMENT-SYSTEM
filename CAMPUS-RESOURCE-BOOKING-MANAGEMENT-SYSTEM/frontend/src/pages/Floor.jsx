import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'
import Breadcrumbs from '../components/Breadcrumbs'
import FloorMap from '../components/campus/FloorMap'
import StatusBadge from '../components/StatusBadge'

export default function Floor() {
  const { id } = useParams()
  const { role } = useAuth()
  const [data, setData] = useState(null)

  useEffect(() => {
    client.get(`/floors/${id}`).then((r) => setData(r.data))
  }, [id])

  if (!data) return <div className="soft-card h-64 animate-pulse" />
  const f = data.floor

  return (
    <div className="space-y-5">
      <Breadcrumbs items={[
        { label: 'Campus', to: '/campus' },
        { label: f.buildingName, to: `/buildings/${f.buildingId}` },
        { label: f.name },
      ]} />
      <div className="flex items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-4xl">{f.name}</h1>
          <p className="text-sm text-grove-600">{f.classrooms} rooms · {f.labs} labs · {f.halls} halls · {f.availableNow} available</p>
        </div>
        {role === 'ADMIN' && <Link to={`/admin/floors/${id}/editor`} className="rounded-full bg-grove-700 px-4 py-2 text-sm text-white">Edit layout</Link>}
      </div>
      <FloorMap resources={data.resources} />
      <div className="grid gap-3 md:grid-cols-2">
        {data.resources.map((r) => (
          <Link key={r.id} to={`/resources/${r.id}`} className="soft-card flex items-center justify-between p-4">
            <div>
              <div className="font-semibold">{r.name}</div>
              <div className="text-xs text-grove-500">{r.code} · cap {r.capacity}</div>
            </div>
            <StatusBadge status={r.status} />
          </Link>
        ))}
      </div>
    </div>
  )
}
