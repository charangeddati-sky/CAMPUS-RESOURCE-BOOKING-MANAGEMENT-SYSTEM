import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../utils/status'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '', department: 'CSE', role: 'STUDENT' })
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault()
    try {
      await register(form)
      toast.success('Account created')
      navigate('/campus')
    } catch (err) {
      toast.error(errorMessage(err, 'Could not register'))
    }
  }

  return (
    <div className="mx-auto flex min-h-screen max-w-lg items-center px-6">
      <form onSubmit={submit} className="w-full space-y-4">
        <h1 className="font-display text-4xl">Join CampusOS</h1>
        {['fullName', 'email', 'password', 'department'].map((k) => (
          <label key={k} className="block text-sm font-medium capitalize">{k}
            <input className="mt-1 w-full rounded-xl border px-3 py-2 dark:bg-grove-800" type={k === 'password' ? 'password' : 'text'} value={form[k]} onChange={(e) => set(k, e.target.value)} required />
          </label>
        ))}
        <label className="block text-sm font-medium">Role
          <select className="mt-1 w-full rounded-xl border px-3 py-2 dark:bg-grove-800" value={form.role} onChange={(e) => set('role', e.target.value)}>
            <option>STUDENT</option>
            <option>PROFESSOR</option>
          </select>
        </label>
        <button className="w-full rounded-full bg-grove-700 py-3 font-semibold text-white">Create account</button>
        <Link to="/login" className="block text-sm">Already have an account? Sign in</Link>
      </form>
    </div>
  )
}
