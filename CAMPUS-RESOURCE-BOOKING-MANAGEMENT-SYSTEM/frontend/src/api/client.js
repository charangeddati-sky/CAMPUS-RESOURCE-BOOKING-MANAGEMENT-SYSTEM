import axios from 'axios'

const client = axios.create({
  baseURL: '/api',
})

const getCache = new Map()
const TTL = 12_000

export function invalidateApiCache() {
  getCache.clear()
}

export function asItems(data) {
  if (Array.isArray(data)) return data
  return data?.items || []
}

export function cachedGet(url, config = {}, ttl = TTL) {
  const key = `${url}|${JSON.stringify(config.params || {})}`
  const hit = getCache.get(key)
  if (hit?.data && Date.now() - hit.at < ttl) {
    return Promise.resolve({ data: hit.data })
  }
  if (hit?.pending) return hit.pending
  const pending = client.get(url, config).then((res) => {
    getCache.set(key, { at: Date.now(), data: res.data })
    return res
  }).catch((err) => {
    getCache.delete(key)
    throw err
  })
  getCache.set(key, { at: 0, pending })
  return pending
}

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('campusos_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (res) => {
    const method = (res.config?.method || 'get').toLowerCase()
    if (method !== 'get') {
      invalidateApiCache()
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      const path = window.location.pathname
      if (!path.startsWith('/login') && !path.startsWith('/register') && path !== '/') {
        localStorage.removeItem('campusos_token')
        localStorage.removeItem('campusos_refresh')
        localStorage.removeItem('campusos_user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default client
