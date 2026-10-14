import { useEffect, useState } from 'react'
import client from '../api/client'

export default function AuthImage({ path, alt, className }) {
  const [url, setUrl] = useState('')
  useEffect(() => {
    let objectUrl
    client.get(path, { responseType: 'blob' }).then((r) => {
      objectUrl = URL.createObjectURL(r.data)
      setUrl(objectUrl)
    }).catch(() => setUrl(''))
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [path])
  if (!url) return <div className={`animate-pulse bg-cream-200 ${className}`} />
  return <img src={url} alt={alt} className={className} />
}

export async function downloadFile(path, filename) {
  const res = await client.get(path, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
