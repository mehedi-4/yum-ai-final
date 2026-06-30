import { useEffect, useRef, useState } from 'react'
import api, { errMsg } from '../api/client'
import { Button, Input } from '../components/ui.jsx'

/** FR-05.4/05.5 / UC-14 - natural-language assistant powered by Gemini. */
export default function Chat() {
  const [messages, setMessages] = useState([
    { from: 'ai', text: 'Hi! Ask me about sales, inventory status, waste, or business recommendations.' },
  ])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const bottom = useRef(null)

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = async (e) => {
    e.preventDefault()
    const text = input.trim()
    if (!text || busy) return
    setInput('')
    setMessages((m) => [...m, { from: 'user', text }])
    setBusy(true)
    try {
      const { data } = await api.post('/ai/chat', { message: text })
      setMessages((m) => [...m, { from: 'ai', text: data.reply, offline: !data.fromGemini }])
    } catch (err) {
      setMessages((m) => [...m, { from: 'ai', text: errMsg(err), error: true }])
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex h-[calc(100vh-6rem)] flex-col">
      <h1 className="mb-4 text-2xl font-bold">AI Assistant</h1>
      <div className="flex-1 space-y-3 overflow-y-auto rounded-xl bg-white p-4 shadow-sm">
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.from === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[75%] whitespace-pre-wrap rounded-2xl px-4 py-2 text-sm ${
              m.from === 'user'
                ? 'bg-orange-500 text-white'
                : m.error ? 'bg-red-50 text-red-600' : 'bg-slate-100 text-slate-700'
            }`}>
              {m.text}
              {m.offline && (
                <span className="mt-1 block text-[10px] text-slate-400">built-in analytics (Gemini offline)</span>
              )}
            </div>
          </div>
        ))}
        {busy && <p className="text-sm text-slate-400">Thinking…</p>}
        <div ref={bottom} />
      </div>
      <form onSubmit={send} className="mt-3 flex gap-2">
        <div className="flex-1">
          <Input value={input} onChange={(e) => setInput(e.target.value)}
                 placeholder="e.g. What are this week's best sellers?" />
        </div>
        <Button type="submit" disabled={busy || !input.trim()}>Send</Button>
      </form>
    </div>
  )
}
