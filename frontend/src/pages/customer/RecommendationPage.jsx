import { useMemo, useState } from 'react'
import * as chatbotService from '../../services/chatbotService'
import * as productService from '../../services/productService'
import { ROUTES } from '../../utils/constants'
import { formatCurrency } from '../../utils/formatCurrency'
import { useCart } from '../../hooks/useCart'
import { SafeImage } from '../../components/common/SafeImage'

const bodyFields = [
  { key: 'height', label: 'Chiều cao', unit: 'cm', min: 120, max: 210 },
  { key: 'weight', label: 'Cân nặng', unit: 'kg', min: 30, max: 140 },
  { key: 'chest', label: 'Vòng ngực', unit: 'cm', min: 50, max: 140 },
  { key: 'waist', label: 'Vòng eo', unit: 'cm', min: 45, max: 130 },
  { key: 'hip', label: 'Vòng hông', unit: 'cm', min: 55, max: 150 },
  { key: 'age', label: 'Tuổi', unit: 'tuổi', min: 12, max: 80 },
]

const stylePresets = [
  { label: 'Công sở', style: 'office', category: 'đầm', color: 'sand', fashionType: 'female' },
  { label: 'Dạo phố', style: 'streetwear', category: 'áo khoác', color: 'black', fashionType: 'unisex' },
  { label: 'Tối giản', style: 'minimal', category: 'phụ kiện', color: 'black', fashionType: 'female' },
]

const quizOptions = {
  occasion: ['Đi học', 'Đi làm', 'Đi chơi', 'Hẹn hò', 'Sự kiện'],
  style: ['Basic', 'Streetwear', 'Minimal', 'Vintage', 'Elegant', 'Sporty', 'Y2K'],
  color: ['Trắng / Be', 'Đen', 'Xanh', 'Pastel', 'Màu nổi'],
}

export function RecommendationPage({ setNotice, setRoute }) {
  const { addToCart } = useCart()
  const [sizeForm, setSizeForm] = useState({ height: 165, weight: 55, chest: 84, waist: 68, hip: 92, age: 25 })
  const [styleForm, setStyleForm] = useState({
    color: 'black',
    style: 'streetwear',
    category: 'áo khoác',
    pattern: '',
    fashionType: 'unisex',
  })
  const [smartSize, setSmartSize] = useState(null)
  const [styleProducts, setStyleProducts] = useState([])
  const [outfits, setOutfits] = useState([])
  const [quiz, setQuiz] = useState({ occasion: 'Đi chơi', style: 'Minimal', gender: 'unisex', budget: 1000000, color: 'Trắng / Be', dislikedColor: '' })
  const [productId, setProductId] = useState(null)
  const [sizeLoading, setSizeLoading] = useState(false)
  const [styleLoading, setStyleLoading] = useState(false)
  const [addingOutfit, setAddingOutfit] = useState(null)

  const bodySummary = useMemo(() => {
    const heightMeters = Number(sizeForm.height || 0) / 100
    const bmi = heightMeters ? Number(sizeForm.weight || 0) / (heightMeters * heightMeters) : 0
    const bodyShape = Number(sizeForm.hip || 0) > Number(sizeForm.chest || 0) + 6
      ? 'Dáng quả lê'
      : Number(sizeForm.chest || 0) > Number(sizeForm.hip || 0) + 6
        ? 'Dáng tam giác ngược'
        : 'Dáng cân đối'

    return {
      bmi: bmi ? bmi.toFixed(1) : '-',
      bodyShape,
      fitHint: Number(sizeForm.waist || 0) > 78 ? 'Ưu tiên form regular hoặc relaxed' : 'Có thể thử slim hoặc regular',
    }
  }, [sizeForm])

  async function runSmartSize(event) {
    event.preventDefault()
    setSizeLoading(true)
    try {
      const products = await productService.getProducts()
      const product = products.find((item) => item.status === 'ACTIVE') || products[0]
      if (!product) throw new Error('Chưa có sản phẩm thật để gợi ý kích cỡ.')
      setProductId(product.id)
      setSmartSize(await chatbotService.recommendSize(product.id, sizeForm))
      setNotice?.('Đã nhận gợi ý kích cỡ từ backend.')
    } catch (error) {
      setSmartSize(null)
      setNotice?.(`Không thể gợi ý kích cỡ: ${error.message}`)
    } finally {
      setSizeLoading(false)
    }
  }

  async function runStyleRecommend(event) {
    event?.preventDefault()
    setStyleLoading(true)
    try {
      const products = await chatbotService.recommendStyle({
        ...styleForm,
        style: quiz.style,
        color: quiz.color,
        fashionType: quiz.gender,
        pattern: styleForm.pattern,
      })
      const filtered = (Array.isArray(products) ? products : [])
        .filter((product) => priceOf(product) <= Number(quiz.budget || Infinity))
        .filter((product) => !quiz.dislikedColor || !textOf(product).includes(quiz.dislikedColor.toLowerCase()))
      setStyleProducts(filtered)
      setOutfits(buildOutfits(filtered, quiz))
      setNotice?.(`Đã tạo ${Math.min(3, buildOutfits(filtered, quiz).length)} bộ trang phục từ sản phẩm hiện có.`)
    } catch (error) {
      setStyleProducts([])
      setNotice?.(`Không thể gợi ý phong cách: ${error.message}`)
    } finally {
      setStyleLoading(false)
    }
  }

  function applyPreset(preset) {
    setStyleForm({
      color: preset.color,
      style: preset.style,
      category: preset.category,
      pattern: '',
      fashionType: preset.fashionType,
    })
  }

  function viewProduct(product) {
    sessionStorage.setItem('fashion:lastProductDetail', JSON.stringify(normalizeProduct(product)))
    setRoute?.(`${ROUTES.PRODUCT_DETAIL}?id=${encodeURIComponent(product.id)}`)
  }

  async function addOutfit(outfit) {
    setAddingOutfit(outfit.id)
    try {
      for (const product of outfit.products) await addToCart(product, firstAvailableVariant(product), 1)
      setNotice?.(`Đã thêm bộ trang phục ${outfit.number} vào giỏ hàng.`)
    } catch (error) {
      setNotice?.(`Chưa thể thêm toàn bộ bộ trang phục: ${error.message}`)
    } finally {
      setAddingOutfit(null)
    }
  }

  return (
    <section className="recommendation-page">
      <div className="recommendation-hero">
        <div>
          <p className="eyebrow">Gợi ý phong cách</p>
          <h1>Phối đồ theo vóc dáng và nhu cầu</h1>
          <p>Nhập số đo, chọn vibe bạn muốn mặc và để hệ thống gợi ý size cùng sản phẩm phù hợp trong catalog.</p>
        </div>
        <div className="style-preset-row">
          {stylePresets.map((preset) => (
            <button type="button" key={preset.label} onClick={() => applyPreset(preset)}>
              {preset.label}
            </button>
          ))}
        </div>
      </div>

      <div className="recommendation-layout">
        <div className="recommendation-toolbox">
          <form className="style-panel" onSubmit={runSmartSize}>
            <div className="style-panel-heading">
              <div>
                <p className="eyebrow">Gợi ý kích cỡ</p>
                <h2>Hồ sơ vóc dáng</h2>
              </div>
              <span>BMI {bodySummary.bmi}</span>
            </div>

            <div className="body-measure-grid">
              {bodyFields.map((field) => (
                <label key={field.key}>{field.label}
                  <div className="measure-input">
                    <input
                      type="number"
                      min={field.min}
                      max={field.max}
                      value={sizeForm[field.key]}
                      onChange={(event) => setSizeForm({ ...sizeForm, [field.key]: Number(event.target.value) })}
                    />
                    <span>{field.unit}</span>
                  </div>
                </label>
              ))}
            </div>

            <div className="body-insight-card">
              <div><span>Dáng người</span><strong>{bodySummary.bodyShape}</strong></div>
              <div><span>Gợi ý form</span><strong>{bodySummary.fitHint}</strong></div>
            </div>

            <button type="submit" disabled={sizeLoading}>
              {sizeLoading ? 'Đang phân tích...' : 'Gợi ý kích cỡ'}
            </button>
          </form>

          <form className="style-panel style-quiz-panel" onSubmit={runStyleRecommend}>
            <div className="style-panel-heading">
              <div>
                <p className="eyebrow">Chọn phong cách</p>
                <h2>Bạn muốn mặc gì hôm nay?</h2>
              </div>
            </div>

            <div className="style-form-grid">
              <label>Dịp mặc
                <select value={quiz.occasion} onChange={(event) => setQuiz({ ...quiz, occasion: event.target.value })}>{quizOptions.occasion.map((option) => <option key={option}>{option}</option>)}</select>
              </label>
              <label>Phong cách yêu thích
                <select value={quiz.style} onChange={(event) => setQuiz({ ...quiz, style: event.target.value })}>{quizOptions.style.map((option) => <option key={option} value={option}>{styleLabel(option)}</option>)}</select>
              </label>
              <label>Màu yêu thích
                <select value={quiz.color} onChange={(event) => setQuiz({ ...quiz, color: event.target.value })}>{quizOptions.color.map((option) => <option key={option}>{option}</option>)}</select>
              </label>
              <label>Giới tính
                <select value={quiz.gender} onChange={(event) => setQuiz({ ...quiz, gender: event.target.value })}>
                  <option value="unisex">Mọi giới tính</option>
                  <option value="female">Nữ</option>
                  <option value="male">Nam</option>
                </select>
              </label>
              <label>Ngân sách cho bộ trang phục
                <input type="number" min="0" step="50000" value={quiz.budget} onChange={(event) => setQuiz({ ...quiz, budget: Number(event.target.value) })} />
              </label>
              <label>Màu không thích
                <input value={quiz.dislikedColor} onChange={(event) => setQuiz({ ...quiz, dislikedColor: event.target.value })} placeholder="Ví dụ: đỏ" />
              </label>
            </div>

            <button type="submit" disabled={styleLoading}>
              {styleLoading ? 'Đang phân tích...' : 'Gợi ý phong cách'}
            </button>
          </form>

        </div>

        <aside className="style-result-panel">
          <div className="size-result-card">
            <p className="eyebrow">Kết quả size</p>
            {smartSize ? (
              <>
                <strong>{smartSize.recommendedSize || '-'}</strong>
                <span>Độ tin cậy {smartSize.confidence || '-'}</span>
                <p>{smartSize.reasoning || 'Backend đã trả về gợi ý kích cỡ cho sản phẩm phù hợp.'}</p>
                <small>Sản phẩm tham chiếu #{productId}</small>
              </>
            ) : (
              <p>Chưa có kết quả. Hãy nhập số đo và bấm gợi ý kích cỡ.</p>
            )}
          </div>

          <div className="style-products-panel">
            <div className="style-panel-heading">
              <div>
                <p className="eyebrow">Hồ sơ phong cách</p>
                <h2>{outfits.length ? `${styleLabel(quiz.style)} ${quiz.occasion.toLowerCase()}` : 'Chưa có hồ sơ phong cách'}</h2>
              </div>
            </div>

            {styleLoading && <p className="style-empty-state">Đang tìm sản phẩm phù hợp...</p>}
            {!styleLoading && !styleProducts.length && <p className="style-empty-state">Chọn bộ lọc phong cách rồi bấm tìm sản phẩm phù hợp.</p>}

            {outfits.length > 0 && <p className="style-profile-copy">Bạn ưu tiên phong cách {styleLabel(quiz.style).toLowerCase()}, màu {quiz.color.toLowerCase()} và ngân sách dưới {formatCurrency(quiz.budget)}. Hệ thống chọn sản phẩm đang kinh doanh để phối thành bộ trang phục.</p>}
            <div className="outfit-grid">
              {!styleLoading && outfits.map((outfit) => <OutfitCard key={outfit.id} outfit={outfit} onView={viewProduct} onAdd={addOutfit} adding={addingOutfit === outfit.id} />)}
            </div>
            {!styleLoading && !outfits.length && <p className="style-empty-state">Chọn các tiêu chí rồi bấm gợi ý để tạo bộ trang phục từ sản phẩm đang kinh doanh.</p>}
          </div>
        </aside>
      </div>
    </section>
  )
}

function buildLocalFashionAdvice(message, quiz) {
  const normalized = message.toLowerCase()
  if (normalized.includes('size') || normalized.includes('cỡ') || normalized.includes('kích')) {
    return 'Bạn hãy mở Hồ sơ vóc dáng, nhập chiều cao và cân nặng để nhận cỡ phù hợp cho từng sản phẩm. Với bộ trang phục hiện tại, hệ thống sẽ hiển thị cỡ khả dụng ngay dưới mỗi món.'
  }
  if (normalized.includes('giá') || normalized.includes('ngân sách') || normalized.includes('budget')) {
    return `Bạn có thể đặt ngân sách ${formatCurrency(quiz.budget)} trong phần Chọn phong cách. Hệ thống sẽ ưu tiên trang phục phong cách ${styleLabel(quiz.style).toLowerCase()} cho dịp ${quiz.occasion.toLowerCase()} và lọc theo mức giá đó.`
  }
  if (normalized.includes('đen') || normalized.includes('black')) {
    return 'Bạn có thể chọn màu Đen trong phần Chọn phong cách, sau đó bấm Gợi ý phong cách để hệ thống phối các sản phẩm màu tối phù hợp.'
  }
  return `Với nhu cầu hiện tại, mình gợi ý phong cách ${styleLabel(quiz.style)} cho dịp ${quiz.occasion.toLowerCase()}, ưu tiên màu ${quiz.color.toLowerCase()} và ngân sách ${formatCurrency(quiz.budget)}. Hãy bấm Gợi ý phong cách để xem các bộ trang phục từ danh mục sản phẩm.`
}

function StyleProductCard({ product, onView }) {
  const normalized = normalizeProduct(product)
  const hasSale = normalized.salePrice && normalized.basePrice && normalized.salePrice < normalized.basePrice

  return (
    <article className="style-product-card">
      <div className="style-product-image">
        <SafeImage src={normalized.imageUrl} alt={normalized.name} fallbackLabel="Sản phẩm thời trang" />
      </div>
      <div>
        <p className="eyebrow">{normalized.brandName || 'Thương hiệu'}</p>
        <h2>{normalized.name}</h2>
        <p>{normalized.material || normalized.categoryName || 'Sản phẩm thời trang'}</p>
      </div>
      <div className="style-product-price">
        <strong>{formatCurrency(normalized.salePrice || normalized.basePrice)}</strong>
        {hasSale && <del>{formatCurrency(normalized.basePrice)}</del>}
      </div>
      <button type="button" className="ghost" onClick={() => onView(product)}>
        Xem chi tiết
      </button>
    </article>
  )
}

function OutfitCard({ outfit, onView, onAdd, adding }) {
  return (
    <article className="outfit-card">
      <div className="outfit-card-heading"><div><p className="eyebrow">BỘ {outfit.number.slice(-2)}</p><h3>{outfit.title}</h3></div><strong>{outfit.score}% phù hợp</strong></div>
      <div className="outfit-product-list">
        {outfit.products.map((product) => {
          const normalized = normalizeProduct(product)
          return <div className="outfit-product" key={product.id}>
            <button type="button" className="outfit-product-image" onClick={() => onView(product)}>
              <SafeImage src={normalized.imageUrl} alt={normalized.name} fallbackLabel="Sản phẩm thời trang" />
            </button>
            <div><strong>{normalized.name}</strong><small>{sizeHint(product)} · {formatCurrency(priceOf(product))}</small></div>
          </div>
        })}
      </div>
      <div className="outfit-total"><span>Tổng giá bộ trang phục</span><strong>{formatCurrency(outfit.total)}</strong></div>
      <p className="outfit-reason">Vì sao phù hợp? {outfit.reason}</p>
      <div className="outfit-actions"><button type="button" className="ghost" onClick={() => onView(outfit.products[0])}>Xem sản phẩm</button><button type="button" onClick={() => onAdd(outfit)} disabled={adding}>{adding ? 'Đang thêm...' : 'Thêm cả bộ vào giỏ'}</button></div>
    </article>
  )
}

function buildOutfits(products, quiz) {
  const normalized = products.filter((product) => product?.id).slice(0, 12)
  if (!normalized.length) return []
  const groups = [normalized.slice(0, 3), normalized.slice(3, 6), normalized.slice(6, 9)].filter((group) => group.length)
  return groups.map((group, index) => ({
    id: `${quiz.style}-${index}`,
    number: `OUTFIT 0${index + 1}`,
    title: `${styleLabel(quiz.style)} ${quiz.occasion}`,
    products: group,
    total: group.reduce((sum, product) => sum + priceOf(product), 0),
    score: Math.max(78, 92 - index * 4),
    reason: `Các sản phẩm có màu ${quiz.color.toLowerCase()} và kiểu dáng ${styleLabel(quiz.style).toLowerCase()}, phù hợp cho dịp ${quiz.occasion.toLowerCase()}.`,
  }))
}

function priceOf(product) {
  return Number(product.salePrice || product.basePrice || product.price || 0)
}

function textOf(product) {
  return [product.name, product.description, product.material, product.color, product.categoryName, product.category?.name].filter(Boolean).join(' ').toLowerCase()
}

function firstAvailableVariant(product) {
  return (product.variants || []).find((variant) => variant?.id && (variant.stockQty == null || Number(variant.stockQty) > 0)) || product.variants?.[0] || null
}

function sizeHint(product) {
  const variant = firstAvailableVariant(product)
  return variant?.size ? `Cỡ đề xuất: ${variant.size}` : 'Cỡ theo sản phẩm'
}

function styleLabel(style) {
  return ({ Basic: 'Cơ bản', basic: 'Cơ bản', Streetwear: 'Đường phố', streetwear: 'Đường phố', Minimal: 'Tối giản', minimal: 'Tối giản', office: 'Công sở', Vintage: 'Cổ điển', vintage: 'Cổ điển', Elegant: 'Thanh lịch', elegant: 'Thanh lịch', Sporty: 'Thể thao', sporty: 'Thể thao', Y2K: 'Phong cách Y2K', y2k: 'Phong cách Y2K' })[style] || style
}

function normalizeProduct(product) {
  return {
    ...product,
    brandName: product.brandName || product.brand?.name || '',
    categoryName: product.categoryName || product.category?.name || '',
    imageUrl: product.primaryImageUrl || product.imageUrls?.[0] || product.images?.[0]?.imageUrl || '',
  }
}
