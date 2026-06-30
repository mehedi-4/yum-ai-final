import { useEffect, useState } from 'react'
import api, { errMsg } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { Badge, Button, Card, ErrorNote, Input, Modal, Select, Table } from '../components/ui.jsx'

const ROLE_COLOR = { ADMIN: 'red', MANAGER: 'blue', STAFF: 'green', VIEWER: 'slate' }
const EMPTY = { name: '', email: '', password: '', role: 'STAFF' }

/** FR-01.4 / UC-02 - user management. Admin can write; Manager sees a read-only list. */
export default function Users() {
  const { user: me, hasRole } = useAuth()
  const isAdmin = hasRole('ADMIN')
  const [users, setUsers] = useState([])
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')

  const load = async () => {
    const { data } = await api.get('/users')
    setUsers(data)
  }

  useEffect(() => {
    load().catch((e) => setError(errMsg(e)))
  }, [])

  const save = async () => {
    setFormError('')
    try {
      if (editing.id) await api.put(`/users/${editing.id}`, editing.form)
      else await api.post('/users', editing.form)
      setEditing(null)
      await load()
    } catch (e) {
      setFormError(errMsg(e))
    }
  }

  const remove = async (id) => {
    if (!confirm('Delete this user?')) return
    try {
      await api.delete(`/users/${id}`)
      await load()
    } catch (e) {
      setError(errMsg(e))
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Users</h1>
        {isAdmin && (
          <Button onClick={() => { setFormError(''); setEditing({ id: null, form: { ...EMPTY } }) }}>+ Add User</Button>
        )}
      </div>
      <ErrorNote message={error} />
      {!isAdmin && (
        <p className="text-sm text-slate-500">Read-only view — only Admins can manage user accounts (FR-01.4).</p>
      )}

      <Card>
        <Table headers={['Name', 'Email', 'Role', 'Created', isAdmin ? 'Actions' : null].filter(Boolean)}>
          {users.map((u) => (
            <tr key={u.userId}>
              <td className="py-2 pr-4 font-medium">
                {u.name} {u.userId === me.userId && <span className="text-xs text-slate-400">(you)</span>}
              </td>
              <td className="py-2 pr-4">{u.email}</td>
              <td className="py-2 pr-4"><Badge color={ROLE_COLOR[u.role]}>{u.role}</Badge></td>
              <td className="py-2 pr-4 text-slate-500">{new Date(u.createdAt).toLocaleDateString()}</td>
              {isAdmin && (
                <td className="py-2 pr-4">
                  <div className="flex gap-2">
                    <Button variant="secondary" onClick={() => {
                      setFormError('')
                      setEditing({ id: u.userId, form: { name: u.name, email: u.email, password: '', role: u.role } })
                    }}>
                      Edit
                    </Button>
                    {u.userId !== me.userId && (
                      <Button variant="danger" onClick={() => remove(u.userId)}>Delete</Button>
                    )}
                  </div>
                </td>
              )}
            </tr>
          ))}
        </Table>
      </Card>

      <Modal open={!!editing} title={editing?.id ? 'Edit User' : 'Add User'} onClose={() => setEditing(null)}>
        {editing && (
          <div className="space-y-3">
            <ErrorNote message={formError} />
            <Input label="Name" value={editing.form.name}
                   onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, name: e.target.value } }))} />
            <Input label="Email" type="email" value={editing.form.email}
                   onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, email: e.target.value } }))} />
            <Input label={editing.id ? 'New password (leave blank to keep)' : 'Password'} type="password"
                   value={editing.form.password}
                   onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, password: e.target.value } }))} />
            <Select label="Role" value={editing.form.role}
                    onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, role: e.target.value } }))}>
              {['ADMIN', 'MANAGER', 'STAFF', 'VIEWER'].map((r) => <option key={r} value={r}>{r}</option>)}
            </Select>
            <Button onClick={save} className="w-full py-2">Save</Button>
          </div>
        )}
      </Modal>
    </div>
  )
}
