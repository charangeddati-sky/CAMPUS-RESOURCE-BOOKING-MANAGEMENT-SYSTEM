import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'

export default function Breadcrumbs({ items }) {
  return (
    <nav aria-label="Breadcrumb" className="mb-4 flex flex-wrap items-center gap-1 text-sm text-grove-600 dark:text-grove-200">
      {items.map((item, i) => (
        <span key={item.label} className="inline-flex items-center gap-1">
          {i > 0 && <ChevronRight className="h-3.5 w-3.5 opacity-60" />}
          {item.to ? (
            <Link className="hover:text-grove-800 dark:hover:text-white" to={item.to}>{item.label}</Link>
          ) : (
            <span className="font-semibold text-grove-800 dark:text-cream-50">{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  )
}
