import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import client, { asItems } from '../api/client'
import { errorMessage } from '../utils/status'

export default function Issues() {
  const [rows, setRows] = useState([])
  const [resources, setResources] = useState([])
  const [form, setForm] = useState({ resourceId: '', category: 'PROJECTOR', description: '' })
  useEffect(() => {
    client.get('/issues/my').then((r) => setRows(r.data)).catch(() => {})
    client.get('/resources', { params: { size: 100 } }).then((r) => setResources(asItems(r.data).slice(0, 80)))
  }, [])
  const submit = async (e) => {
    e.preventDefault()
    try {
      await client.post('/issues', form)
      toast.success('Issue reported')
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <form onSubmit={submit} className="soft-card space-y-3 p-5">
        <h1 className="font-display text-3xl">Report an issue</h1>
        <select className="w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.resourceId} onChange={(e) => setForm({ ...form, resourceId: e.target.value })}>
          <option value="">Select resource</option>
          {resources.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
        </select>
        <select className="w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
          {['PROJECTOR','AC','COMPUTER','WIFI','FURNITURE','AUDIO','LIGHTING','OTHER'].map((c) => <option key={c}>{c}</option>)}
        </select>
        <textarea className="w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <button className="rounded-full bg-grove-700 px-4 py-2 text-white">Submit</button>
      </form>
      <div className="space-y-2">{rows.map((i) => <div key={i.id} className="soft-card p-4">{i.category} · {i.status}<div className="text-sm">{i.description}</div></div>)}</div>
    </div>
  )
}
