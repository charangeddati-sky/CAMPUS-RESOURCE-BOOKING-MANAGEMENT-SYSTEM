import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import client, { asItems } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, ErrorBanner, PageHeader, Pagination, Skeleton } from '../components/ui'
import { errorMessage } from '../utils/status'

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState(params.get('q') || '')
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [meta, setMeta] = useState({ total: 0, totalPages: 0 })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const run = async (query, nextPage = 0) => {
    setLoading(true)
    setError('')
    try {
      const { data } = await client.get('/resources', { params: { q: query || undefined, page: nextPage, size: 24 } })
      setRows(asItems(data))
      setMeta({ total: data.total ?? asItems(data).length, totalPages: data.totalPages ?? 1 })
      setPage(nextPage)
    } catch (err) {
      setError(errorMessage(err, 'Search failed.'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { run(params.get('q') || q, 0) }, [])

  return (
    <div>
      <PageHeader kicker="Catalog" title="Search" subtitle="Names, codes and departments from the campus database." />
      <form className="my-4 flex gap-2" onSubmit={(e) => { e.preventDefault(); setParams({ q }); run(q, 0) }}>
        <input className="field flex-1 rounded-full" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Room 204, Physics Lab, Central Block..." aria-label="Search resources" />
        <button className="btn-primary">Search</button>
      </form>
      {error && <ErrorBanner message={error} onRetry={() => run(q, page)} />}
      {loading && <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16" />)}</div>}
      {!loading && rows.length === 0 && <EmptyState title="No matching resources">Try a room code or building name.</EmptyState>}
      <div className="space-y-2">
        {rows.map((r) => (
          <Link key={r.id} to={`/resources/${r.id}`} className="soft-card flex items-center justify-between p-4 transition hover:shadow-md">
            <div>
              <div className="font-semibold">{r.name}</div>
              <div className="text-xs">{r.buildingName} · {r.floorName} · {r.typeName}</div>
            </div>
            <StatusBadge status={r.status} />
          </Link>
        ))}
      </div>
      <Pagination page={page} totalPages={meta.totalPages} total={meta.total} onPage={(p) => run(q, p)} />
    </div>
  )
}
