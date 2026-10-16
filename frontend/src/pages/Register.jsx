import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Field from '../components/ui/Field'
import useFetch from '../hooks/useFetch'
import api from '../api/client'
import { authApi } from '../api/services'

const fallbackDepartments = [
  { id: 1, name: 'Engineering' },
  { id: 2, name: 'HR' },
  { id: 3, name: 'Operations' },
  { id: 4, name: 'Finance' },
]

function validate(f) {
  const e = {}
  if (!f.firstName.trim()) e.firstName = 'Enter your first name'
  if (!f.lastName.trim()) e.lastName = 'Enter your last name'
  if (!/^\S+@\S+\.\S+$/.test(f.email)) e.email = 'Enter a valid email address'
  if (!f.departmentId) e.departmentId = 'Choose your department'
  if (f.password.length < 8 || !/\d/.test(f.password))
    e.password = 'Use at least 8 characters, including a number'
  if (f.confirm !== f.password) e.confirm = "Passwords don't match"
  return e
}

export default function Register() {
  const navigate = useNavigate()
  const { data: departments } = useFetch(() => api.get('/departments'), fallbackDepartments)
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', departmentId: '', password: '', confirm: '',
  })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    const found = validate(form)
    setErrors(found)
    setServerError('')
    if (Object.keys(found).length) return

    setBusy(true)
    try {
      const { confirm, ...payload } = form
      await authApi.register({ ...payload, departmentId: Number(payload.departmentId) })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (err) {
      if (err.response?.status === 409) setErrors({ email: 'That email is already registered' })
      else setServerError(err.response?.data?.message || "Couldn't create your account. Try again.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <form onSubmit={submit} noValidate className="card w-full max-w-md space-y-4">
        <div>
          <h1 className="text-xl font-semibold">Create your account</h1>
          <p className="text-sm text-slate-500">Knowledge Gap Intelligence Platform</p>
        </div>

        {serverError && <div className="rounded-lg bg-red-50 p-2 text-sm text-red-600">{serverError}</div>}

        <div className="grid grid-cols-2 gap-3">
          <Field label="First name" value={form.firstName} onChange={set('firstName')} error={errors.firstName} />
          <Field label="Last name" value={form.lastName} onChange={set('lastName')} error={errors.lastName} />
        </div>
        <Field label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />

        <Field label="Department" error={errors.departmentId}>
          <select className="input" value={form.departmentId} onChange={set('departmentId')}>
            <option value="">Choose department</option>
            {departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
        </Field>

        <Field label="Password" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} error={errors.password} />
        <Field label="Confirm password" type="password" autoComplete="new-password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} />

        <button className="btn-primary w-full" disabled={busy}>
          {busy ? 'Creating account...' : 'Create account'}
        </button>
        <a className="btn-outline w-full" href="/oauth2/authorization/google">Continue with Google</a>

        <p className="text-center text-sm text-slate-500">
          Already registered? <Link to="/login" className="text-brand-600">Sign in</Link>
        </p>
      </form>
    </div>
  )
}