import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import client from '../api/client'
import { errorMessage } from '../utils/status'
import { EmptyState } from '../components/ui'
import useReducedMotion from '../hooks/useReducedMotion'

export default function AiBooking() {
  const navigate = useNavigate()
  const reduced = useReducedMotion()
  const [prompt, setPrompt] = useState('Book Classroom 1 tomorrow from 10 AM to 12 PM.')
  const [res, setRes] = useState(null)
  const [loading, setLoading] = useState(false)

  const run = async (e, confirm = false) => {
    e?.preventDefault()
    setLoading(true)
    try {
      let data
      try {
        data = (await client.post('/ai/book', { prompt, confirm })).data
      } catch (err) {
        if (err.response?.status !== 404) throw err
        const found = (await client.post('/ai/interpret', { prompt })).data
        data = {
          action: 'CLARIFY',
          message: found.explanation || 'Pick a room below. Direct natural-language booking needs the updated backend.',
          questions: [],
          intent: found.intent,
          matches: found.recommendations || [],
          booking: null,
          aiAvailable: found.aiAvailable,
        }
      }
      setRes(data)
      if (data.action === 'BOOKED' && data.booking?.id) {
        toast.success('Booking submitted')
        navigate(`/bookings/${data.booking.id}/confirmation`)
      } else if (data.action === 'UNAVAILABLE') {
        toast.error(data.message)
      }
    } catch (err) {
      toast.error(errorMessage(err, 'AI assistant is temporarily unavailable. You can continue with manual booking.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <div className={`overflow-hidden rounded-[2rem] bg-grove-800 p-8 text-cream-50 shadow-xl ${reduced ? '' : 'transition'}`}>
        <p className="text-xs uppercase tracking-[0.25em] text-gold-400">Natural-language booking</p>
        <h1 className="mt-2 font-display text-4xl">Say the room, date and time. We book only what exists in SQL.</h1>
        <form onSubmit={(e) => run(e, false)} className="mt-6">
          <label htmlFor="nl-prompt" className="sr-only">Booking request</label>
          <textarea id="nl-prompt" className="h-28 w-full rounded-2xl bg-white/10 p-4 outline-none ring-0" value={prompt} onChange={(e) => setPrompt(e.target.value)} />
          <div className="mt-4 flex flex-wrap gap-2">
            <button disabled={loading} className="rounded-full bg-gold-500 px-5 py-2 font-semibold text-grove-900">{loading ? 'Working…' : 'Book with natural language'}</button>
            <Link to="/book" className="rounded-full border border-white/20 px-5 py-2 text-sm">Manual booking</Link>
          </div>
        </form>
        {res && !res.aiAvailable && (
          <p className="mt-3 text-sm text-gold-400">Groq is offline. Extraction still uses the campus parser; booking still goes through Spring validation.</p>
        )}
      </div>
      {res && (
        <div className="mt-6 space-y-3">
          <div className={`rounded-2xl border px-4 py-3 text-sm ${res.action === 'BOOKED' ? 'border-grove-600 bg-grove-700/10' : 'border-cream-200 dark:border-white/10'}`}>
            <div className="text-xs font-semibold uppercase tracking-wide text-grove-500">{res.action}</div>
            <p>{res.message}</p>
            {res.questions?.length > 0 && (
              <ul className="mt-2 list-disc pl-5 text-grove-600">
                {res.questions.map((q) => <li key={q}>{q}</li>)}
              </ul>
            )}
            {res.action === 'CONFIRM' && (
              <button className="btn-primary mt-3" onClick={(e) => run(e, true)}>Confirm booking</button>
            )}
          </div>
          <p className="text-xs uppercase tracking-widest text-grove-500">
            {res.intent?.resourceType} · {res.intent?.resourceName || res.intent?.query} · {res.intent?.date} · {res.intent?.startTime}–{res.intent?.endTime}
          </p>
          {(res.matches || []).map((r) => (
            <div key={r.resourceId} className="soft-card flex items-center justify-between gap-3 p-4">
              <div>
                <div className="font-semibold">{r.resourceName} <span className="text-xs">{r.resourceCode}</span></div>
                <div className="text-sm">{r.reason}</div>
              </div>
              <Link to={`/book/${r.resourceId}`} className="btn-primary shrink-0">Open form</Link>
            </div>
          ))}
          {(!res.matches || res.matches.length === 0) && res.action !== 'BOOKED' && (
            <EmptyState title="No matching resource in SQL" action={<Link to="/campus" className="btn-ghost">Campus map</Link>} />
          )}
        </div>
      )}
    </div>
  )
}
