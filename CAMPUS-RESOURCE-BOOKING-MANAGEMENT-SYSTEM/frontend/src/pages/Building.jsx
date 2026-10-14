import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import client from '../api/client'
import Breadcrumbs from '../components/Breadcrumbs'

export default function Building() {
  const { id } = useParams()
  const [data, setData] = useState(null)

  useEffect(() => {
    client.get(`/buildings/${id}`).then((r) => setData(r.data))
  }, [id])

  if (!data) return <div className="soft-card h-64 animate-pulse" />
  const b = data.building

  return (
    <div>
      <Breadcrumbs items={[{ label: 'Campus', to: '/campus' }, { label: b.name }]} />
      <div className="grid gap-6 lg:grid-cols-[1.2fr_.8fr]">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-gold-500">{b.virtueName} · {b.code}</p>
          <h1 className="font-display text-4xl">{b.name}</h1>
          <p className="mt-3 max-w-2xl text-grove-600 dark:text-grove-200">{b.description}</p>
          <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-4">
            <Stat l="Floors" v={b.floors} />
            <Stat l="Resources" v={b.resources} />
            <Stat l="Available" v={b.availableNow} />
            <Stat l="Department" v={b.department || '—'} />
          </div>
        </div>
        <img src="/campus.jpg" alt="" className="h-56 w-full rounded-3xl object-cover" />
      </div>
      <div className="mt-8 grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {data.floors.map((f) => (
          <Link key={f.id} to={`/floors/${f.id}`} className="soft-card p-5 hover:shadow-md">
            <div className="font-display text-2xl">{f.name}</div>
            <div className="mt-3 space-y-1 text-sm">
              <div>{f.classrooms} rooms</div>
              <div>{f.labs} labs</div>
              <div>{f.halls} halls</div>
              {f.libraries > 0 && <div>{f.libraries} library spaces</div>}
              <div className="font-semibold text-grove-700">{f.availableNow} available</div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}

function Stat({ l, v }) {
  return (
    <div className="soft-card p-4">
      <div className="text-xs uppercase text-grove-500">{l}</div>
      <div className="font-display text-2xl">{v}</div>
    </div>
  )
}
