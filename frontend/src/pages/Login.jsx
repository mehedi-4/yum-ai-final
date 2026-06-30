import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, ErrorNote, Input } from '../components/ui.jsx'
import { errMsg } from '../api/client'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-900 p-4">
      <form onSubmit={submit} className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-xl">
        <h1 className="text-3xl font-bold text-orange-500">YumAI</h1>
        <p className="mb-6 mt-1 text-sm text-slate-500">AI-Powered Restaurant Management</p>
        <ErrorNote message={error} />
        <div className="space-y-4">
          <Input label="Email" type="email" value={email} required
                 onChange={(e) => setEmail(e.target.value)} placeholder="admin@yumai.com" />
          <Input label="Password" type="password" value={password} required
                 onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          <Button type="submit" disabled={busy} className="w-full py-2">
            {busy ? 'Signing in...' : 'Sign in'}
          </Button>
        </div>
        <p className="mt-6 text-xs text-slate-400">
          Demo accounts: admin@yumai.com / Admin@123 · manager@yumai.com / Manager@123 ·
          staff@yumai.com / Staff@123 · viewer@yumai.com / Viewer@123
        </p>
      </form>
    </div>
  )
}
