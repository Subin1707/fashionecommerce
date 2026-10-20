import { useEffect, useRef, useState } from 'react'
import * as chatbotService from '../../services/chatbotService'
import { SafeImage } from './SafeImage'
import { formatCurrency } from '../../utils/formatCurrency'
import { ROUTES } from '../../utils/constants'
import { repairCatalogText, repairProduct } from '../../utils/repairCatalogText'

const STORAGE_KEY = 'fashion:stylist-chat-history:v3'

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
      const answer = repairCatalogText(typeof result === 'string' ? result : result?.answer || result?.message || 'Trợ lý chưa có câu trả lời. Bạn vui lòng thử lại.')
      setMessages((current) => [...current, { role: 'assistant', content: answer }])
      setProducts(typeof result === 'object' ? (result?.products || []).map(repairProduct) : [])
    } catch (error) {
      const detail = repairCatalogText(error?.message || 'Đã xảy ra lỗi không xác định.')
      setMessages((current) => [...current, { role: 'assistant', content: `Chưa gửi được câu hỏi: ${detail}` }])
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
            <div><strong>Trợ lý thời trang AI</strong><small>Hỏi về sản phẩm và nhận gợi ý phối đồ</small></div>
            <button type="button" aria-label="Đóng trò chuyện" onClick={() => setOpen(false)}>×</button>
          </header>
          <div className="stylist-chat-messages" aria-live="polite">
            {!messages.length && <p className="stylist-chat-welcome">Mình có thể giúp bạn tìm hiểu sản phẩm, chất liệu, màu sắc, kích cỡ, giá và cách phối đồ theo dịp hoặc ngân sách. Bạn đang quan tâm món nào?</p>}
            {messages.map((item, index) => <div className={`stylist-chat-message ${item.role}`} key={`${index}-${item.role}`}><span>{repairCatalogText(item.content)}</span></div>)}
            {loading && <div className="stylist-chat-message assistant"><span>Đang xem thông tin sản phẩm và nghĩ cách phối đồ…</span></div>}
            {!!products.length && <div className="stylist-chat-products">{products.slice(0, 8).map((product) => <article className="stylist-chat-product" key={product.id}>
              <SafeImage src={product.primaryImageUrl} alt={product.name} fallbackLabel="Sản phẩm" />
              <div><strong>{product.name}</strong><small>{product.brandName || product.categoryName || 'Thời trang'} · {formatCurrency(product.salePrice || product.basePrice)}</small></div>
              <button type="button" onClick={() => setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(product.id)}`)}>Xem</button>
            </article>)}</div>}
            <div ref={endRef} />
          </div>
          <div className="stylist-chat-prompts">
            {['Gợi ý trang phục đi làm dưới 1 triệu', 'Chất liệu sản phẩm này là gì?'].map((prompt) => <button type="button" key={prompt} onClick={() => setMessage(prompt)}>{prompt}</button>)}
          </div>
          <form className="stylist-chat-compose" onSubmit={send}>
            <textarea aria-label="Tin nhắn cho trợ lý thời trang" placeholder="Hỏi về sản phẩm hoặc mô tả phong cách bạn muốn…" value={message} onChange={(event) => setMessage(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form.requestSubmit() } }} />
            <button type="submit" disabled={loading || !message.trim()}>Gửi</button>
          </form>
        </section>
      )}
      <button className="stylist-chat-launcher" type="button" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
        {open ? 'Đóng trò chuyện' : 'Hỏi trợ lý thời trang'}
      </button>
    </div>
  )
}
