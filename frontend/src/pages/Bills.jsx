import { useEffect, useState } from 'react'
import api, { errMsg } from '../api/client'
import { Badge, Button, Card, ErrorNote, Table, money } from '../components/ui.jsx'

function download(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

/** FR-02.5 / UC-06 - billing history with printable invoices (FR-02.4). */
export default function Bills() {
  const [bills, setBills] = useState([])
  const [error, setError] = useState('')

  const load = async () => {
    const { data } = await api.get('/bills')
    setBills(data)
  }

  useEffect(() => {
    load().catch((e) => setError(errMsg(e)))
  }, [])

  const pdf = async (id) => {
    const { data } = await api.get(`/bills/${id}/pdf`, { responseType: 'blob' })
    download(data, `invoice-${id}.pdf`)
  }

  const markPaid = async (id) => {
    await api.patch(`/bills/${id}/pay`)
    await load()
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Billing History</h1>
      <ErrorNote message={error} />
      <Card>
        <Table headers={['Bill #', 'Order #', 'Table', 'Total', 'Tax', 'Discount', 'Status', 'Generated', 'Actions']}>
          {bills.slice(0, 100).map((b) => (
            <tr key={b.billId}>
              <td className="py-2 pr-4">{b.billId}</td>
              <td className="py-2 pr-4">{b.order.orderId}</td>
              <td className="py-2 pr-4">{b.order.tableNumber ?? '—'}</td>
              <td className="py-2 pr-4 font-medium">{money(b.totalAmount)}</td>
              <td className="py-2 pr-4">{money(b.taxAmount)}</td>
              <td className="py-2 pr-4">{money(b.discountAmount)}</td>
              <td className="py-2 pr-4">
                <Badge color={b.paymentStatus === 'PAID' ? 'green' : 'amber'}>{b.paymentStatus}</Badge>
              </td>
              <td className="py-2 pr-4 text-slate-500">{new Date(b.generatedAt).toLocaleString()}</td>
              <td className="py-2 pr-4">
                <div className="flex gap-2">
                  <Button variant="secondary" onClick={() => pdf(b.billId)}>PDF</Button>
                  {b.paymentStatus === 'UNPAID' && (
                    <Button variant="success" onClick={() => markPaid(b.billId)}>Mark Paid</Button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </Table>
        {bills.length > 100 && (
          <p className="mt-2 text-xs text-slate-400">Showing the 100 most recent bills of {bills.length}.</p>
        )}
      </Card>
    </div>
  )
}
