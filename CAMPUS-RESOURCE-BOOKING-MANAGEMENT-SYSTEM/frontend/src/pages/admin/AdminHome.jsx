import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import client from '../../api/client'
import { downloadFile } from '../../components/AuthImage'
import { ConfirmDialog, EmptyState, PageHeader, Skeleton } from '../../components/ui'
import { errorMessage } from '../../utils/status'
import AnalyticsDashboard from './AnalyticsDashboard'
import ResourceManager from './ResourceManager'

const TABS = ['overview', 'buildings', 'resources', 'floors', 'users', 'issues', 'maintenance', 'reports', 'insights']

export default function AdminHome() {
  const [tab, setTab] = useState('overview')
  const [users, setUsers] = useState([])
  const [issues, setIssues] = useState([])
  const [maint, setMaint] = useState([])
  const [floors, setFloors] = useState([])
  const [buildings, setBuildings] = useState([])
  const [insight, setInsight] = useState('')
  const [q, setQ] = useState('Which building is most utilized?')
  const [from, setFrom] = useState(new Date(Date.now() - 14 * 86400000).toISOString().slice(0, 10))
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10))
  const [loadingTab, setLoadingTab] = useState(false)

  // Building/Floor management state
  const [buildingForm, setBuildingForm] = useState(null) // null = closed, {} = new, {id:..} = editing
  const [floorForm, setFloorForm] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const [busy, setBusy] = useState(false)
  const [expandedBuilding, setExpandedBuilding] = useState(null)
  const [buildingFloors, setBuildingFloors] = useState({})

  const loadBuildings = async () => {
    setLoadingTab(true)
    try {
      const { data } = await client.get('/admin/buildings')
      setBuildings(data || [])
    } catch {
      // Fallback to public campus API
      try {
        const { data } = await client.get('/campus')
        setBuildings(data || [])
      } catch (err) {
        toast.error(errorMessage(err, 'Could not load buildings'))
      }
    } finally {
      setLoadingTab(false)
    }
  }

  const loadFloorsForBuilding = async (buildingId) => {
    try {
      const { data } = await client.get(`/admin/buildings/${buildingId}/floors`)
      setBuildingFloors((prev) => ({ ...prev, [buildingId]: data || [] }))
    } catch {
      try {
        const { data } = await client.get(`/buildings/${buildingId}`)
        setBuildingFloors((prev) => ({ ...prev, [buildingId]: data.floors || [] }))
      } catch (err) {
        toast.error(errorMessage(err))
      }
    }
  }

  useEffect(() => {
    if (tab === 'buildings') {
      loadBuildings()
    }
    if (tab === 'floors') {
      setLoadingTab(true)
      client.get('/floors').then((r) => setFloors(r.data || [])).catch(async () => {
        const campus = await client.get('/campus')
        const all = []
        for (const b of campus.data || []) {
          const d = await client.get(`/buildings/${b.id}`)
          ;(d.data.floors || []).forEach((f) => all.push(f))
        }
        setFloors(all)
      }).finally(() => setLoadingTab(false))
    }
    if (tab === 'users') {
      setLoadingTab(true)
      client.get('/admin/users').then((r) => setUsers(r.data)).finally(() => setLoadingTab(false))
    }
    if (tab === 'issues') {
      setLoadingTab(true)
      client.get('/issues').then((r) => setIssues(r.data)).finally(() => setLoadingTab(false))
    }
    if (tab === 'maintenance') {
      setLoadingTab(true)
      client.get('/admin/maintenance').then((r) => setMaint(r.data)).finally(() => setLoadingTab(false))
    }
  }, [tab])

  const toggleBuilding = (id) => {
    if (expandedBuilding === id) {
      setExpandedBuilding(null)
    } else {
      setExpandedBuilding(id)
      if (!buildingFloors[id]) loadFloorsForBuilding(id)
    }
  }

  const saveBuilding = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      if (buildingForm.id) {
        await client.put(`/admin/buildings/${buildingForm.id}`, buildingForm)
        toast.success('Building updated successfully')
      } else {
        await client.post('/admin/buildings', buildingForm)
        toast.success('Building created successfully')
      }
      setBuildingForm(null)
      loadBuildings()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save building'))
    } finally {
      setBusy(false)
    }
  }

  const saveFloor = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      if (floorForm.id) {
        await client.put(`/admin/floors/${floorForm.id}`, floorForm)
        toast.success('Floor updated successfully')
      } else {
        await client.post(`/admin/buildings/${floorForm.buildingId}/floors`, floorForm)
        toast.success('Floor added successfully')
      }
      setFloorForm(null)
      if (floorForm.buildingId) loadFloorsForBuilding(floorForm.buildingId)
      loadBuildings()
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save floor'))
    } finally {
      setBusy(false)
    }
  }

  const runConfirmed = async () => {
    if (!confirm) return
    setBusy(true)
    try {
      await confirm.run()
      loadBuildings()
      if (confirm.buildingId) loadFloorsForBuilding(confirm.buildingId)
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
      setConfirm(null)
    }
  }

  const askDeleteBuilding = (b) => setConfirm({
    title: `Delete ${b.name}?`,
    danger: true,
    confirmLabel: 'Delete Building',
    body: 'This will permanently remove the building and all its empty floors. Buildings with rooms cannot be deleted.',
    run: async () => {
      await client.delete(`/admin/buildings/${b.id}`)
      toast.success('Building deleted successfully')
    },
  })

  const askDeleteFloor = (f, buildingId) => setConfirm({
    title: `Delete ${f.name}?`,
    danger: true,
    confirmLabel: 'Delete Floor',
    body: 'This will permanently remove the floor. Floors with rooms cannot be deleted — remove all rooms first.',
    buildingId,
    run: async () => {
      await client.delete(`/admin/floors/${f.id}`)
      toast.success('Floor removed successfully')
    },
  })

  const ask = async (e) => {
    e.preventDefault()
    const { data } = await client.post('/ai/insights', { question: q })
    setInsight(data.reply)
  }

  return (
    <div className="w-full">
      {tab !== 'overview' && tab !== 'resources' && tab !== 'buildings' && (
        <PageHeader kicker="Administration" title="Campus Operating Console" subtitle="Analytics, floors, people and exports. Occupancy and charts come from SQL." />
      )}
      <div className="my-4 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)} className={`rounded-full px-4 py-1.5 text-sm capitalize transition ${tab === t ? 'bg-grove-700 text-white shadow-sm' : 'bg-white hover:bg-grove-50 dark:bg-grove-800 dark:hover:bg-grove-700'}`}>{t}</button>
        ))}
      </div>

      {/* Overview tab */}
      {tab === 'overview' && <AnalyticsDashboard />}

      {/* Resources tab */}
      {tab === 'resources' && <ResourceManager />}

      {/* ────── Buildings & Floors Tab ────── */}
      {tab === 'buildings' && (
        <div className="space-y-4">
          <PageHeader
            kicker="Administration"
            title="Buildings & Floors"
            subtitle="Manage the campus hierarchy. Buildings contain floors, floors contain rooms."
            actions={<button type="button" className="btn-primary" onClick={() => setBuildingForm({ name: '', code: '', virtueName: '', description: '', bookable: true })}>+ Add Building</button>}
          />

          {loadingTab && <Skeleton className="h-64" />}

          {!loadingTab && buildings.length === 0 && (
            <EmptyState title="No buildings yet" icon="🏢" action={<button className="btn-primary" onClick={() => setBuildingForm({ name: '', code: '', virtueName: '', description: '', bookable: true })}>+ Add Building</button>}>
              Create your first building to start organizing campus resources.
            </EmptyState>
          )}

          {!loadingTab && buildings.map((b) => (
            <div key={b.id} className="soft-card overflow-hidden">
              {/* Building header */}
              <div className="flex items-center justify-between gap-3 p-5 cursor-pointer hover:bg-grove-50/50 dark:hover:bg-grove-800/50 transition" onClick={() => toggleBuilding(b.id)}>
                <div className="flex items-center gap-3">
                  <span className="grid h-10 w-10 place-items-center rounded-xl bg-grove-700/10 text-lg font-bold text-grove-700 dark:bg-grove-700/30 dark:text-grove-200">🏢</span>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-display text-xl">{b.name}</span>
                      <span className="rounded-full bg-cream-100 px-2 py-0.5 text-xs text-grove-500 dark:bg-grove-700">{b.code}</span>
                      {b.virtueName && <span className="text-xs text-grove-500">{b.virtueName}</span>}
                    </div>
                    <div className="text-xs text-grove-500">{b.floors || '?'} floors · {b.resources || b.availableNow || '?'} resources{b.department ? ` · ${b.department}` : ''}</div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={(e) => { e.stopPropagation(); setBuildingForm({ ...b }) }} className="btn-ghost text-xs">Edit</button>
                  <button onClick={(e) => { e.stopPropagation(); askDeleteBuilding(b) }} className="btn-ghost text-xs text-brick-500">Delete</button>
                  <span className={`text-sm transition ${expandedBuilding === b.id ? 'rotate-90' : ''}`}>▶</span>
                </div>
              </div>

              {/* Expanded floors */}
              {expandedBuilding === b.id && (
                <div className="border-t border-cream-200 bg-cream-50/50 dark:border-white/10 dark:bg-grove-900/50">
                  <div className="px-5 py-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold uppercase tracking-wide text-grove-500">Floors in {b.name}</span>
                      <button onClick={() => setFloorForm({ buildingId: b.id, name: '', level: (buildingFloors[b.id]?.length || 0), description: '' })} className="rounded-full bg-grove-700 px-3 py-1 text-xs text-white">+ Add Floor</button>
                    </div>
                  </div>

                  {!buildingFloors[b.id] && <div className="px-5 pb-4"><Skeleton className="h-12" /></div>}

                  {buildingFloors[b.id]?.length === 0 && (
                    <div className="px-5 pb-4 text-center text-sm text-grove-500">
                      No floors in this building yet. <button onClick={() => setFloorForm({ buildingId: b.id, name: 'Ground Floor', level: 0, description: '' })} className="text-grove-700 underline dark:text-grove-200">Add the first floor</button>
                    </div>
                  )}

                  {buildingFloors[b.id]?.map((f) => (
                    <div key={f.id} className="flex items-center justify-between border-t border-cream-200/60 px-5 py-3 dark:border-white/5">
                      <div className="flex items-center gap-3">
                        <span className="grid h-8 w-8 place-items-center rounded-lg bg-grove-100 text-sm font-bold text-grove-600 dark:bg-grove-700">L{f.level}</span>
                        <div>
                          <div className="font-semibold">{f.name}</div>
                          <div className="text-xs text-grove-500">
                            {f.totalResources != null ? `${f.totalResources} rooms · ${f.availableNow || 0} available` : `Level ${f.level}`}
                            {f.description ? ` · ${f.description}` : ''}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Link to={`/floors/${f.id}`} className="text-xs text-grove-600 hover:underline">View</Link>
                        <Link to={`/admin/floors/${f.id}/editor`} className="text-xs text-grove-600 hover:underline">Layout</Link>
                        <button onClick={() => setFloorForm({ ...f, buildingId: b.id })} className="text-xs text-grove-600 hover:underline">Edit</button>
                        <button onClick={() => askDeleteFloor(f, b.id)} className="text-xs text-brick-500 hover:underline">Delete</button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Floors tab (legacy) */}
      {tab === 'floors' && (
        loadingTab ? <Skeleton className="h-40" /> : (
          <div className="grid gap-2 md:grid-cols-2">
            {floors.map((f) => (
              <Link key={f.id} to={`/admin/floors/${f.id}/editor`} className="soft-card p-4 hover:shadow-md transition">{f.buildingName} · {f.name} · {f.availableNow}/{f.totalResources} free</Link>
            ))}
            {floors.length === 0 && <EmptyState title="No floors found" icon="🏗️">Floors will appear once buildings are set up.</EmptyState>}
          </div>
        )
      )}

      {/* Users tab */}
      {tab === 'users' && (
        loadingTab ? <Skeleton className="h-40" /> : (
          <div className="overflow-x-auto soft-card">
            <table className="w-full text-sm">
              <thead><tr className="text-xs uppercase tracking-wide text-grove-500"><th className="p-3 text-left">Name</th><th className="p-3 text-left">Email</th><th className="p-3 text-left">Role</th><th className="p-3 text-left">Department</th></tr></thead>
              <tbody>{users.map((u) => <tr key={u.id} className="border-t border-cream-200 dark:border-white/10"><td className="p-3 font-semibold">{u.fullName}</td><td className="p-3">{u.email}</td><td className="p-3"><span className="rounded-full bg-grove-100 px-2 py-0.5 text-xs dark:bg-grove-700">{u.role}</span></td><td className="p-3 text-grove-500">{u.department || '—'}</td></tr>)}</tbody>
            </table>
            {users.length === 0 && <div className="p-8 text-center text-grove-500">No users found.</div>}
          </div>
        )
      )}

      {/* Issues tab */}
      {tab === 'issues' && (
        issues.length === 0 ? <EmptyState title="No issues reported" icon="✅">All issues have been resolved.</EmptyState> :
        issues.map((i) => (
          <div key={i.id} className="soft-card mb-2 flex items-center justify-between p-4">
            <div><span className="rounded-full bg-gold-400/20 px-2 py-0.5 text-xs font-semibold">{i.category}</span> · <span className="text-xs">{i.status}</span><div className="mt-1 text-sm text-grove-600">{i.description}</div></div>
            {i.status !== 'RESOLVED' && <button className="btn-ghost text-xs" onClick={async () => { await client.put(`/issues/${i.id}`, { status: 'RESOLVED', resolution: 'Fixed' }); toast.success('Issue resolved'); setIssues((prev) => prev.map((x) => x.id === i.id ? { ...x, status: 'RESOLVED' } : x)) }}>Resolve</button>}
          </div>
        ))
      )}

      {/* Maintenance tab */}
      {tab === 'maintenance' && (
        maint.length === 0 ? <EmptyState title="No maintenance scheduled" icon="🔧">All systems operational.</EmptyState> :
        maint.map((m) => (
          <div key={m.id} className="soft-card mb-2 p-4">
            <div className="font-semibold">{m.reason}</div>
            <div className="text-sm text-grove-500">{m.startDate} – {m.endDate}</div>
          </div>
        ))
      )}

      {/* Reports tab */}
      {tab === 'reports' && (
        <div className="soft-card space-y-4 p-5">
          <h2 className="font-display text-xl">Generate Reports</h2>
          <p className="text-sm text-grove-600">Professional reports include KPIs, utilization, forecasts, and the booking register for the selected date range.</p>
          <div className="flex flex-wrap gap-3">
            <label className="text-xs font-semibold">From<input type="date" className="field mt-1" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
            <label className="text-xs font-semibold">To<input type="date" className="field mt-1" value={to} onChange={(e) => setTo(e.target.value)} /></label>
          </div>
          <div className="flex flex-wrap gap-3">
            <button className="btn-primary" onClick={() => downloadFile(`/admin/reports/csv?from=${from}&to=${to}`, 'bookings.csv')}>📊 CSV</button>
            <button className="btn-primary" onClick={() => downloadFile(`/admin/reports/excel?from=${from}&to=${to}`, 'bookings.xlsx')}>📗 Excel</button>
            <button className="btn-primary" onClick={() => downloadFile(`/admin/reports/pdf?from=${from}&to=${to}`, `campusos-analytics-${from}-to-${to}.pdf`)}>📄 PDF Report</button>
          </div>
        </div>
      )}

      {/* Insights tab */}
      {tab === 'insights' && (
        <form onSubmit={ask} className="soft-card space-y-3 p-5">
          <h2 className="font-display text-xl">AI Insights</h2>
          <p className="text-sm text-grove-500">Ask questions about campus usage, trends, and patterns.</p>
          <input className="field" value={q} onChange={(e) => setQ(e.target.value)} placeholder="e.g. Which building is most utilized?" />
          <button className="btn-primary">Ask CampusOS</button>
          {insight && <div className="rounded-2xl bg-cream-100 p-4 text-sm leading-relaxed dark:bg-grove-800">{insight}</div>}
        </form>
      )}

      {/* ──── Building Form Modal ──── */}
      {buildingForm && (
        <div className="fixed inset-0 z-40 overflow-y-auto bg-black/40 p-4 animate-fade-in" role="dialog" aria-modal="true" onClick={() => setBuildingForm(null)}>
          <form onSubmit={saveBuilding} className="mx-auto my-8 max-w-lg rounded-2xl bg-cream-50 p-6 shadow-xl dark:bg-grove-900 animate-modal-enter" onClick={(e) => e.stopPropagation()}>
            <h2 className="font-display text-2xl">{buildingForm.id ? 'Edit Building' : 'Add Building'}</h2>
            <div className="mt-4 space-y-3">
              <label className="block text-xs font-semibold uppercase text-grove-500">Name *
                <input className="field mt-1" required value={buildingForm.name || ''} onChange={(e) => setBuildingForm({ ...buildingForm, name: e.target.value })} />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Code *
                <input className="field mt-1" required value={buildingForm.code || ''} onChange={(e) => setBuildingForm({ ...buildingForm, code: e.target.value })} placeholder="e.g. LOYALTY1" />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Virtue Name
                <input className="field mt-1" value={buildingForm.virtueName || ''} onChange={(e) => setBuildingForm({ ...buildingForm, virtueName: e.target.value })} placeholder="e.g. Loyalty" />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Department
                <input className="field mt-1" value={buildingForm.department || ''} onChange={(e) => setBuildingForm({ ...buildingForm, department: e.target.value })} />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Description
                <textarea className="field mt-1 min-h-20" value={buildingForm.description || ''} onChange={(e) => setBuildingForm({ ...buildingForm, description: e.target.value })} />
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={buildingForm.bookable !== false} onChange={(e) => setBuildingForm({ ...buildingForm, bookable: e.target.checked })} />
                Bookable
              </label>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" className="btn-ghost" onClick={() => setBuildingForm(null)}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={busy}>{busy ? 'Saving…' : (buildingForm.id ? 'Save Changes' : 'Create Building')}</button>
            </div>
          </form>
        </div>
      )}

      {/* ──── Floor Form Modal ──── */}
      {floorForm && (
        <div className="fixed inset-0 z-40 overflow-y-auto bg-black/40 p-4 animate-fade-in" role="dialog" aria-modal="true" onClick={() => setFloorForm(null)}>
          <form onSubmit={saveFloor} className="mx-auto my-8 max-w-md rounded-2xl bg-cream-50 p-6 shadow-xl dark:bg-grove-900 animate-modal-enter" onClick={(e) => e.stopPropagation()}>
            <h2 className="font-display text-2xl">{floorForm.id ? 'Edit Floor' : 'Add Floor'}</h2>
            <div className="mt-4 space-y-3">
              <label className="block text-xs font-semibold uppercase text-grove-500">Name *
                <input className="field mt-1" required value={floorForm.name || ''} onChange={(e) => setFloorForm({ ...floorForm, name: e.target.value })} placeholder="e.g. Ground Floor" />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Level *
                <input type="number" className="field mt-1" required min="0" max="50" value={floorForm.level ?? 0} onChange={(e) => setFloorForm({ ...floorForm, level: parseInt(e.target.value, 10) })} />
              </label>
              <label className="block text-xs font-semibold uppercase text-grove-500">Description
                <textarea className="field mt-1" value={floorForm.description || ''} onChange={(e) => setFloorForm({ ...floorForm, description: e.target.value })} />
              </label>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" className="btn-ghost" onClick={() => setFloorForm(null)}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={busy}>{busy ? 'Saving…' : (floorForm.id ? 'Save Changes' : 'Add Floor')}</button>
            </div>
          </form>
        </div>
      )}

      {/* Confirm dialog */}
      <ConfirmDialog
        open={!!confirm}
        title={confirm?.title}
        danger={confirm?.danger}
        confirmLabel={confirm?.confirmLabel}
        busy={busy}
        onCancel={() => setConfirm(null)}
        onConfirm={runConfirmed}
      >
        {confirm?.body}
      </ConfirmDialog>
    </div>
  )
}
