import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, CalendarCheck, Compass, Sparkles } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'

const blocks = ['Loyalty 1', 'Loyalty 2', 'Loyalty 3', 'Loyalty 4', 'Wisdom', 'Honesty', 'Truth']

export default function Landing() {
  const { isAuthenticated } = useAuth()
  const [stats, setStats] = useState(null)

  useEffect(() => {
    client.get('/public/stats').then((r) => setStats(r.data)).catch(() => {})
  }, [])

  const go = (path) => (isAuthenticated ? path : '/login')

  return (
    <div className="min-h-screen bg-grove-900 text-cream-50">
      <header className="relative z-10 mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <div>
          <div className="font-display text-2xl">CampusOS</div>
          <div className="text-[11px] uppercase tracking-[0.22em] text-gold-400">VVIT digital campus</div>
        </div>
        <div className="flex gap-2 text-sm">
          <Link to="/login" className="rounded-full px-4 py-2 hover:bg-white/10">Sign in</Link>
          <Link to="/register" className="rounded-full bg-gold-500 px-4 py-2 font-semibold text-grove-900">Create account</Link>
        </div>
      </header>

      <section className="relative overflow-hidden">
        <img src="/campus.jpg" alt="" className="absolute inset-0 h-full w-full object-cover opacity-40" />
        <div className="absolute inset-0 bg-gradient-to-b from-grove-900/40 via-grove-900/75 to-grove-900" />
        <div className="relative mx-auto grid max-w-6xl items-center gap-10 px-6 pb-20 pt-10 lg:grid-cols-[1.1fr_.9fr] lg:pt-16">
          <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.7 }}>
            <p className="text-xs uppercase tracking-[0.32em] text-gold-400">Vasireddy Venkatadri Institute of Technology</p>
            <h1 className="mt-4 font-display text-5xl leading-[1.05] md:text-7xl">
              See the campus.
              <span className="block text-gold-400">Book the room.</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg text-cream-200">
              An interactive twin of Loyalty, Wisdom, Honesty and Truth — live availability, a clean booking flow, and an assistant that only recommends rooms that exist in MySQL.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={go('/campus')} className="inline-flex items-center gap-2 rounded-full bg-cream-100 px-6 py-3 font-semibold text-grove-900">
                <Compass className="h-4 w-4" /> Explore campus
              </Link>
              <Link to={go('/book')} className="inline-flex items-center gap-2 rounded-full border border-white/20 px-6 py-3">
                <CalendarCheck className="h-4 w-4" /> Book a resource
              </Link>
              <Link to={go('/ai-chat')} className="inline-flex items-center gap-2 rounded-full border border-gold-500/40 px-6 py-3 text-gold-400">
                <Sparkles className="h-4 w-4" /> Ask the assistant
              </Link>
            </div>
            <div className="mt-10 flex flex-wrap gap-2">
              {blocks.map((b) => (
                <span key={b} className="rounded-full border border-white/15 px-3 py-1 text-xs tracking-wide text-cream-200">{b}</span>
              ))}
            </div>
          </motion.div>
          <motion.div initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.7, delay: 0.1 }} className="relative">
            <img src="/campus.jpg" alt="VVIT aerial campus" className="rounded-[2rem] shadow-2xl ring-1 ring-white/20" />
            <img src="/campus-architecture.png" alt="Campus architecture" className="absolute -bottom-8 -left-6 hidden w-44 rounded-2xl bg-white p-2 shadow-xl md:block" />
            {stats && (
              <div className="absolute -right-2 top-6 hidden rounded-2xl bg-grove-800/90 p-4 text-sm shadow-xl backdrop-blur md:block">
                <div className="text-gold-400">{stats.availableNow} free now</div>
                <div className="text-cream-200">{stats.resources} resources · {stats.buildings} blocks</div>
              </div>
            )}
          </motion.div>
        </div>
      </section>

      <section className="mx-auto grid max-w-6xl gap-4 px-6 py-10 md:grid-cols-4">
        {[
          [stats?.buildings ?? 8, 'Named blocks'],
          [stats?.resources ?? '140+', 'Bookable rooms'],
          ['Live', 'SQL availability'],
          ['AI', 'Groq + database'],
        ].map(([v, l], i) => (
          <motion.div key={l} initial={{ opacity: 0, y: 12 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ delay: i * 0.05 }} className="rounded-3xl border border-white/10 bg-white/5 p-6">
            <div className="font-display text-4xl">{v}</div>
            <div className="mt-1 text-sm text-cream-200">{l}</div>
          </motion.div>
        ))}
      </section>

      <section className="mx-auto max-w-6xl px-6 pb-20">
        <h2 className="font-display text-4xl">How a booking actually happens</h2>
        <div className="mt-8 grid gap-4 md:grid-cols-3">
          {[
            ['01', 'Walk the twin', 'Click Loyalty, Wisdom, Honesty or Truth on the aerial map. Floors and rooms are stored in MySQL, not in the UI.'],
            ['02', 'Pick a free hour', 'Availability is computed from overlapping bookings, maintenance and working hours. If it is red, it will not book.'],
            ['03', 'Confirm & show up', 'Students go through professor then admin approval. Check in from the booking page — no scanning required.'],
          ].map((item) => (
            <div key={item[0]} className="rounded-3xl border border-white/10 bg-gradient-to-br from-white/10 to-transparent p-6">
              <div className="text-gold-400">{item[0]}</div>
              <div className="mt-2 font-display text-2xl">{item[1]}</div>
              <p className="mt-2 text-sm text-cream-200">{item[2]}</p>
            </div>
          ))}
        </div>
        <Link to={go('/dashboard')} className="mt-10 inline-flex items-center gap-2 text-gold-400">
          Open the operating console <ArrowRight className="h-4 w-4" />
        </Link>
      </section>
    </div>
  )
}
