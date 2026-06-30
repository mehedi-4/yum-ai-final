import { useEffect, useState } from 'react'
import api, { errMsg } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { Badge, Button, Card, ErrorNote, Input, Modal, Select, Table, money } from '../components/ui.jsx'

const EMPTY = { name: '', description: '', price: '', costPrice: '', category: '', isAvailable: true }

export default function Menu() {
  const { hasRole } = useAuth()
  const canEdit = hasRole('MANAGER') // UC-03
  const [items, setItems] = useState([])
  const [inventory, setInventory] = useState([])
  const [editing, setEditing] = useState(null) // null | {form, id, ingredients}
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')

  const load = async () => {
    const requests = [api.get('/menu')]
    if (canEdit) requests.push(api.get('/inventory'))
    const [m, inv] = await Promise.all(requests)
    setItems(m.data)
    if (inv) setInventory(inv.data)
  }

  useEffect(() => {
    load().catch((e) => setError(errMsg(e)))
  }, [])

  const openEditor = (item) => {
    setFormError('')
    setEditing({
      id: item?.menuItemId ?? null,
      form: item
        ? { name: item.name, description: item.description ?? '', price: item.price,
            costPrice: item.costPrice, category: item.category, isAvailable: item.isAvailable }
        : { ...EMPTY },
      ingredients: item
        ? item.ingredients.map((i) => ({ inventoryItemId: i.inventoryItem.itemId, quantityNeeded: i.quantityNeeded }))
        : [],
    })
  }

  const save = async () => {
    setFormError('')
    const body = {
      ...editing.form,
      price: Number(editing.form.price),
      costPrice: Number(editing.form.costPrice) || 0,
      ingredients: editing.ingredients
        .filter((i) => i.inventoryItemId)
        .map((i) => ({ inventoryItemId: Number(i.inventoryItemId), quantityNeeded: Number(i.quantityNeeded) || 0 })),
    }
    try {
      if (editing.id) await api.put(`/menu/${editing.id}`, body)
      else await api.post('/menu', body)
      setEditing(null)
      await load()
    } catch (e) {
      setFormError(errMsg(e))
    }
  }

  const toggle = async (id) => {
    await api.patch(`/menu/${id}/availability`)
    await load()
  }

  const remove = async (id) => {
    if (!confirm('Delete this menu item?')) return
    try {
      await api.delete(`/menu/${id}`)
      await load()
    } catch (e) {
      setError(errMsg(e))
    }
  }

  const setForm = (k, v) => setEditing((ed) => ({ ...ed, form: { ...ed.form, [k]: v } }))
  const setIng = (idx, k, v) =>
    setEditing((ed) => {
      const ingredients = [...ed.ingredients]
      ingredients[idx] = { ...ingredients[idx], [k]: v }
      return { ...ed, ingredients }
    })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Menu</h1>
        {canEdit && <Button onClick={() => openEditor(null)}>+ Add Item</Button>}
      </div>
      <ErrorNote message={error} />

      <Card>
        <Table headers={['Name', 'Category', 'Price', canEdit ? 'Cost' : null, 'Available', canEdit ? 'Actions' : null].filter(Boolean)}>
          {items.map((m) => (
            <tr key={m.menuItemId}>
              <td className="py-2 pr-4">
                <span className="font-medium">{m.name}</span>
                {m.description && <span className="block text-xs text-slate-400">{m.description}</span>}
              </td>
              <td className="py-2 pr-4">{m.category}</td>
              <td className="py-2 pr-4">{money(m.price)}</td>
              {canEdit && <td className="py-2 pr-4 text-slate-500">{money(m.costPrice)}</td>}
              <td className="py-2 pr-4">
                <Badge color={m.isAvailable ? 'green' : 'red'}>{m.isAvailable ? 'Available' : 'Unavailable'}</Badge>
              </td>
              {canEdit && (
                <td className="py-2 pr-4">
                  <div className="flex gap-2">
                    <Button variant="secondary" onClick={() => openEditor(m)}>Edit</Button>
                    <Button variant="secondary" onClick={() => toggle(m.menuItemId)}>
                      {m.isAvailable ? 'Disable' : 'Enable'}
                    </Button>
                    <Button variant="danger" onClick={() => remove(m.menuItemId)}>Delete</Button>
                  </div>
                </td>
              )}
            </tr>
          ))}
        </Table>
      </Card>

      <Modal open={!!editing} title={editing?.id ? 'Edit Menu Item' : 'Add Menu Item'} onClose={() => setEditing(null)}>
        {editing && (
          <div className="space-y-3">
            <ErrorNote message={formError} />
            <Input label="Name" value={editing.form.name} onChange={(e) => setForm('name', e.target.value)} />
            <Input label="Description" value={editing.form.description}
                   onChange={(e) => setForm('description', e.target.value)} />
            <div className="grid grid-cols-3 gap-3">
              <Input label="Price" type="number" step="0.01" value={editing.form.price}
                     onChange={(e) => setForm('price', e.target.value)} />
              <Input label="Cost" type="number" step="0.01" value={editing.form.costPrice}
                     onChange={(e) => setForm('costPrice', e.target.value)} />
              <Input label="Category" value={editing.form.category}
                     onChange={(e) => setForm('category', e.target.value)} />
            </div>

            <div>
              <p className="mb-1 text-sm font-medium text-slate-600">
                Ingredients (auto-deducted on order completion)
              </p>
              {editing.ingredients.map((ing, idx) => (
                <div key={idx} className="mb-2 flex items-end gap-2">
                  <div className="flex-1">
                    <Select value={ing.inventoryItemId} onChange={(e) => setIng(idx, 'inventoryItemId', e.target.value)}>
                      <option value="">Select ingredient…</option>
                      {inventory.map((i) => (
                        <option key={i.itemId} value={i.itemId}>{i.name} ({i.unit})</option>
                      ))}
                    </Select>
                  </div>
                  <div className="w-28">
                    <Input type="number" step="0.01" placeholder="Qty" value={ing.quantityNeeded}
                           onChange={(e) => setIng(idx, 'quantityNeeded', e.target.value)} />
                  </div>
                  <Button variant="danger" onClick={() =>
                    setEditing((ed) => ({ ...ed, ingredients: ed.ingredients.filter((_, i) => i !== idx) }))}>
                    ✕
                  </Button>
                </div>
              ))}
              <Button variant="secondary" onClick={() =>
                setEditing((ed) => ({ ...ed, ingredients: [...ed.ingredients, { inventoryItemId: '', quantityNeeded: '' }] }))}>
                + Add ingredient
              </Button>
            </div>

            <Button onClick={save} className="w-full py-2">Save</Button>
          </div>
        )}
      </Modal>
    </div>
  )
}
