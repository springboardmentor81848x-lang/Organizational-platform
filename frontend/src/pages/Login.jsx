import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import Field from '../components/ui/Field'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)

  if (isAuthenticated) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault()
    const found = {}
    if (!/^\S+@\S+\.\S+$/.test(form.email)) found.email = 'Enter a valid email address'
    if (!form.password) found.password = 'Enter your password'
    setErrors(found)
    setServerError('')
    if (Object.keys(found).length) return

    setBusy(true)
    try {
      await login(form.email, form.password)
      navigate(location.state?.from?.pathname || '/', { replace: true })
    } catch (err) {
      setServerError(err.response?.data?.message || 'Check your email and password, then try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <form onSubmit={submit} noValidate className="card w-full max-w-sm space-y-4">
        <div>
          <h1 className="text-xl font-semibold">Sign in</h1>
          <p className="text-sm text-slate-500">Knowledge Gap Intelligence Platform</p>
        </div>

        {location.state?.registered && (
          <div className="rounded-lg bg-emerald-50 p-2 text-sm text-emerald-700">
            Account created. Sign in to continue.
          </div>
        )}
        {serverError && <div className="rounded-lg bg-red-50 p-2 text-sm text-red-600">{serverError}</div>}

        <Field label="Email" type="email" autoComplete="email" value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })} error={errors.email} />
        <Field label="Password" type="password" autoComplete="current-password" value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })} error={errors.password} />

        <div className="text-right text-sm">
          <Link to="/forgot-password" className="text-brand-600">Forgot password?</Link>
        </div>

        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
        <a className="btn-outline w-full" href="/oauth2/authorization/google">Continue with Google</a>

        <p className="text-center text-sm text-slate-500">
          Don't have an account? <Link to="/register" className="text-brand-600">Create one</Link>
        </p>
      </form>
    </div>
  )
}