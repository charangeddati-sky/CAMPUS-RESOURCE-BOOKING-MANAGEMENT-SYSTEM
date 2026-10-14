import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import client from '../../api/client'
import { FloorEditor } from '../../components/campus/FloorMap'

export default function FloorEditorPage() {
  const { id } = useParams()
  const [data, setData] = useState(null)
  useEffect(() => { client.get(`/floors/${id}`).then((r) => setData(r.data)) }, [id])
  if (!data) return <div className="soft-card h-64 animate-pulse" />
  return (
    <div>
      <h1 className="font-display text-3xl">Floor layout · {data.floor.buildingName} · {data.floor.name}</h1>
      <p className="mb-4 text-sm text-grove-600">Drag rooms to reposition. Width and height are percentages of the canvas.</p>
      <FloorEditor
        resources={data.resources}
        onSave={async (updates) => {
          await client.put(`/admin/floors/${id}/layout`, updates)
          toast.success('Layout saved')
        }}
      />
    </div>
  )
}
