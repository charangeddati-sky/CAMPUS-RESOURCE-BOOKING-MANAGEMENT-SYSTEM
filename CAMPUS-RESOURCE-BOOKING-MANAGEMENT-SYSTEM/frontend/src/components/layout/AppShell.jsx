import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import {
  Bell, Building2, CalendarDays, ClipboardCheck, Compass, LayoutDashboard,
  LogOut, Menu, Moon, Search, Sparkles, Sun, X,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import client from '../../api/client'

const links = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/campus', label: 'Campus', icon: Compass },
  { to: '/book', label: 'Book a room', icon: CalendarDays },
  { to: '/bookings', label: 'My bookings', icon: ClipboardCheck },
  { to: '/calendar', label: 'Calendar', icon: CalendarDays },
  { to: '/ai-chat', label: 'AI assistant', icon: Sparkles },
  { to: '/ai-booking', label: 'AI booking', icon: Sparkles },
  { to: '/available-now', label: 'Available now', icon: Building2 },
]

export default function AppShell() {
  const { user, logout, role } = useAuth()
  const { mode, setMode } = useTheme()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [q, setQ] = useState('')
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    client.get('/notifications/unread').then((r) => setUnread(r.data.count || 0)).catch(() => {})
  }, [])

  const extra = []
  if (role === 'PROFESSOR' || role === 'ADMIN') extra.push({ to: '/approvals', label: 'Approvals', icon: ClipboardCheck })
  if (role === 'ADMIN') {
    extra.push({ to: '/admin/analytics', label: 'Analytics', icon: LayoutDashboard })
    extra.push({ to: '/admin/resources', label: 'Resources', icon: Building2 })
    extra.push({ to: '/admin', label: 'Admin', icon: Building2 })
  }

  const onSearch = (e) => {
    e.preventDefault()
    if (q.trim()) navigate(`/search?q=${encodeURIComponent(q.trim())}`)
  }

  return (
    <div className="min-h-screen w-full bg-cream-50 dark:bg-grove-900">
      <header className="sticky top-0 z-40 w-full border-b border-cream-200/80 bg-cream-50/90 backdrop-blur-md dark:border-white/10 dark:bg-grove-900/90">
        <div className="flex w-full items-center gap-3 px-5 py-3">
          <button className="rounded-lg p-2 lg:hidden focus-ring" onClick={() => setOpen(true)} aria-label="Open menu">
            <Menu className="h-5 w-5" />
          </button>
          <Link to="/dashboard" className="flex items-center gap-2">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-grove-700 text-cream-100 font-display font-bold">V</span>
            <span className="hidden sm:block">
              <span className="block font-display text-lg leading-none">CampusOS</span>
              <span className="text-[11px] uppercase tracking-[0.18em] text-grove-500">VVIT</span>
            </span>
          </Link>
          <form onSubmit={onSearch} className="relative mx-6 hidden min-w-0 flex-1 md:block">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-grove-400" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search Room 204, Physics Lab, Seminar Hall..."
              className="field rounded-full pl-10"
            />
          </form>
          <div className="ml-auto flex items-center gap-1">
            <button className="rounded-full p-2 focus-ring" aria-label="Toggle theme" onClick={() => setMode(mode === 'dark' ? 'light' : mode === 'light' ? 'system' : 'dark')}>
              {mode === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
            <Link to="/notifications" className="relative rounded-full p-2 focus-ring" aria-label="Notifications">
              <Bell className="h-4 w-4" />
              {unread > 0 && <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-brick-500" />}
            </Link>
            <div className="hidden items-center gap-2 pl-2 sm:flex">
              <div className="text-right">
                <div className="text-sm font-semibold">{user?.fullName}</div>
                <div className="text-[11px] uppercase tracking-wide text-grove-500">{user?.role}</div>
              </div>
              <button className="rounded-full p-2 focus-ring" onClick={() => { logout(); navigate('/') }} aria-label="Log out">
                <LogOut className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="flex w-full">
        <aside className="sticky top-[61px] hidden h-[calc(100vh-61px)] w-56 shrink-0 overflow-y-auto border-r border-cream-200 p-3 lg:block dark:border-white/10">
          <NavList items={[...links, ...extra]} />
        </aside>
        <main className="min-h-[calc(100vh-61px)] min-w-0 flex-1 px-5 py-5 pb-24 lg:px-8">
          <Outlet />
        </main>
      </div>

      <AnimatePresence>
        {open && (
          <motion.div className="fixed inset-0 z-50 bg-black/40 lg:hidden" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setOpen(false)}>
            <motion.div initial={{ x: -24, opacity: 0 }} animate={{ x: 0, opacity: 1 }} exit={{ x: -24, opacity: 0 }} className="h-full w-72 bg-cream-50 p-4 dark:bg-grove-900" onClick={(e) => e.stopPropagation()}>
              <div className="mb-4 flex items-center justify-between">
                <span className="font-display text-lg">CampusOS</span>
                <button onClick={() => setOpen(false)} aria-label="Close menu"><X /></button>
              </div>
              <NavList items={[...links, ...extra]} onClick={() => setOpen(false)} />
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <nav className="fixed bottom-0 left-0 right-0 z-40 grid grid-cols-5 border-t border-cream-200 bg-cream-50/95 p-1 backdrop-blur md:hidden dark:border-white/10 dark:bg-grove-900/95">
        {[
          { to: '/dashboard', label: 'Home', icon: LayoutDashboard },
          { to: '/campus', label: 'Campus', icon: Compass },
          { to: '/book', label: 'Book', icon: CalendarDays },
          { to: '/bookings', label: 'Bookings', icon: ClipboardCheck },
          { to: '/ai-chat', label: 'AI', icon: Sparkles },
        ].map((item) => (
          <NavLink key={item.to} to={item.to} className={({ isActive }) => `flex flex-col items-center gap-0.5 py-2 text-[10px] ${isActive ? 'text-grove-700 dark:text-gold-400' : 'text-grove-500'}`}>
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

function NavList({ items, onClick }) {
  return (
    <nav className="space-y-1">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          onClick={onClick}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
              isActive ? 'bg-grove-700 text-cream-50 shadow-sm' : 'text-grove-700 hover:bg-grove-100 dark:text-cream-100 dark:hover:bg-white/5'
            }`
          }
        >
          <item.icon className="h-4 w-4" />
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}
