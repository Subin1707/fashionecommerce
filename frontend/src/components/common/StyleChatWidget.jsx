import { useEffect, useRef, useState } from 'react'
import * as chatbotService from '../../services/chatbotService'
import { SafeImage } from './SafeImage'
import { formatCurrency } from '../../utils/formatCurrency'
import { ROUTES } from '../../utils/constants'

const STORAGE_KEY = 'fashion:stylist-chat-history:v2'

function readHistory() {
  try {
    const value = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(value) ? value.slice(-24) : []
  } catch {
    return []
  }
}

export function StyleChatWidget({ setRoute, route }) {
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState(readHistory)
  const [loading, setLoading] = useState(false)
  const [products, setProducts] = useState([])
  const endRef = useRef(null)
  const currentProductId = new URLSearchParams((route || '').split('?')[1] || '').get('id')

  useEffect(() => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages.slice(-24)))
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, open])

  async function send(event) {
    event.preventDefault()
    const text = message.trim()
    if (!text || loading) return
    const history = messages.slice(-12).map(({ role, content }) => ({ role, content }))
    setMessages((current) => [...current, { role: 'user', content: text }])
    setMessage('')
    setLoading(true)
    try {
      const result = await chatbotService.askChatbot(text, history, currentProductId ? Number(currentProductId) : null)
      const answer = typeof result === 'string' ? result : result?.answer || result?.message || 'No answer returned by chatbot.'
      setMessages((current) => [...current, { role: 'assistant', content: answer }])
      setProducts(typeof result === 'object' ? result?.products || [] : [])
    } catch (error) {
      const detail = error?.message || 'Unknown request error.'
      setMessages((current) => [...current, { role: 'assistant', content: 'Chat request failed: ' + detail }])
      setProducts([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="stylist-chat">
      {open && (
        <section className="stylist-chat-window" aria-label="Trợ lý sản phẩm và phong cách">
          <header className="stylist-chat-header">
            <div><strong>Stylist AI</strong><small>Hỏi sản phẩm và nhận gợi ý phối đồ</small></div>
            <button type="button" aria-label="Đóng chat" onClick={() => setOpen(false)}>×</button>
          </header>
          <div className="stylist-chat-messages" aria-live="polite">
            {!messages.length && <p className="stylist-chat-welcome">Mình có thể giúp bạn tìm hiểu sản phẩm, chất liệu, màu, size, giá và phối đồ theo dịp hoặc ngân sách. Bạn đang quan tâm món nào?</p>}
            {messages.map((item, index) => <div className={`stylist-chat-message ${item.role}`} key={`${index}-${item.role}`}><span>{item.content}</span></div>)}
            {loading && <div className="stylist-chat-message assistant"><span>Đang xem thông tin sản phẩm và nghĩ cách phối…</span></div>}
            {!!products.length && <div className="stylist-chat-products">{products.slice(0, 8).map((product) => <article className="stylist-chat-product" key={product.id}>
              <SafeImage src={product.primaryImageUrl} alt={product.name} fallbackLabel="Sản phẩm" />
              <div><strong>{product.name}</strong><small>{product.brandName || product.categoryName || 'Thời trang'} · {formatCurrency(product.salePrice || product.basePrice)}</small></div>
              <button type="button" onClick={() => setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(product.id)}`)}>Xem</button>
            </article>)}</div>}
            <div ref={endRef} />
          </div>
          <div className="stylist-chat-prompts">
            {['Tư vấn outfit đi làm dưới 1 triệu', 'Chất liệu sản phẩm này là gì?'].map((prompt) => <button type="button" key={prompt} onClick={() => setMessage(prompt)}>{prompt}</button>)}
          </div>
          <form className="stylist-chat-compose" onSubmit={send}>
            <textarea aria-label="Tin nhắn cho stylist" placeholder="Hỏi sản phẩm hoặc mô tả phong cách bạn muốn…" value={message} onChange={(event) => setMessage(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form.requestSubmit() } }} />
            <button type="submit" disabled={loading || !message.trim()}>Gửi</button>
          </form>
        </section>
      )}
      <button className="stylist-chat-launcher" type="button" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
        {open ? 'Đóng chat' : 'Hỏi stylist AI'}
      </button>
    </div>
  )
}
