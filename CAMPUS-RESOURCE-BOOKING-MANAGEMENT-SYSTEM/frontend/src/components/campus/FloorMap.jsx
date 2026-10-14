import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { statusMeta } from '../../utils/status'

export default function FloorMap({ resources, onSelect, selectedId }) {
  const navigate = useNavigate()

  return (
    <div className="relative min-h-[420px] overflow-hidden rounded-3xl border border-dashed border-grove-300 bg-[linear-gradient(90deg,#e8dfcc22_1px,transparent_1px),linear-gradient(#e8dfcc22_1px,transparent_1px)] bg-[size:24px_24px] p-3 dark:border-white/10 dark:bg-grove-800">
      {resources.map((r) => {
        const meta = statusMeta(r.status)
        const selected = selectedId === r.id
        return (
          <button
            key={r.id}
            type="button"
            onClick={() => (onSelect ? onSelect(r) : navigate(`/resources/${r.id}`))}
            className="absolute overflow-hidden rounded-xl border-2 text-left shadow-sm transition"
            style={{
              left: `${r.positionX || 0}%`,
              top: `${r.positionY || 0}%`,
              width: `${r.width || 14}%`,
              height: `${r.height || 16}%`,
              transform: `rotate(${r.rotation || 0}deg)`,
              borderColor: selected ? '#d4a017' : meta.color,
              background: `${meta.color}18`,
            }}
          >
            <div className="h-full w-full p-2">
              <div className="truncate text-xs font-bold">{r.name}</div>
              <div className="truncate text-[10px] opacity-70">{r.code}</div>
              <div className="mt-1 text-lg leading-none">{meta.emoji}</div>
            </div>
          </button>
        )
      })}
      {resources.length === 0 && (
        <div className="grid h-[380px] place-items-center text-sm text-grove-500">No resources on this floor.</div>
      )}
    </div>
  )
}

export function FloorEditor({ resources, onSave }) {
  const [items, setItems] = useState(resources)

  useEffect(() => {
    setItems(resources)
  }, [resources])

  const drag = (id, e) => {
    const canvas = e.currentTarget.parentElement.getBoundingClientRect()
    const move = (ev) => {
      const x = ((ev.clientX - canvas.left) / canvas.width) * 100
      const y = ((ev.clientY - canvas.top) / canvas.height) * 100
      setItems((prev) => prev.map((r) => (r.id === id ? { ...r, positionX: clamp(x), positionY: clamp(y) } : r)))
    }
    const up = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', up)
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
  }

  return (
    <div>
      <div className="relative min-h-[480px] rounded-3xl border bg-cream-100 dark:bg-grove-800">
        {items.map((r) => (
          <div
            key={r.id}
            onPointerDown={(e) => drag(r.id, e)}
            className="absolute cursor-move rounded-lg border-2 border-grove-700 bg-white p-2 text-xs shadow dark:bg-grove-700"
            style={{
              left: `${r.positionX}%`,
              top: `${r.positionY}%`,
              width: `${r.width}%`,
              height: `${r.height}%`,
            }}
          >
            <input
              className="w-full bg-transparent font-semibold"
              value={r.name}
              onChange={(e) => setItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, name: e.target.value } : x)))}
            />
            <div className="mt-1 flex gap-1">
              <label className="flex items-center gap-1">W
                <input type="number" className="w-14 rounded border px-1 dark:bg-grove-800" value={r.width}
                  onChange={(e) => setItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, width: Number(e.target.value) } : x)))} />
              </label>
              <label className="flex items-center gap-1">H
                <input type="number" className="w-14 rounded border px-1 dark:bg-grove-800" value={r.height}
                  onChange={(e) => setItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, height: Number(e.target.value) } : x)))} />
              </label>
            </div>
          </div>
        ))}
      </div>
      <button
        className="mt-4 rounded-full bg-grove-700 px-5 py-2 text-sm font-semibold text-white"
        onClick={() => onSave(items.map((r) => ({
          resourceId: r.id, positionX: r.positionX, positionY: r.positionY, width: r.width, height: r.height, rotation: r.rotation, name: r.name, typeCode: r.typeCode,
        })))}
      >
        Save layout
      </button>
    </div>
  )
}

function clamp(n) {
  return Math.max(0, Math.min(90, n))
}
