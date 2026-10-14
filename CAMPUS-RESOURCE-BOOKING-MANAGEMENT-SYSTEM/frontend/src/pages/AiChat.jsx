import { useCallback, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import client from '../api/client'
import { errorMessage, formatDate, formatTime } from '../utils/status'
import { EmptyState, PageHeader, Spinner, SuccessDialog } from '../components/ui'

const sparks = [
  'Book a classroom for 50 people tomorrow from 10 AM to 12 PM.',
  'Find a room with projector in Loyalty 1.',
  'What rooms are available now?',
  'Show my upcoming bookings.',
  'I need a seminar hall for 100 people.',
  'Cancel my last booking.',
]

/** Build a consolidated prompt from accumulated slots + new message */
function buildPrompt(slots, message) {
  const parts = []
  if (message) parts.push(message)
  // Append accumulated context
  const ctx = []
  if (slots.building) ctx.push(`building: ${slots.building}`)
  if (slots.capacity) ctx.push(`capacity: ${slots.capacity}`)
  if (slots.date) ctx.push(`date: ${slots.date}`)
  if (slots.startTime && slots.endTime) ctx.push(`time: ${slots.startTime} to ${slots.endTime}`)
  if (slots.resourceType) ctx.push(`type: ${slots.resourceType}`)
  if (slots.facilities?.length) ctx.push(`facilities: ${slots.facilities.join(', ')}`)
  if (slots.resourceName) ctx.push(`room: ${slots.resourceName}`)
  if (ctx.length) parts.push(`[context: ${ctx.join(', ')}]`)
  return parts.join(' ')
}

/** Merge intent slots from AI response into accumulated context */
function mergeSlots(prev, intent) {
  if (!intent) return prev
  const next = { ...prev }
  if (intent.building) next.building = intent.building
  if (intent.capacity) next.capacity = intent.capacity
  if (intent.date) next.date = intent.date
  if (intent.startTime) next.startTime = intent.startTime
  if (intent.endTime) next.endTime = intent.endTime
  if (intent.resourceType) next.resourceType = intent.resourceType
  if (intent.resourceName) next.resourceName = intent.resourceName
  if (intent.requiredFacilities?.length) next.facilities = intent.requiredFacilities
  return next
}

export default function AiChat() {
  const navigate = useNavigate()
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState([])
  const [recs, setRecs] = useState([])
  const [loading, setLoading] = useState(false)
  const [slots, setSlots] = useState({})
  const [pendingConfirm, setPendingConfirm] = useState(null)
  const [successBooking, setSuccessBooking] = useState(null)
  const bottomRef = useRef(null)
  const inputRef = useRef(null)

  const scrollToBottom = useCallback(() => {
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
  }, [])

  const addMessage = useCallback((msg) => {
    setMessages((m) => [...m, msg])
    scrollToBottom()
  }, [scrollToBottom])

  const send = async (text) => {
    const message = (text || input).trim()
    if (!message || loading) return
    setInput('')
    addMessage({ role: 'user', content: message })
    setLoading(true)

    const lower = message.toLowerCase()
    const isBookingIntent = /\b(book|reserve|schedule)\b/.test(lower)
    const isShowBookings = /\b(my booking|show booking|upcoming|cancel.*booking)\b/.test(lower)

    try {
      if (isBookingIntent || pendingConfirm) {
        // Use the /ai/book endpoint for booking intents
        const isConfirmMessage = /\b(yes|confirm|go ahead|book it|do it|sure|ok)\b/i.test(message)
        const confirm = isConfirmMessage && pendingConfirm ? true : false

        let prompt = buildPrompt(slots, message)
        if (confirm && pendingConfirm?.intent) {
          const it = pendingConfirm.intent
          prompt = `Book ${it.resourceName || slots.resourceName || 'room'} on ${it.date || slots.date} from ${it.startTime || slots.startTime} to ${it.endTime || slots.endTime} with ${it.capacity || slots.capacity || 1} attendees`
        }

        let data
        try {
          const res = await client.post('/ai/book', { prompt, confirm })
          data = res.data
        } catch (err) {
          if (err.response?.status === 404) {
            // Fallback to interpret
            const found = (await client.post('/ai/interpret', { prompt })).data
            data = {
              action: 'CLARIFY',
              message: found.explanation || 'I found some matches. You can book directly from the list.',
              questions: [],
              intent: found.intent,
              matches: found.recommendations || [],
              booking: null,
              aiAvailable: found.aiAvailable,
            }
          } else throw err
        }

        // Merge slots from response
        if (data.intent) {
          setSlots((prev) => mergeSlots(prev, data.intent))
        }

        // Update recommendations
        if (data.matches?.length) {
          setRecs(data.matches)
        }

        // Handle different AI booking responses
        if (data.action === 'BOOKED' && data.booking?.id) {
          setPendingConfirm(null)
          setSlots({})
          setSuccessBooking(data.booking)
          addMessage({
            role: 'assistant',
            content: `🎉 **Booking Confirmed!**\n\nBooking ID: **#${data.booking.id}**\nRoom: ${data.booking.resources?.[0]?.name || 'N/A'}\nLocation: ${data.booking.resources?.[0]?.buildingName || ''} – ${data.booking.resources?.[0]?.floorName || ''}\nDate: ${data.booking.date}\nTime: ${data.booking.startTime} – ${data.booking.endTime}\nStatus: ${data.booking.status}`,
            type: 'success',
            booking: data.booking,
          })
          toast.success('Booking created successfully!')
        } else if (data.action === 'CONFIRM') {
          setPendingConfirm(data)
          addMessage({
            role: 'assistant',
            content: data.message,
            type: 'confirm',
            intent: data.intent,
          })
        } else if (data.action === 'CLARIFY') {
          addMessage({
            role: 'assistant',
            content: data.message + (data.questions?.length ? '\n\n' + data.questions.map((q) => `• ${q}`).join('\n') : ''),
            type: 'clarify',
          })
        } else if (data.action === 'AMBIGUOUS') {
          addMessage({
            role: 'assistant',
            content: data.message,
            type: 'clarify',
          })
        } else if (data.action === 'UNAVAILABLE') {
          addMessage({
            role: 'assistant',
            content: data.message,
            type: 'error',
          })
        }

        if (!data.aiAvailable) {
          // Show subtle indicator that Groq is offline
        }
      } else {
        // Regular chat — not a booking intent
        const [{ data: chat }, { data: found }] = await Promise.all([
          client.post('/ai/chat', { message, history: messages.slice(-10).map((m) => ({ role: m.role, content: m.content })) }),
          client.post('/ai/interpret', { prompt: message }),
        ])
        addMessage({ role: 'assistant', content: chat.reply, aiAvailable: chat.aiAvailable })
        setRecs(found.recommendations || [])

        if (found.intent) {
          setSlots((prev) => mergeSlots(prev, found.intent))
        }
      }
    } catch (err) {
      toast.error(errorMessage(err, 'Assistant could not answer just now.'))
      addMessage({
        role: 'assistant',
        content: 'I could not reach the assistant right now. You can use the campus map or the manual booking form — both work without AI.',
        type: 'error',
      })
    } finally {
      setLoading(false)
      inputRef.current?.focus()
    }
  }

  const confirmBooking = () => {
    if (pendingConfirm?.intent) {
      const it = pendingConfirm.intent
      const room = it.resourceName || slots.resourceName || 'the room'
      const date = it.date || slots.date
      const start = it.startTime || slots.startTime
      const end = it.endTime || slots.endTime
      const cap = it.capacity || slots.capacity || 1
      send(`Yes, confirm booking ${room} on ${date} from ${start} to ${end} with ${cap} attendees`)
    } else {
      send('Yes, confirm the booking.')
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.1fr_.9fr]">
      <div className="flex flex-col">
        <PageHeader
          kicker="AI Assistant"
          title="Chat with CampusOS"
          subtitle="Ask questions, find rooms, or book directly by describing what you need. The assistant can search the database and create real bookings."
          actions={<Link to="/book" className="btn-ghost">Manual booking</Link>}
        />

        {/* Quick suggestion chips */}
        <div className="mb-3 flex flex-wrap gap-2">
          {sparks.map((s) => (
            <button key={s} onClick={() => send(s)} className="rounded-full border px-3 py-1.5 text-xs transition hover:bg-grove-700 hover:text-white dark:border-white/10">{s}</button>
          ))}
        </div>

        {/* Chat messages */}
        <div className="min-h-[360px] max-h-[60vh] space-y-3 overflow-y-auto pr-1">
          {messages.length === 0 && (
            <div className="flex h-48 items-center justify-center text-center text-sm text-grove-500">
              <div>
                <div className="mx-auto mb-3 grid h-12 w-12 place-items-center rounded-full bg-grove-700/10 text-xl">🤖</div>
                <p>Ask me anything about campus rooms, or say<br /><strong>"Book a classroom for 50 people tomorrow"</strong></p>
              </div>
            </div>
          )}

          {messages.map((m, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className={`rounded-2xl p-4 text-sm ${
                m.role === 'user'
                  ? 'ml-10 bg-grove-700 text-white'
                  : m.type === 'success'
                  ? 'mr-10 border-2 border-grove-500/30 bg-grove-50 shadow-sm dark:bg-grove-800'
                  : m.type === 'error'
                  ? 'mr-10 border border-brick-500/20 bg-brick-500/5 dark:bg-grove-800'
                  : m.type === 'confirm'
                  ? 'mr-10 border-2 border-gold-400/40 bg-gold-400/5 dark:bg-grove-800'
                  : 'mr-10 bg-white shadow-sm dark:bg-grove-800'
              }`}
            >
              <div className="whitespace-pre-line">{m.content}</div>

              {/* Booking success card */}
              {m.type === 'success' && m.booking && (
                <div className="mt-3 flex flex-wrap gap-2">
                  <Link to={`/bookings/${m.booking.id}`} className="btn-primary text-xs">View Booking</Link>
                  <Link to={`/bookings/${m.booking.id}/confirmation`} className="btn-ghost text-xs">Details</Link>
                </div>
              )}

              {/* Confirmation prompt */}
              {m.type === 'confirm' && i === messages.length - 1 && pendingConfirm && (
                <div className="mt-3 flex gap-2">
                  <button onClick={confirmBooking} className="btn-primary text-xs">✓ Confirm Booking</button>
                  <button onClick={() => { setPendingConfirm(null); addMessage({ role: 'assistant', content: 'Booking cancelled. Let me know if you need something else.' }) }} className="btn-ghost text-xs">✕ Cancel</button>
                </div>
              )}
            </motion.div>
          ))}

          {loading && (
            <div className="mr-10 flex items-center gap-2 rounded-2xl bg-white p-4 text-sm dark:bg-grove-800">
              <Spinner size={16} className="text-grove-500" />
              <span className="text-grove-500">Looking through the campus database…</span>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* Accumulated context indicator */}
        {Object.keys(slots).length > 0 && (
          <div className="mt-2 flex flex-wrap items-center gap-1 text-xs text-grove-500">
            <span className="font-semibold">Context:</span>
            {slots.building && <span className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{slots.building}</span>}
            {slots.capacity && <span className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{slots.capacity} people</span>}
            {slots.date && <span className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{slots.date}</span>}
            {slots.startTime && <span className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{slots.startTime}–{slots.endTime}</span>}
            {slots.resourceType && <span className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{slots.resourceType}</span>}
            {slots.facilities?.map((f) => <span key={f} className="rounded-full bg-grove-100 px-2 py-0.5 dark:bg-grove-800">{f}</span>)}
            <button onClick={() => setSlots({})} className="ml-1 text-brick-500 hover:underline">Clear</button>
          </div>
        )}

        {/* Input */}
        <form onSubmit={(e) => { e.preventDefault(); send() }} className="mt-3 flex gap-2">
          <input
            ref={inputRef}
            className="field flex-1 rounded-full"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={pendingConfirm ? 'Type "yes" to confirm or describe changes…' : 'Describe the room you need or ask a question…'}
            disabled={loading}
          />
          <button className="btn-primary" disabled={loading}>
            {loading ? <Spinner size={16} /> : 'Send'}
          </button>
        </form>
      </div>

      {/* Right sidebar — recommendations */}
      <aside className="space-y-3">
        <div className="font-display text-2xl">Matching Rooms</div>
        <p className="text-xs text-grove-500">Rooms found in the database matching your request. Click "Book" to start the booking flow.</p>
        {recs.map((r) => (
          <div key={r.resourceId} className="soft-card p-4 transition hover:shadow-md">
            <div className="flex items-start justify-between gap-2">
              <div>
                <div className="font-semibold">{r.resourceName}</div>
                <div className="text-xs text-grove-500">{r.resourceCode} · score {r.score}</div>
              </div>
              <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${r.available ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                {r.available ? 'Available' : 'Unavailable'}
              </span>
            </div>
            <p className="mt-2 text-sm text-grove-600 dark:text-grove-200">{r.reason}</p>
            <div className="mt-3 flex gap-2">
              <Link to={`/book/${r.resourceId}`} className="btn-primary text-xs">Book this room</Link>
              <Link to={`/resources/${r.resourceId}`} className="btn-ghost text-xs">View details</Link>
            </div>
          </div>
        ))}
        {recs.length === 0 && (
          <EmptyState title="No matches yet" icon="🔍">
            Ask for a classroom, lab, or hall and matching rooms will appear here with availability and scores.
          </EmptyState>
        )}
      </aside>

      {/* Success dialog (shown after booking) */}
      <SuccessDialog
        open={!!successBooking}
        title="Your Booking is Confirmed!"
        onClose={() => setSuccessBooking(null)}
        actions={
          <>
            <Link to={`/bookings/${successBooking?.id}`} className="btn-primary" onClick={() => setSuccessBooking(null)}>View Booking</Link>
            <button className="btn-ghost" onClick={() => setSuccessBooking(null)}>Continue chatting</button>
          </>
        }
      >
        <div className="space-y-2 text-left">
          <div className="flex justify-between"><span className="text-grove-500">Booking ID</span><span className="font-semibold">#{successBooking?.id}</span></div>
          <div className="flex justify-between"><span className="text-grove-500">Room</span><span className="font-semibold">{successBooking?.resources?.[0]?.name}</span></div>
          <div className="flex justify-between"><span className="text-grove-500">Location</span><span>{successBooking?.resources?.[0]?.buildingName} – {successBooking?.resources?.[0]?.floorName}</span></div>
          <div className="flex justify-between"><span className="text-grove-500">Date</span><span>{formatDate(successBooking?.date)}</span></div>
          <div className="flex justify-between"><span className="text-grove-500">Time</span><span>{formatTime(successBooking?.startTime)} – {formatTime(successBooking?.endTime)}</span></div>
          <div className="flex justify-between"><span className="text-grove-500">Status</span><span className="rounded-full bg-grove-100 px-2 py-0.5 text-xs font-semibold text-grove-700 dark:bg-grove-700 dark:text-grove-100">{successBooking?.status?.replaceAll('_', ' ')}</span></div>
        </div>
      </SuccessDialog>
    </div>
  )
}
