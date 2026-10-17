import { useAuth } from '../context/AuthContext'

export default function Dashboard() {
  const { user, logout } = useAuth()
  return (
    <div className="mx-auto max-w-xl p-10">
      <div className="card space-y-3">
        <h1 className="text-xl font-semibold">Welcome, {user.firstName}</h1>
        <p className="text-sm text-slate-500">{user.email} - {user.roles.join(', ')}</p>
        <button className="btn-outline" onClick={logout}>Log out</button>
      </div>
    </div>
  )
}