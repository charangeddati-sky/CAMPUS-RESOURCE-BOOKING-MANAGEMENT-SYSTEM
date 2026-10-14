import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'

export default function Favorites() {
  const [favs, setFavs] = useState([])
  const [recent, setRecent] = useState([])
  useEffect(() => {
    client.get('/favorites').then((r) => setFavs(r.data))
    client.get('/recent').then((r) => setRecent(r.data))
  }, [])
  return (
    <div className="grid gap-8 md:grid-cols-2">
      <section>
        <h1 className="font-display text-3xl">My favorites</h1>
        <div className="mt-3 space-y-2">
          {favs.map((r) => <Link key={r.id} to={`/resources/${r.id}`} className="soft-card block p-3">⭐ {r.name}</Link>)}
          {favs.length === 0 && <p>No favorites yet.</p>}
        </div>
      </section>
      <section>
        <h2 className="font-display text-3xl">Recently viewed</h2>
        <div className="mt-3 space-y-2">
          {recent.map((r) => <Link key={r.id} to={`/resources/${r.id}`} className="soft-card block p-3">{r.name}</Link>)}
        </div>
      </section>
    </div>
  )
}
