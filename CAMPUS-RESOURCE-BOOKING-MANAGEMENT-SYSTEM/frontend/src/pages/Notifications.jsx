import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'

export default function Notifications() {
  const [rows, setRows] = useState([])
  const load = () => client.get('/notifications').then((r) => setRows(r.data))
  useEffect(() => { load() }, [])
  return (
    <div>
      <div className="mb-4 flex justify-between">
        <h1 className="font-display text-4xl">Notifications</h1>
        <button className="text-sm" onClick={async () => { await client.post('/notifications/read-all'); load() }}>Mark all read</button>
      </div>
      <div className="space-y-2">
        {rows.map((n) => (
          <Link key={n.id} to={n.link || '/dashboard'} onClick={() => client.post(`/notifications/${n.id}/read`)} className={`soft-card block p-4 ${n.readFlag ? 'opacity-60' : ''}`}>
            <div className="font-semibold">{n.title}</div>
            <div className="text-sm">{n.message}</div>
          </Link>
        ))}
      </div>
    </div>
  )
}
