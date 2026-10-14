import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import useReducedMotion from '../hooks/useReducedMotion'

export const fadeUp = {
  initial: { opacity: 0, y: 18 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.45, ease: [0.22, 1, 0.36, 1] },
}

export function MotionCard({ children, className = '', delay = 0 }) {
  const reduced = useReducedMotion()
  if (reduced) {
    return <div className={className}>{children}</div>
  }
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay, ease: [0.22, 1, 0.36, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  )
}

export function StatCard({ label, value, hint, accent }) {
  return (
    <div className="soft-card relative overflow-hidden p-5">
      <div className="stat-kicker">{label}</div>
      <div className="mt-1 font-display text-3xl tabular-nums" style={{ color: accent }}>{value ?? '—'}</div>
      {hint && <div className="mt-1 text-xs text-grove-500">{hint}</div>}
    </div>
  )
}

export function PageHeader({ kicker, title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        {kicker && <p className="stat-kicker text-gold-500">{kicker}</p>}
        <h1 className="font-display text-4xl tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 max-w-2xl text-sm text-grove-600 dark:text-grove-200">{subtitle}</p>}
      </div>
      {actions}
    </div>
  )
}

export function Skeleton({ className = 'h-24' }) {
  return <div className={`soft-card animate-pulse bg-cream-100 dark:bg-grove-800 ${className}`} aria-hidden />
}

export function EmptyState({ title, children, action, icon }) {
  return (
    <div className="soft-card px-6 py-10 text-center">
      {icon && <div className="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-cream-100 text-2xl dark:bg-grove-800">{icon}</div>}
      <div className="font-display text-2xl">{title}</div>
      {children && <p className="mt-2 text-sm text-grove-500">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null
  return (
    <div role="alert" className="rounded-2xl border border-brick-500/30 bg-brick-500/10 px-4 py-3 text-sm">
      <div className="font-semibold text-brick-600">Something went wrong</div>
      <p className="text-grove-600 dark:text-grove-200">{message}</p>
      {onRetry && <button type="button" className="btn-ghost mt-2 text-xs" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function ConfirmDialog({ open, title, children, confirmLabel = 'Confirm', danger = false, busy = false, onConfirm, onCancel }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 animate-fade-in" role="dialog" aria-modal="true" aria-labelledby="confirm-title" onClick={onCancel}>
      <div className="w-full max-w-md rounded-2xl bg-cream-50 p-5 shadow-xl dark:bg-grove-800 animate-modal-enter" onClick={(e) => e.stopPropagation()}>
        <h2 id="confirm-title" className="font-display text-2xl">{title}</h2>
        <div className="mt-2 text-sm text-grove-600 dark:text-grove-200">{children}</div>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={onCancel} disabled={busy}>Cancel</button>
          <button
            type="button"
            className={danger ? 'btn-primary bg-brick-500 hover:bg-brick-600' : 'btn-primary'}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export function Pagination({ page, totalPages, onPage, total }) {
  if (!totalPages || totalPages <= 1) {
    return total != null ? <p className="text-xs text-grove-500">{total} results</p> : null
  }
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-3">
      <p className="text-xs text-grove-500">{total} results · page {page + 1} of {totalPages}</p>
      <div className="flex gap-2">
        <button type="button" className="btn-ghost text-xs" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</button>
        <button type="button" className="btn-ghost text-xs" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
      </div>
    </div>
  )
}

/* ─── Confetti celebration overlay (disabled) ──────────────────── */

export function ConfettiOverlay() {
  return null
}

/* ─── Success dialog with animated check ───────────────────────── */

export function SuccessDialog({ open, title = 'Success!', children, actions, onClose }) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4 animate-fade-in" role="dialog" aria-modal="true" onClick={onClose}>
      <div className="w-full max-w-md rounded-2xl bg-cream-50 p-6 text-center shadow-xl dark:bg-grove-800 animate-modal-enter" onClick={(e) => e.stopPropagation()}>
        {/* Animated check circle */}
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-grove-600 animate-success-pop">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
            <path d="M8 16l6 6L24 10" stroke="#fff" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" className="animate-check-draw" />
          </svg>
        </div>
        <h2 className="mt-4 font-display text-3xl">{title}</h2>
        <div className="mt-3 text-sm text-grove-600 dark:text-grove-200">{children}</div>
        {actions && <div className="mt-5 flex flex-wrap justify-center gap-2">{actions}</div>}
      </div>
    </div>
  )
}

/* ─── Loading spinner ──────────────────────────────────────────── */

export function Spinner({ size = 20, className = '' }) {
  return (
    <svg className={`animate-spin ${className}`} width={size} height={size} viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" className="opacity-20" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}
