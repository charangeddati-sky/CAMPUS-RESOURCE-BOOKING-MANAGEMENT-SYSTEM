import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { occupancyColor } from '../../utils/status'

const SCHEMATIC_GRID = {
  GROUND: { gridArea: 'ground' },
  LOYALTY2: { gridArea: 'b' },
  LOYALTY3: { gridArea: 'c' },
  LOYALTY1: { gridArea: 'a' },
  WISDOM: { gridArea: 'w' },
  LOYALTY4: { gridArea: 'd' },
  TRUTH: { gridArea: 'truth' },
  HONESTY: { gridArea: 'h' },
}

export default function CampusMap({ buildings, heatmap = [] }) {
  const [view, setView] = useState('aerial')
  const [hover, setHover] = useState(null)
  const navigate = useNavigate()
  const heat = Object.fromEntries((heatmap || []).map((h) => [h.buildingId, h]))
  const hovered = buildings.find((b) => b.id === hover)

  return (
    <div className="w-full">
      <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="stat-kicker text-gold-500">Digital twin</p>
          <h2 className="font-display text-3xl md:text-4xl">VVIT campus</h2>
          <p className="text-sm text-grove-600 dark:text-grove-200">Boxes follow the real blocks — Loyalty, Wisdom, Honesty, Truth. Click any to enter.</p>
        </div>
        <div className="flex rounded-full bg-white p-1 text-sm shadow-sm dark:bg-grove-800">
          {['aerial', 'schematic'].map((v) => (
            <button key={v} onClick={() => setView(v)} className={`rounded-full px-4 py-1.5 capitalize ${view === v ? 'bg-grove-700 text-white' : ''}`}>
              {v}
            </button>
          ))}
        </div>
      </div>

      {view === 'aerial' ? (
        <div className="relative w-full overflow-hidden rounded-[1.6rem] border border-cream-200 shadow-2xl dark:border-white/10">
          <img src="/campus.jpg" alt="VVIT aerial campus" className="block w-full select-none" />
          {buildings.map((b) => (
            <Hotspot
              key={b.id}
              building={b}
              heat={heat[b.id]}
              hover={hover === b.id}
              onEnter={() => setHover(b.id)}
              onLeave={() => setHover(null)}
              onClick={() => b.bookable && navigate(`/buildings/${b.id}`)}
              box={{ x: b.mapX, y: b.mapY, w: b.mapWidth, h: b.mapHeight }}
            />
          ))}
          <AnimatePresence>
            {hovered && <HoverCard building={hovered} heat={heat[hovered.id]} />}
          </AnimatePresence>
        </div>
      ) : (
        <Schematic buildings={buildings} heat={heat} onHover={setHover} hovered={hovered} navigate={navigate} />
      )}

      <div className="mt-4 flex flex-wrap gap-3 text-xs text-grove-600">
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-green-500" /> Quiet</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-yellow-500" /> Moderate</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-red-500" /> Busy</span>
        <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-sm border border-dashed border-grove-500" /> Ground — no bookings</span>
      </div>
    </div>
  )
}

function Hotspot({ building, heat, hover, onEnter, onLeave, onClick, box }) {
  const color = occupancyColor(heat?.level || building.liveStatus)
  const dashed = !building.bookable
  return (
    <button
      type="button"
      aria-label={building.name}
      onMouseEnter={onEnter}
      onMouseLeave={onLeave}
      onFocus={onEnter}
      onBlur={onLeave}
      onClick={onClick}
      className="absolute overflow-hidden rounded-md text-left transition-all duration-300"
      style={{
        left: `${box.x}%`,
        top: `${box.y}%`,
        width: `${box.w}%`,
        height: `${box.h}%`,
        border: dashed ? `2px dashed ${color}` : `2px solid ${hover ? color : `${color}cc`}`,
        background: hover ? `${color}40` : `${color}22`,
        boxShadow: hover ? `0 0 0 3px ${color}66, 0 12px 30px ${color}33` : 'none',
        cursor: building.bookable ? 'pointer' : 'default',
      }}
    >
      <span className="absolute inset-x-0 bottom-0 bg-black/55 px-1.5 py-1 text-[10px] font-bold uppercase tracking-wide text-white md:text-[11px]">
        {building.name}
        {building.virtueName ? ` · ${building.virtueName}` : ''}
      </span>
    </button>
  )
}

function Schematic({ buildings, heat, onHover, hovered, navigate }) {
  return (
    <div className="relative w-full overflow-hidden rounded-[1.6rem] border border-cream-200 bg-white p-4 shadow-2xl dark:border-white/10 dark:bg-grove-800 md:p-6">
      <div
        className="grid min-h-[520px] gap-3"
        style={{
          gridTemplateColumns: '1.05fr 0.9fr 0.85fr 0.95fr 0.95fr',
          gridTemplateRows: '1fr 1.05fr 1.15fr',
          gridTemplateAreas: `
            "ground b b c c"
            "ground a w d d"
            "truth  h h h h"
          `,
        }}
      >
        {buildings.map((b) => {
          const area = SCHEMATIC_GRID[b.code]
          if (!area) return null
          const color = occupancyColor(heat[b.id]?.level || b.liveStatus)
          const active = hovered?.id === b.id
          return (
            <button
              key={b.id}
              type="button"
              onMouseEnter={() => onHover(b.id)}
              onMouseLeave={() => onHover(null)}
              onClick={() => b.bookable && navigate(`/buildings/${b.id}`)}
              className="flex flex-col items-center justify-center rounded-2xl border-2 p-3 text-center transition-all duration-300"
              style={{
                gridArea: area.gridArea,
                borderColor: color,
                background: active ? `${color}22` : '#fffdf8',
                boxShadow: active ? `0 10px 28px ${color}33` : 'none',
                borderStyle: b.bookable ? 'solid' : 'dashed',
              }}
            >
              <div className="text-[10px] uppercase tracking-[0.2em]" style={{ color }}>{b.virtueName}</div>
              <div className="font-display text-xl md:text-2xl">{b.name}</div>
              <div className="mt-1 text-xs text-grove-500">
                {b.bookable ? `${b.availableNow}/${b.resources} free` : 'No bookings'}
              </div>
            </button>
          )
        })}
      </div>
      <AnimatePresence>
        {hovered && <HoverCard building={hovered} heat={heat[hovered.id]} />}
      </AnimatePresence>
    </div>
  )
}

function HoverCard({ building, heat }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 8 }}
      className="pointer-events-none absolute bottom-4 left-4 right-4 max-w-sm rounded-2xl bg-grove-900/92 p-4 text-cream-50 shadow-xl backdrop-blur md:left-auto md:right-4"
    >
      <div className="font-display text-xl">{building.name}</div>
      <div className="text-xs uppercase tracking-[0.2em] text-gold-400">{building.virtueName} · {building.code}</div>
      <p className="mt-2 line-clamp-2 text-sm text-cream-200">{building.description}</p>
      <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
        <div className="rounded-lg bg-white/10 p-2"><div className="text-lg font-bold">{building.resources}</div>rooms</div>
        <div className="rounded-lg bg-white/10 p-2"><div className="text-lg font-bold">{building.availableNow}</div>free</div>
        <div className="rounded-lg bg-white/10 p-2"><div className="text-lg font-bold">{heat?.percent ?? 0}%</div>in use</div>
      </div>
      {!building.bookable && <p className="mt-2 text-xs text-gold-400">Sports ground — viewing only.</p>}
    </motion.div>
  )
}
