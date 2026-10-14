import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../utils/status'

const demos = [
  { email: 'student@example.com', role: 'Student' },
  { email: 'professor@example.com', role: 'Professor' },
  { email: 'admin@example.com', role: 'Admin' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('student@example.com')
  const [password, setPassword] = useState('Password@123')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await login(email, password)
      toast.success('Welcome back')
      navigate('/dashboard')
    } catch (err) {
      toast.error(errorMessage(err, 'Invalid email or password'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden lg:block">
        <img src="/campus.jpg" alt="" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-grove-900/55" />
        <div className="absolute bottom-12 left-12 right-12 text-cream-50">
          <p className="text-xs uppercase tracking-[0.28em] text-gold-400">VVIT · Nambur</p>
          <div className="mt-2 font-display text-5xl">Walk the campus before you book it.</div>
        </div>
      </div>
      <div className="flex items-center justify-center p-8">
        <motion.form initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} onSubmit={submit} className="w-full max-w-md space-y-4">
          <h1 className="font-display text-4xl">Sign in</h1>
          <p className="text-sm text-grove-600">Demo password for every role: <code>Password@123</code></p>
          <label className="block text-sm font-medium">Email
            <input className="field mt-1" value={email} onChange={(e) => setEmail(e.target.value)} required type="email" />
          </label>
          <label className="block text-sm font-medium">Password
            <input className="field mt-1" value={password} onChange={(e) => setPassword(e.target.value)} required type="password" />
          </label>
          <button disabled={loading} className="btn-primary w-full py-3">{loading ? 'Signing in…' : 'Sign in'}</button>
          <div className="grid grid-cols-3 gap-2">
            {demos.map((d) => (
              <button type="button" key={d.email} onClick={() => setEmail(d.email)} className="rounded-xl border px-2 py-2 text-xs hover:bg-cream-100 dark:border-white/10">{d.role}</button>
            ))}
          </div>
          <p className="text-sm">New here? <Link className="font-semibold text-grove-700" to="/register">Create an account</Link></p>
        </motion.form>
      </div>
    </div>
  )
}
