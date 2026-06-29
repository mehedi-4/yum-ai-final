import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

// Persistent sidebar navigation (SRS 3.3.1)
const NAV = [
  { to: '/', label: 'Dashboard', icon: '📊', minRole: 'VIEWER', end: true },
  { to: '/orders', label: 'Orders & POS', icon: '🧾', minRole: 'STAFF' },
  { to: '/menu', label: 'Menu', icon: '🍔', minRole: 'STAFF' },
  { to: '/inventory', label: 'Inventory', icon: '📦', minRole: 'STAFF' },
  { to: '/bills', label: 'Billing History', icon: '💳', minRole: 'MANAGER' },
  { to: '/reports', label: 'Reports', icon: '📑', minRole: 'MANAGER' },
  { to: '/chat', label: 'AI Assistant', icon: '💬', minRole: 'MANAGER' },
  { to: '/users', label: 'Users', icon: '👥', minRole: 'MANAGER' },
]

export default function Layout() {
  const { user, logout, hasRole } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="flex min-h-screen">
      <aside className="w-60 shrink-0 bg-slate-900 text-slate-200 flex flex-col">
        <div className="px-5 py-6">
          <h1 className="text-2xl font-bold text-orange-400">YumAI</h1>
          <p className="text-xs text-slate-400 mt-1">Restaurant Management SaaS</p>
        </div>
        <nav className="flex-1 px-3 space-y-1">
          {NAV.filter((n) => hasRole(n.minRole)).map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${
                  isActive ? 'bg-orange-500 text-white' : 'hover:bg-slate-800'
                }`
              }
            >
              <span>{n.icon}</span>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-700 p-4">
          <p className="text-sm font-medium">{user?.name}</p>
          <p className="text-xs text-slate-400">{user?.role}</p>
          <button
            onClick={handleLogout}
            className="mt-3 w-full rounded-lg bg-slate-800 px-3 py-2 text-sm hover:bg-slate-700"
          >
            Logout
          </button>
        </div>
      </aside>
      <main className="flex-1 p-6 overflow-x-hidden">
        <Outlet />
      </main>
    </div>
  )
}
