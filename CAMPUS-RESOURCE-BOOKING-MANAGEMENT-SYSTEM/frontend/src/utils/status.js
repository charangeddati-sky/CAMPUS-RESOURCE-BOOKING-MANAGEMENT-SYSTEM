export function statusMeta(status) {
  switch (status) {
    case 'AVAILABLE':
      return { label: 'Available', color: '#16a34a', emoji: '🟢' }
    case 'BOOKED':
      return { label: 'Booked', color: '#dc2626', emoji: '🔴' }
    case 'MAINTENANCE':
      return { label: 'Maintenance', color: '#ca8a04', emoji: '🟡' }
    case 'BLOCKED':
    case 'UNAVAILABLE':
    case 'OUT_OF_SERVICE':
      return { label: 'Unavailable', color: '#334155', emoji: '⚫' }
    case 'INACTIVE':
      return { label: 'Inactive', color: '#64748b', emoji: '⚪' }
    case 'PENDING':
      return { label: 'Pending', color: '#ea580c', emoji: '🟠' }
    case 'PENDING_PROFESSOR':
      return { label: 'Awaiting Professor', color: '#ea580c', emoji: '🟠' }
    case 'PENDING_ADMIN':
      return { label: 'Awaiting Admin', color: '#d97706', emoji: '🟠' }
    case 'CONFIRMED':
      return { label: 'Confirmed', color: '#16a34a', emoji: '✅' }
    case 'CHECKED_IN':
      return { label: 'Checked In', color: '#0284c7', emoji: '📍' }
    case 'COMPLETED':
      return { label: 'Completed', color: '#6366f1', emoji: '✔️' }
    case 'CANCELLED':
      return { label: 'Cancelled', color: '#64748b', emoji: '❌' }
    case 'REJECTED':
      return { label: 'Rejected', color: '#dc2626', emoji: '🚫' }
    case 'NO_SHOW':
      return { label: 'No Show', color: '#9f1239', emoji: '⚠️' }
    default:
      return { label: status || 'Unknown', color: '#64748b', emoji: '⚪' }
  }
}

export function occupancyColor(level) {
  if (level === 'HIGH') return '#dc2626'
  if (level === 'MEDIUM') return '#ca8a04'
  return '#16a34a'
}

export function errorMessage(err, fallback = 'Something went wrong') {
  return err?.response?.data?.message || err?.message || fallback
}

/** Format a date string for display */
export function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr + 'T00:00:00')
    return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

/** Format time string (HH:mm) for display */
export function formatTime(timeStr) {
  if (!timeStr) return ''
  const [h, m] = timeStr.split(':')
  const hour = parseInt(h, 10)
  const ampm = hour >= 12 ? 'PM' : 'AM'
  const h12 = hour % 12 || 12
  return `${h12}:${m} ${ampm}`
}

/** Get badge styling for a given booking or resource status */
export function statusBadge(status) {
  const meta = statusMeta(status)
  let bg = 'bg-cream-100 text-grove-700 dark:bg-grove-800 dark:text-grove-300'
  switch (status) {
    case 'CONFIRMED':
    case 'AVAILABLE':
      bg = 'bg-emerald-50 text-emerald-700 border border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800'
      break
    case 'PENDING':
    case 'PENDING_PROFESSOR':
    case 'PENDING_ADMIN':
      bg = 'bg-amber-50 text-amber-700 border border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800'
      break
    case 'CHECKED_IN':
      bg = 'bg-sky-50 text-sky-700 border border-sky-200 dark:bg-sky-950/40 dark:text-sky-300 dark:border-sky-800'
      break
    case 'COMPLETED':
      bg = 'bg-indigo-50 text-indigo-700 border border-indigo-200 dark:bg-indigo-950/40 dark:text-indigo-300 dark:border-indigo-800'
      break
    case 'CANCELLED':
    case 'REJECTED':
    case 'NO_SHOW':
      bg = 'bg-rose-50 text-rose-700 border border-rose-200 dark:bg-rose-950/40 dark:text-rose-300 dark:border-rose-800'
      break
    default:
      bg = 'bg-cream-100 text-grove-700 dark:bg-grove-800 dark:text-grove-300'
  }
  return { label: meta.label, bg, color: '' }
}
