import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import client, { asItems, invalidateApiCache } from '../../api/client'
import StatusBadge from '../../components/StatusBadge'
import { ConfirmDialog, EmptyState, ErrorBanner, PageHeader, Pagination, Skeleton } from '../../components/ui'
import { errorMessage } from '../../utils/status'

const STATUSES = [
  { value: 'AVAILABLE', label: 'Available' },
  { value: 'UNAVAILABLE', label: 'Unavailable' },
  { value: 'MAINTENANCE', label: 'Maintenance' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const emptyForm = () => ({
  name: '',
  code: '',
  typeCode: 'CLASSROOM',
  buildingId: '',
  floorId: '',
  capacity: 40,
  department: '',
  description: '',
  imageUrl: '',
  managementStatus: 'AVAILABLE',
  workingHoursStart: '08:00',
  workingHoursEnd: '18:00',
  facilityCodes: [],
  projector: false,
  smartBoard: false,
  airConditioned: false,
  wifi: true,
  audio: false,
  microphones: false,
  stage: false,
  computers: '',
  studySeats: '',
  readingArea: false,
  openingHours: '',
  equipmentNotes: '',
  softwareNotes: '',
})

function timeValue(v) {
  return (v || '').toString().slice(0, 5)
}

export default function ResourceManager() {
  const [lookups, setLookups] = useState({ buildings: [], floors: [], types: [], facilities: [] })
  const [rows, setRows] = useState([])
  const [meta, setMeta] = useState({ total: 0, totalPages: 0, page: 0 })
  const [filters, setFilters] = useState({ q: '', buildingId: '', typeCode: '', status: '', page: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(() => new Set())
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [formErrors, setFormErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [confirm, setConfirm] = useState(null)
  const [busy, setBusy] = useState(false)

  const floorsForBuilding = useMemo(
    () => lookups.floors.filter((f) => !form.buildingId || String(f.buildingId) === String(form.buildingId)),
    [lookups.floors, form.buildingId]
  )

  const loadLookups = () => client.get('/admin/resources/lookups').then((r) => setLookups(r.data))

  const load = (next = filters) => {
    setLoading(true)
    setError('')
    client.get('/admin/resources', {
      params: {
        q: next.q || undefined,
        buildingId: next.buildingId || undefined,
        typeCode: next.typeCode || undefined,
        status: next.status || undefined,
        page: next.page,
        size: 20,
      },
    }).then((r) => {
      setRows(asItems(r.data))
      setMeta({ total: r.data.total ?? 0, totalPages: r.data.totalPages ?? 0, page: r.data.page ?? 0 })
      setSelected(new Set())
    }).catch((err) => {
      const status = err.response?.status
      if (status === 404) {
        setError('Resource management is not available on the running server. Restart the Spring Boot backend to load the new admin APIs.')
      } else {
        setError(errorMessage(err, 'Could not load resources.'))
      }
    })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadLookups().catch(() => {})
    load(filters)
  }, [])

  const setFilter = (patch) => {
    const next = { ...filters, ...patch, page: patch.page ?? 0 }
    setFilters(next)
    load(next)
  }

  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm())
    setFormErrors({})
    setFormOpen(true)
  }

  const openEdit = (row) => {
    setEditing(row)
    setForm({
      ...emptyForm(),
      name: row.name || '',
      code: row.code || '',
      typeCode: row.typeCode || 'CLASSROOM',
      buildingId: row.buildingId ? String(row.buildingId) : '',
      floorId: row.floorId ? String(row.floorId) : '',
      capacity: row.capacity ?? 0,
      department: row.department || '',
      description: row.description || '',
      imageUrl: row.imageUrl || '',
      managementStatus: row.managementStatus || 'AVAILABLE',
      workingHoursStart: timeValue(row.workingHoursStart) || '08:00',
      workingHoursEnd: timeValue(row.workingHoursEnd) || '18:00',
      facilityCodes: row.facilities || [],
      projector: !!row.projector,
      smartBoard: !!row.smartBoard,
      airConditioned: !!row.airConditioned,
      wifi: row.wifi !== false,
      audio: !!row.audio,
      microphones: !!row.microphones,
      stage: !!row.stage,
      computers: row.computers ?? '',
      studySeats: row.studySeats ?? '',
      readingArea: !!row.readingArea,
      openingHours: row.openingHours || '',
      equipmentNotes: row.equipmentNotes || '',
      softwareNotes: row.softwareNotes || '',
    })
    setFormErrors({})
    setFormOpen(true)
  }

  const validateForm = () => {
    const errs = {}
    if (!form.name.trim()) errs.name = 'Name is required'
    if (!form.floorId) errs.floorId = 'Choose a floor'
    if (!form.typeCode) errs.typeCode = 'Choose a type'
    const cap = Number(form.capacity)
    if (Number.isNaN(cap) || cap < 0 || cap > 5000) errs.capacity = 'Capacity must be between 0 and 5000'
    if (form.workingHoursStart && form.workingHoursEnd && form.workingHoursEnd <= form.workingHoursStart) {
      errs.workingHoursEnd = 'End must be after start'
    }
    if (form.imageUrl && !/^(https?:\/\/|\/)/i.test(form.imageUrl.trim())) {
      errs.imageUrl = 'Use an http(s) URL or a path starting with /'
    }
    if (!STATUSES.some((s) => s.value === form.managementStatus)) errs.managementStatus = 'Choose a valid status'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  const payload = () => ({
    name: form.name.trim(),
    code: form.code.trim() || null,
    typeCode: form.typeCode,
    buildingId: form.buildingId ? Number(form.buildingId) : null,
    floorId: Number(form.floorId),
    capacity: Number(form.capacity),
    department: form.department.trim() || null,
    description: form.description.trim() || null,
    imageUrl: form.imageUrl.trim() || null,
    managementStatus: form.managementStatus,
    workingHoursStart: form.workingHoursStart || null,
    workingHoursEnd: form.workingHoursEnd || null,
    facilityCodes: form.facilityCodes,
    projector: form.projector,
    smartBoard: form.smartBoard,
    airConditioned: form.airConditioned,
    wifi: form.wifi,
    audio: form.audio,
    microphones: form.microphones,
    stage: form.stage,
    computers: form.computers === '' ? null : Number(form.computers),
    studySeats: form.studySeats === '' ? null : Number(form.studySeats),
    readingArea: form.readingArea,
    openingHours: form.openingHours.trim() || null,
    equipmentNotes: form.equipmentNotes.trim() || null,
    softwareNotes: form.softwareNotes.trim() || null,
  })

  const save = async (e) => {
    e.preventDefault()
    if (!validateForm()) return
    setSaving(true)
    try {
      if (editing) {
        await client.put(`/admin/resources/${editing.id}`, payload())
        toast.success('Resource updated')
      } else {
        await client.post('/admin/resources', payload())
        toast.success('Resource created')
      }
      invalidateApiCache()
      setFormOpen(false)
      load(filters)
    } catch (err) {
      toast.error(errorMessage(err, 'Could not save resource.'))
    } finally {
      setSaving(false)
    }
  }

  const runConfirmed = async () => {
    if (!confirm) return
    setBusy(true)
    try {
      await confirm.run()
      invalidateApiCache()
      load(filters)
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setBusy(false)
      setConfirm(null)
    }
  }

  const askDelete = (row) => setConfirm({
    title: `Delete ${row.name}?`,
    danger: true,
    confirmLabel: 'Delete',
    body: row.totalBookings > 0
      ? `This room has ${row.totalBookings} booking record(s). Deletion will be blocked — deactivate it instead.`
      : 'This permanently removes the resource. Existing campus maps and searches will no longer show it.',
    run: async () => {
      await client.delete(`/admin/resources/${row.id}`)
      toast.success('Resource deleted')
    },
  })

  const askStatus = (row, status, label) => setConfirm({
    title: `Mark ${row.name} as ${label}?`,
    danger: status === 'INACTIVE',
    confirmLabel: 'Confirm',
    body: status === 'INACTIVE' && row.upcomingBookings > 0
      ? `There are ${row.upcomingBookings} upcoming booking(s). Deactivation will be blocked until those are cancelled or completed.`
      : `Campus, available-now, analytics and new bookings will use this status immediately.`,
    run: async () => {
      await client.patch(`/admin/resources/${row.id}/status`, { managementStatus: status })
      toast.success(`Status set to ${label}`)
    },
  })

  const selectedIds = [...selected]
  const toggleAll = () => {
    if (selected.size === rows.length) setSelected(new Set())
    else setSelected(new Set(rows.map((r) => r.id)))
  }
  const toggleOne = (id) => {
    const next = new Set(selected)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setSelected(next)
  }

  const bulk = (action, label) => {
    if (selectedIds.length === 0) return
    setConfirm({
      title: `${label} ${selectedIds.length} resource(s)?`,
      danger: action === 'DELETE' || action === 'INACTIVE',
      confirmLabel: label,
      body: 'Each resource is checked individually. Items with booking history cannot be deleted; items with upcoming bookings cannot be deactivated.',
      run: async () => {
        const { data } = await client.post('/admin/resources/bulk', { action, ids: selectedIds })
        if (data.failed === 0) toast.success(`${data.succeeded} updated`)
        else toast.error(`${data.succeeded} updated, ${data.failed} skipped`)
      },
    })
  }

  const toggleFacility = (code) => {
    setForm((f) => {
      const has = f.facilityCodes.includes(code)
      return { ...f, facilityCodes: has ? f.facilityCodes.filter((c) => c !== code) : [...f.facilityCodes, code] }
    })
  }

  const facilityLabel = (code) => lookups.facilities.find((f) => f.code === code)?.name || code

  return (
    <div className="space-y-4">
      <PageHeader
        kicker="Administration"
        title="Resource management"
        subtitle="Add, edit, deactivate or delete rooms, labs and halls. Changes flow through to campus, availability, analytics and booking."
        actions={<button type="button" className="btn-primary" onClick={openCreate}>Add resource</button>}
      />

      <form className="grid gap-2 md:grid-cols-5" onSubmit={(e) => { e.preventDefault(); setFilter({ q: filters.q, page: 0 }) }}>
        <input className="field md:col-span-2" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} placeholder="Search name, code, department…" aria-label="Search resources" />
        <select className="field" value={filters.buildingId} onChange={(e) => setFilter({ buildingId: e.target.value })} aria-label="Building">
          <option value="">All buildings</option>
          {lookups.buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
        <select className="field" value={filters.typeCode} onChange={(e) => setFilter({ typeCode: e.target.value })} aria-label="Type">
          <option value="">All types</option>
          {lookups.types.map((t) => <option key={t.code} value={t.code}>{t.name}</option>)}
        </select>
        <select className="field" value={filters.status} onChange={(e) => setFilter({ status: e.target.value })} aria-label="Status">
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
      </form>

      {selectedIds.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-grove-700/20 bg-grove-700/5 px-4 py-3 text-sm">
          <span className="font-semibold">{selectedIds.length} selected</span>
          <button type="button" className="btn-ghost text-xs" onClick={() => bulk('AVAILABLE', 'Set available')}>Available</button>
          <button type="button" className="btn-ghost text-xs" onClick={() => bulk('UNAVAILABLE', 'Set unavailable')}>Unavailable</button>
          <button type="button" className="btn-ghost text-xs" onClick={() => bulk('MAINTENANCE', 'Set maintenance')}>Maintenance</button>
          <button type="button" className="btn-ghost text-xs" onClick={() => bulk('INACTIVE', 'Deactivate')}>Deactivate</button>
          <button type="button" className="text-xs text-brick-500" onClick={() => bulk('DELETE', 'Delete')}>Delete</button>
        </div>
      )}

      {error && <ErrorBanner message={error} onRetry={() => load(filters)} />}
      {loading && <Skeleton className="h-64" />}
      {!loading && rows.length === 0 && (
        <EmptyState title="No resources match" action={<button className="btn-primary" type="button" onClick={openCreate}>Add resource</button>}>
          Try another filter or create a new room.
        </EmptyState>
      )}

      {!loading && rows.length > 0 && (
        <div className="soft-card overflow-x-auto">
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead>
              <tr className="text-xs uppercase tracking-wide text-grove-500">
                <th className="p-3"><input type="checkbox" checked={selected.size === rows.length} onChange={toggleAll} aria-label="Select all" /></th>
                <th className="p-3">Resource</th>
                <th>Location</th>
                <th>Type</th>
                <th>Cap</th>
                <th>Status</th>
                <th>Live</th>
                <th>Bookings</th>
                <th className="p-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t border-cream-200 dark:border-white/10">
                  <td className="p-3"><input type="checkbox" checked={selected.has(r.id)} onChange={() => toggleOne(r.id)} aria-label={`Select ${r.name}`} /></td>
                  <td className="p-3">
                    <div className="font-semibold">{r.name}</div>
                    <div className="text-xs text-grove-500">{r.code}</div>
                  </td>
                  <td>{r.buildingName}<div className="text-xs text-grove-500">{r.floorName}</div></td>
                  <td>{r.typeName}</td>
                  <td>{r.capacity}</td>
                  <td><StatusBadge status={r.managementStatus} /></td>
                  <td><StatusBadge status={r.liveStatus} /></td>
                  <td>{r.upcomingBookings} upcoming<div className="text-xs text-grove-500">{r.totalBookings} total</div></td>
                  <td className="p-3">
                    <div className="flex flex-wrap justify-end gap-2">
                      <button type="button" className="text-xs font-semibold text-grove-700" onClick={() => openEdit(r)}>Edit</button>
                      {r.managementStatus !== 'INACTIVE' && (
                        <button type="button" className="text-xs text-grove-500" onClick={() => askStatus(r, 'INACTIVE', 'Inactive')}>Deactivate</button>
                      )}
                      {r.managementStatus === 'INACTIVE' && (
                        <button type="button" className="text-xs text-grove-500" onClick={() => askStatus(r, 'AVAILABLE', 'Available')}>Activate</button>
                      )}
                      <button type="button" className="text-xs text-brick-500" onClick={() => askDelete(r)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pagination page={meta.page} totalPages={meta.totalPages} total={meta.total} onPage={(p) => setFilter({ page: p })} />

      {formOpen && (
        <div className="fixed inset-0 z-40 overflow-y-auto bg-black/40 p-4" role="dialog" aria-modal="true" aria-labelledby="resource-form-title">
          <form onSubmit={save} className="mx-auto my-6 max-w-3xl rounded-2xl bg-cream-50 p-5 shadow-xl dark:bg-grove-900">
            <div className="mb-4 flex items-start justify-between gap-3">
              <div>
                <p className="stat-kicker text-gold-500">{editing ? 'Edit resource' : 'New resource'}</p>
                <h2 id="resource-form-title" className="font-display text-3xl">{editing ? editing.name : 'Add a campus resource'}</h2>
              </div>
              <button type="button" className="btn-ghost" onClick={() => setFormOpen(false)}>Close</button>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              <Field label="Name" error={formErrors.name}><input className="field" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
              <Field label="Code (optional)"><input className="field" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="Auto-generated if blank" /></Field>
              <Field label="Type" error={formErrors.typeCode}>
                <select className="field" value={form.typeCode} onChange={(e) => setForm({ ...form, typeCode: e.target.value })}>
                  {lookups.types.map((t) => <option key={t.code} value={t.code}>{t.name}</option>)}
                </select>
              </Field>
              <Field label="Status" error={formErrors.managementStatus}>
                <select className="field" value={form.managementStatus} onChange={(e) => setForm({ ...form, managementStatus: e.target.value })}>
                  {STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              </Field>
              <Field label="Building">
                <select className="field" value={form.buildingId} onChange={(e) => setForm({ ...form, buildingId: e.target.value, floorId: '' })}>
                  <option value="">Any / choose floor</option>
                  {lookups.buildings.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </Field>
              <Field label="Floor / location" error={formErrors.floorId}>
                <select className="field" value={form.floorId} onChange={(e) => setForm({ ...form, floorId: e.target.value, buildingId: lookups.floors.find((f) => String(f.id) === e.target.value)?.buildingId || form.buildingId })} required>
                  <option value="">Select floor</option>
                  {floorsForBuilding.map((f) => <option key={f.id} value={f.id}>{f.buildingName} · {f.name}</option>)}
                </select>
              </Field>
              <Field label="Capacity" error={formErrors.capacity}><input type="number" min="0" max="5000" className="field" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} required /></Field>
              <Field label="Department"><input className="field" value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} /></Field>
              <Field label="Opens"><input type="time" className="field" value={form.workingHoursStart} onChange={(e) => setForm({ ...form, workingHoursStart: e.target.value })} /></Field>
              <Field label="Closes" error={formErrors.workingHoursEnd}><input type="time" className="field" value={form.workingHoursEnd} onChange={(e) => setForm({ ...form, workingHoursEnd: e.target.value })} /></Field>
              <Field label="Image URL" error={formErrors.imageUrl} className="md:col-span-2">
                <input className="field" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://… or /campus.jpg" />
                {form.imageUrl && /^(https?:\/\/|\/)/i.test(form.imageUrl) && (
                  <img src={form.imageUrl} alt="" className="mt-2 h-28 w-full rounded-xl object-cover" />
                )}
              </Field>
              <Field label="Description" className="md:col-span-2">
                <textarea className="field min-h-24" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </Field>
            </div>
            <div className="mt-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-grove-500">Facilities</div>
              <div className="mt-2 flex flex-wrap gap-2">
                {lookups.facilities.map((f) => (
                  <label key={f.code} className={`cursor-pointer rounded-full border px-3 py-1 text-xs ${form.facilityCodes.includes(f.code) ? 'border-grove-700 bg-grove-700 text-white' : 'border-cream-200 dark:border-white/10'}`}>
                    <input type="checkbox" className="sr-only" checked={form.facilityCodes.includes(f.code)} onChange={() => toggleFacility(f.code)} />
                    {f.name}
                  </label>
                ))}
              </div>
            </div>
            <div className="mt-4 grid gap-3 md:grid-cols-4">
              {[['projector', 'Projector'], ['smartBoard', 'Smart board'], ['airConditioned', 'AC'], ['wifi', 'Wi-Fi'], ['audio', 'Audio'], ['microphones', 'Mics'], ['stage', 'Stage'], ['readingArea', 'Reading area']].map(([key, label]) => (
                <label key={key} className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={!!form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.checked })} />
                  {label}
                </label>
              ))}
              <Field label="Computers"><input type="number" min="0" className="field" value={form.computers} onChange={(e) => setForm({ ...form, computers: e.target.value })} /></Field>
              <Field label="Study seats"><input type="number" min="0" className="field" value={form.studySeats} onChange={(e) => setForm({ ...form, studySeats: e.target.value })} /></Field>
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" className="btn-ghost" onClick={() => setFormOpen(false)}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving…' : (editing ? 'Save changes' : 'Create resource')}</button>
            </div>
          </form>
        </div>
      )}

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

function Field({ label, error, className = '', children }) {
  return (
    <label className={`block text-xs ${className}`}>
      <span className="font-semibold uppercase tracking-wide text-grove-500">{label}</span>
      <div className="mt-1">{children}</div>
      {error && <span className="text-brick-500">{error}</span>}
    </label>
  )
}
