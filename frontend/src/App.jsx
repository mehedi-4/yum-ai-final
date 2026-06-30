import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Orders from './pages/Orders.jsx'
import Menu from './pages/Menu.jsx'
import Inventory from './pages/Inventory.jsx'
import Bills from './pages/Bills.jsx'
import Reports from './pages/Reports.jsx'
import Chat from './pages/Chat.jsx'
import Users from './pages/Users.jsx'

function Protected({ minRole, children }) {
  const { user, hasRole } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (minRole && !hasRole(minRole)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        {/* UC-10: dashboard for all roles */}
        <Route index element={<Dashboard />} />
        {/* UC-04/05: orders for Staff and above */}
        <Route path="orders" element={<Protected minRole="STAFF"><Orders /></Protected>} />
        <Route path="menu" element={<Protected minRole="STAFF"><Menu /></Protected>} />
        <Route path="inventory" element={<Protected minRole="STAFF"><Inventory /></Protected>} />
        {/* UC-06: billing history Manager/Admin */}
        <Route path="bills" element={<Protected minRole="MANAGER"><Bills /></Protected>} />
        {/* UC-11/12: reports Manager/Admin */}
        <Route path="reports" element={<Protected minRole="MANAGER"><Reports /></Protected>} />
        {/* UC-14: AI chat Manager/Admin */}
        <Route path="chat" element={<Protected minRole="MANAGER"><Chat /></Protected>} />
        {/* UC-02: user management Manager (read) / Admin (write) */}
        <Route path="users" element={<Protected minRole="MANAGER"><Users /></Protected>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
