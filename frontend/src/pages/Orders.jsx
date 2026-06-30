import { useEffect, useState } from 'react'
import api, { errMsg } from '../api/client'
import { Badge, Button, Card, ErrorNote, Input, Modal, Table, money } from '../components/ui.jsx'

const STATUS_COLOR = { PENDING: 'amber', COMPLETED: 'green', CANCELLED: 'red' }

export default function Orders() {
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  const [showNew, setShowNew] = useState(false)
  const [error, setError] = useState('')

  // POS cart state (FR-02.1)
  const [cart, setCart] = useState({}) // menuItemId -> qty
  const [tableNumber, setTableNumber] = useState('')
  const [discount, setDiscount] = useState(0)
  const [cartError, setCartError] = useState('')

  const load = async () => {
    const [o, m] = await Promise.all([api.get('/orders'), api.get('/menu')])
    setOrders(o.data)
    setMenu(m.data)
  }

  useEffect(() => {
    load().catch((e) => setError(errMsg(e)))
  }, [])

  const addToCart = (id) => setCart((c) => ({ ...c, [id]: (c[id] ?? 0) + 1 }))
  const removeFromCart = (id) =>
    setCart((c) => {
      const next = { ...c }
      if (next[id] > 1) next[id] -= 1
      else delete next[id]
      return next
    })

  const cartItems = Object.entries(cart).map(([id, qty]) => ({
    item: menu.find((m) => m.menuItemId === Number(id)),
    qty,
  }))
  const subtotal = cartItems.reduce((s, { item, qty }) => s + (item?.price ?? 0) * qty, 0)

  const placeOrder = async () => {
    setCartError('')
    try {
      await api.post('/orders', {
        tableNumber: tableNumber || null,
        discountPercent: Number(discount) || 0,
        items: Object.entries(cart).map(([menuItemId, quantity]) => ({
          menuItemId: Number(menuItemId),
          quantity,
        })),
      })
      setCart({})
      setTableNumber('')
      setDiscount(0)
      setShowNew(false)
      await load()
    } catch (e) {
      setCartError(errMsg(e))
    }
  }

  const act = async (id, action) => {
    setError('')
    try {
      await api.post(`/orders/${id}/${action}`)
      await load()
    } catch (e) {
      setError(errMsg(e))
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Orders & POS</h1>
        <Button onClick={() => setShowNew(true)}>+ New Order</Button>
      </div>
      <ErrorNote message={error} />

      <Card>
        <Table headers={['#', 'Table', 'Items', 'Discount', 'Tax', 'Total', 'Status', 'Created', 'Actions']}>
          {orders.map((o) => (
            <tr key={o.orderId}>
              <td className="py-2 pr-4">{o.orderId}</td>
              <td className="py-2 pr-4">{o.tableNumber ?? '—'}</td>
              <td className="py-2 pr-4">
                {o.items.map((i) => `${i.menuItem.name} ×${i.quantity}`).join(', ')}
              </td>
              <td className="py-2 pr-4">{money(o.discountAmount)}</td>
              <td className="py-2 pr-4">{money(o.taxAmount)}</td>
              <td className="py-2 pr-4 font-medium">{money(o.totalAmount)}</td>
              <td className="py-2 pr-4">
                <Badge color={STATUS_COLOR[o.status]}>{o.status}</Badge>
              </td>
              <td className="py-2 pr-4 text-slate-500">{new Date(o.createdAt).toLocaleString()}</td>
              <td className="py-2 pr-4">
                {o.status === 'PENDING' && (
                  <div className="flex gap-2">
                    <Button variant="success" onClick={() => act(o.orderId, 'complete')}>Complete</Button>
                    <Button variant="danger" onClick={() => act(o.orderId, 'cancel')}>Cancel</Button>
                  </div>
                )}
              </td>
            </tr>
          ))}
        </Table>
        {orders.length === 0 && <p className="py-4 text-sm text-slate-400">No orders yet.</p>}
      </Card>

      <Modal open={showNew} title="New Order" onClose={() => setShowNew(false)}>
        <ErrorNote message={cartError} />
        <div className="mb-4 grid grid-cols-2 gap-3">
          <Input label="Table number" value={tableNumber} onChange={(e) => setTableNumber(e.target.value)}
                 placeholder="T1" />
          <Input label="Discount %" type="number" min="0" max="100" value={discount}
                 onChange={(e) => setDiscount(e.target.value)} />
        </div>
        <div className="mb-4 grid max-h-64 grid-cols-2 gap-2 overflow-y-auto">
          {menu.filter((m) => m.isAvailable).map((m) => (
            <button
              key={m.menuItemId}
              onClick={() => addToCart(m.menuItemId)}
              className="rounded-lg border border-slate-200 p-2 text-left text-sm hover:border-orange-400 hover:bg-orange-50"
            >
              <span className="block font-medium">{m.name}</span>
              <span className="text-slate-500">{money(m.price)}</span>
            </button>
          ))}
        </div>
        {cartItems.length > 0 && (
          <div className="mb-4 space-y-2 rounded-lg bg-slate-50 p-3">
            {cartItems.map(({ item, qty }) => (
              <div key={item.menuItemId} className="flex items-center justify-between text-sm">
                <span>{item.name} × {qty}</span>
                <span className="flex items-center gap-2">
                  {money(item.price * qty)}
                  <button onClick={() => removeFromCart(item.menuItemId)}
                          className="text-red-400 hover:text-red-600">−</button>
                </span>
              </div>
            ))}
            <div className="border-t pt-2 text-sm font-semibold">
              Subtotal: {money(subtotal)} <span className="font-normal text-slate-400">(tax & discount applied at billing)</span>
            </div>
          </div>
        )}
        <Button onClick={placeOrder} disabled={cartItems.length === 0} className="w-full py-2">
          Place Order
        </Button>
      </Modal>
    </div>
  )
}
