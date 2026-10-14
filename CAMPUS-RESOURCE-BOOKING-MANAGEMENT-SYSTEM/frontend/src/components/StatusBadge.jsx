import { statusMeta } from '../utils/status'

export default function StatusBadge({ status }) {
  const meta = statusMeta(status)
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold"
      style={{ background: `${meta.color}18`, color: meta.color }}
    >
      <span aria-hidden>{meta.emoji}</span>
      {meta.label}
    </span>
  )
}
