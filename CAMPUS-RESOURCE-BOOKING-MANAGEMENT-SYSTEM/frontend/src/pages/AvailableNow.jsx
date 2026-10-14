import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client, { asItems, cachedGet } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, ErrorBanner, PageHeader, Pagination, Skeleton } from '../components/ui'
import { errorMessage } from '../utils/status'

export default function AvailableNow() {
  const [rows, setRows] = useState([])
  const [type, setType] = useState('')
  const [q, setQ] = useState('')
  const [buildingId, setBuildingId] = useState('')
  const [buildings, setBuildings] = useState([])
  const [page, setPage] = useState(0)
  const [meta, setMeta] = useState({ total: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    cachedGet('/campus').then((r) => setBuildings(r.data || [])).catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setError('')
    client.get('/available-now', {
      params: {
        typeCode: type || undefined,
        q: q || undefined,
        buildingId: buildingId || undefined,
        page,
        size: 24,
      },
    }).then((r) => {
      const data = r.data
      setRows(asItems(data))
      setMeta({ total: data.total ?? asItems(data).length, totalPages: data.totalPages ?? 1 })
    }).catch((err) => setError(errorMessage(err, 'Could not load availability.')))
      .finally(() => setLoading(false))
  }, [type, q, buildingId, page])

  const grouped = rows.reduce((acc, r) => {
    acc[r.buildingName] = acc[r.buildingName] || []
    acc[r.buildingName].push(r)
    return acc
  }, {})

  return (
    <div>
      <PageHeader kicker="Live" title="What’s available now?" subtitle="Filtered from the occupancy snapshot — bookings, maintenance and blocks included." />
      <form className="my-4 grid gap-2 md:grid-cols-3" onSubmit={(e) => { e.preventDefault(); setPage(0) }}>
        <input className="field" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} placeholder="Search room, code, block…" aria-label="Search available rooms" />
        <select className="field" value={buildingId} onChange={(e) => { setBuildingId(e.target.value); setPage(0) }} aria-label="Building">
          <option value="">All buildings</option>
          {buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
        <select className="field" value={type} onChange={(e) => { setType(e.target.value); setPage(0) }} aria-label="Resource type">
          <option value="">All types</option>
          <option>CLASSROOM</option><option>LABORATORY</option><option>SEMINAR_HALL</option><option>LIBRARY</option>
        </select>
      </form>
      {error && <ErrorBanner message={error} />}
      {loading && <div className="space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-16" />)}</div>}
      {!loading && rows.length === 0 && (
        <EmptyState title="Nothing free right now" action={<Link to="/book" className="btn-primary">Search another time</Link>}>
          Try another building or type, or book ahead from the wizard.
        </EmptyState>
      )}
      {Object.entries(grouped).map(([b, items]) => (
        <section key={b} className="mb-6">
          <h2 className="font-display text-2xl">{b}</h2>
          <div className="mt-2 grid gap-2 md:grid-cols-2">
            {items.map((r) => (
              <Link key={r.id} to={`/resources/${r.id}`} className="soft-card flex items-center justify-between p-3 transition hover:shadow-md">
                <span>
                  <span className="font-semibold">{r.name}</span>
                  <span className="ml-2 text-xs text-grove-500">cap {r.capacity}</span>
                </span>
                <StatusBadge status={r.status} />
              </Link>
            ))}
          </div>
        </section>
      ))}
      <Pagination page={page} totalPages={meta.totalPages} total={meta.total} onPage={setPage} />
    </div>
  )
}
