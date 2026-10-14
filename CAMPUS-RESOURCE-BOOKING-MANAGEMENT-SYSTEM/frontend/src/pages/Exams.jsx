import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'
import { errorMessage } from '../utils/status'

export default function Exams() {
  const { role } = useAuth()
  const [exams, setExams] = useState([])
  const [form, setForm] = useState({
    name: 'Mid semester examination', date: new Date().toISOString().slice(0, 10), startTime: '10:00', endTime: '13:00', requiredCapacity: 500,
  })
  const [result, setResult] = useState(null)
  useEffect(() => { client.get('/exams').then((r) => setExams(r.data)) }, [])
  const submit = async (e) => {
    e.preventDefault()
    try {
      const { data } = await client.post('/exams', form)
      setResult(data)
      toast.success('Rooms allocated')
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }
  return (
    <div>
      <h1 className="font-display text-4xl">Examination mode</h1>
      {role === 'ADMIN' && (
        <form onSubmit={submit} className="soft-card mt-4 grid gap-3 p-5 md:grid-cols-2">
          <input className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input type="number" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.requiredCapacity} onChange={(e) => setForm({ ...form, requiredCapacity: Number(e.target.value) })} />
          <input type="date" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
          <div className="grid grid-cols-2 gap-2">
            <input type="time" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
            <input type="time" className="rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          </div>
          <button className="rounded-full bg-grove-700 py-2 text-white md:col-span-2">Auto-allocate rooms</button>
        </form>
      )}
      {result && <pre className="mt-4 whitespace-pre-wrap rounded-2xl bg-grove-900 p-4 text-cream-50">{result.summary}</pre>}
      <div className="mt-4 space-y-2">{exams.map((x) => <div key={x.id} className="soft-card p-4">{x.name} · {x.examDate} · {x.requiredCapacity} seats</div>)}</div>
    </div>
  )
}
