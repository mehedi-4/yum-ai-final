import { useEffect, useState } from 'react'
import api, { errMsg } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { Badge, Button, Card, ErrorNote, Input, Modal, Select, Table } from '../components/ui.jsx'

const EMPTY = { name: '', quantity: '', unit: '', category: '', lowStockThreshold: '' }

export default function Inventory() {
  const { hasRole } = useAuth()
  const canManage = hasRole('MANAGER') // FR-03.3
  const [items, setItems] = useState([])
  const [wasteLogs, setWasteLogs] = useState([])
  const [tab, setTab] = useState('stock')
  const [editing, setEditing] = useState(null)
  const [waste, setWaste] = useState(null) // {inventoryItemId, quantity, reason}
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')

  const load = async () => {
    const [inv, logs] = await Promise.all([api.get('/inventory'), api.get('/inventory/waste-logs')])
    setItems(inv.data)
    setWasteLogs(logs.data)
  }

  useEffect(() => {
    load().catch((e) => setError(errMsg(e)))
  }, [])

  const save = async () => {
    setFormError('')
    const body = {
      ...editing.form,
      quantity: Number(editing.form.quantity),
      lowStockThreshold: Number(editing.form.lowStockThreshold),
    }
    try {
      if (editing.id) await api.put(`/inventory/${editing.id}`, body)
      else await api.post('/inventory', body)
      setEditing(null)
      await load()
    } catch (e) {
      setFormError(errMsg(e))
    }
  }

  const logWaste = async () => {
    setFormError('')
    try {
      await api.post('/inventory/waste-logs', {
        inventoryItemId: Number(waste.inventoryItemId),
        quantity: Number(waste.quantity),
        reason: waste.reason,
      })
      setWaste(null)
      await load()
    } catch (e) {
      setFormError(errMsg(e))
    }
  }

  const remove = async (id) => {
    if (!confirm('Delete this inventory item?')) return
    try {
      await api.delete(`/inventory/${id}`)
      await load()
    } catch (e) {
      setError(errMsg(e))
    }
  }

  const lowCount = items.filter((i) => i.quantity <= i.lowStockThreshold).length

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Inventory</h1>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => { setFormError(''); setWaste({ inventoryItemId: '', quantity: '', reason: '' }) }}>
            Log Waste
          </Button>
          {canManage && (
            <Button onClick={() => { setFormError(''); setEditing({ id: null, form: { ...EMPTY } }) }}>+ Add Item</Button>
          )}
        </div>
      </div>
      <ErrorNote message={error} />

      {/* FR-03.2 low-stock alert banner */}
      {lowCount > 0 && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          ⚠ {lowCount} item{lowCount > 1 ? 's are' : ' is'} at or below the low-stock threshold.
        </div>
      )}

      <div className="flex gap-1">
        {[['stock', 'Stock'], ['waste', 'Waste Log']].map(([key, label]) => (
          <button key={key} onClick={() => setTab(key)}
                  className={`rounded-lg px-4 py-1.5 text-sm font-medium ${
                    tab === key ? 'bg-orange-500 text-white' : 'bg-white hover:bg-slate-50'
                  }`}>
            {label}
          </button>
        ))}
      </div>

      {tab === 'stock' ? (
        <Card>
          <Table headers={['Item', 'Category', 'Quantity', 'Threshold', 'Status', 'Last Updated', canManage ? 'Actions' : null].filter(Boolean)}>
            {items.map((i) => (
              <tr key={i.itemId} className={i.quantity <= i.lowStockThreshold ? 'bg-red-50' : ''}>
                <td className="py-2 pr-4 font-medium">{i.name}</td>
                <td className="py-2 pr-4">{i.category}</td>
                <td className="py-2 pr-4">{i.quantity} {i.unit}</td>
                <td className="py-2 pr-4 text-slate-500">{i.lowStockThreshold} {i.unit}</td>
                <td className="py-2 pr-4">
                  <Badge color={i.quantity <= i.lowStockThreshold ? 'red' : 'green'}>
                    {i.quantity <= i.lowStockThreshold ? 'LOW STOCK' : 'OK'}
                  </Badge>
                </td>
                <td className="py-2 pr-4 text-slate-500">{new Date(i.lastUpdated).toLocaleString()}</td>
                {canManage && (
                  <td className="py-2 pr-4">
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={() => {
                        setFormError('')
                        setEditing({ id: i.itemId, form: { name: i.name, quantity: i.quantity, unit: i.unit,
                          category: i.category, lowStockThreshold: i.lowStockThreshold } })
                      }}>
                        Edit
                      </Button>
                      <Button variant="danger" onClick={() => remove(i.itemId)}>Delete</Button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </Table>
        </Card>
      ) : (
        <Card>
          <Table headers={['Item', 'Quantity', 'Reason', 'Logged By', 'When']}>
            {wasteLogs.map((w) => (
              <tr key={w.logId}>
                <td className="py-2 pr-4 font-medium">{w.inventoryItem.name}</td>
                <td className="py-2 pr-4">{w.quantity} {w.inventoryItem.unit}</td>
                <td className="py-2 pr-4">{w.reason}</td>
                <td className="py-2 pr-4">{w.loggedBy.name}</td>
                <td className="py-2 pr-4 text-slate-500">{new Date(w.loggedAt).toLocaleString()}</td>
              </tr>
            ))}
          </Table>
          {wasteLogs.length === 0 && <p className="py-4 text-sm text-slate-400">No waste logged.</p>}
        </Card>
      )}

      <Modal open={!!editing} title={editing?.id ? 'Edit Inventory Item' : 'Add Inventory Item'} onClose={() => setEditing(null)}>
        {editing && (
          <div className="space-y-3">
            <ErrorNote message={formError} />
            <Input label="Name" value={editing.form.name}
                   onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, name: e.target.value } }))} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Quantity" type="number" step="0.01" value={editing.form.quantity}
                     onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, quantity: e.target.value } }))} />
              <Input label="Unit (kg, L, pcs…)" value={editing.form.unit}
                     onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, unit: e.target.value } }))} />
              <Input label="Category" value={editing.form.category}
                     onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, category: e.target.value } }))} />
              <Input label="Low-stock threshold" type="number" step="0.01" value={editing.form.lowStockThreshold}
                     onChange={(e) => setEditing((ed) => ({ ...ed, form: { ...ed.form, lowStockThreshold: e.target.value } }))} />
            </div>
            <Button onClick={save} className="w-full py-2">Save</Button>
          </div>
        )}
      </Modal>

      <Modal open={!!waste} title="Log Waste" onClose={() => setWaste(null)}>
        {waste && (
          <div className="space-y-3">
            <ErrorNote message={formError} />
            <Select label="Item" value={waste.inventoryItemId}
                    onChange={(e) => setWaste((w) => ({ ...w, inventoryItemId: e.target.value }))}>
              <option value="">Select item…</option>
              {items.map((i) => (
                <option key={i.itemId} value={i.itemId}>{i.name} ({i.quantity} {i.unit} in stock)</option>
              ))}
            </Select>
            <Input label="Quantity wasted" type="number" step="0.01" value={waste.quantity}
                   onChange={(e) => setWaste((w) => ({ ...w, quantity: e.target.value }))} />
            <Input label="Reason" value={waste.reason} placeholder="Spoiled, over-prepared…"
                   onChange={(e) => setWaste((w) => ({ ...w, reason: e.target.value }))} />
            <Button onClick={logWaste} disabled={!waste.inventoryItemId || !waste.quantity || !waste.reason}
                    className="w-full py-2">
              Log Waste
            </Button>
          </div>
        )}
      </Modal>
    </div>
  )
}
