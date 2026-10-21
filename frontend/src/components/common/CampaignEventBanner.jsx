import { useEffect, useState } from 'react'
import * as campaignEventService from '../../services/campaignEventService'
import { campaignImageSource } from '../../services/campaignEventService'
import { ROUTES } from '../../utils/constants'
import './CampaignEventBanner.css'

export function CampaignEventBanner({ setRoute }) {
  const [events, setEvents] = useState([])
  const [activeIndex, setActiveIndex] = useState(0)
  const [imageFailed, setImageFailed] = useState(false)

  useEffect(() => {
    let current = true
    const loadEvents = () => campaignEventService.getActiveCampaignEvents()
      .then((rows) => {
        if (current) setEvents(Array.isArray(rows) ? rows : [])
      })
      .catch(() => {
        if (current) setEvents([])
      })
    loadEvents()
    const refreshTimer = window.setInterval(loadEvents, 60_000)
    return () => {
      current = false
      window.clearInterval(refreshTimer)
    }
  }, [])

  useEffect(() => {
    setActiveIndex(0)
    setImageFailed(false)
  }, [events.length])

  useEffect(() => {
    if (events.length < 2 || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined
    const timer = window.setInterval(() => {
      setActiveIndex((index) => (index + 1) % events.length)
    }, 5000)
    return () => window.clearInterval(timer)
  }, [events.length])

  if (events.length === 0) return null

  const event = events[activeIndex]
  const move = (direction) => {
    setImageFailed(false)
    setActiveIndex((index) => (index + direction + events.length) % events.length)
  }

  function openCampaign() {
    const destination = event.targetUrl?.trim() || ROUTES.PRODUCTS
    if (/^https?:\/\//i.test(destination)) {
      window.location.assign(destination)
      return
    }
    setRoute(destination.replace(/^#\/?/, '').replace(/^\//, '') || ROUTES.PRODUCTS)
  }

  return (
    <section className="campaign-banner" aria-label="Sự kiện khuyến mãi">
      <button
        type="button"
        className="campaign-banner-slide"
        onClick={openCampaign}
        aria-label={`${event.eventName}: ${event.title}. Mở chương trình`}
      >
        <picture className="campaign-banner-picture">
          {event.mobileImageUrl && <source media="(max-width: 640px)" srcSet={campaignImageSource(event.mobileImageUrl)} />}
          <img
            src={campaignImageSource(event.desktopImageUrl)}
            alt=""
            onError={() => setImageFailed(true)}
          />
        </picture>
        <span className={`campaign-banner-shade${imageFailed ? ' image-failed' : ''}`} />
        <span className="campaign-banner-copy">
          <span className="campaign-banner-kicker">{event.eventName}</span>
          <span className="campaign-banner-title">{event.title}</span>
          {event.message && <span className="campaign-banner-message">{event.message}</span>}
          {event.offerLabel && <span className="campaign-banner-offer">{event.offerLabel}</span>}
          {event.voucherCode && <span className="campaign-banner-code">Mã {event.voucherCode}</span>}
          <span className="campaign-banner-cta">Khám phá bộ sưu tập <span aria-hidden="true">↗</span></span>
        </span>
        <span className="campaign-banner-date">
          {new Date(event.startAt).toLocaleDateString('vi-VN')} — {new Date(event.endAt).toLocaleDateString('vi-VN')}
        </span>
      </button>

      {events.length > 1 && (
        <>
          <button type="button" className="campaign-banner-arrow previous" onClick={() => move(-1)} aria-label="Sự kiện trước">‹</button>
          <button type="button" className="campaign-banner-arrow next" onClick={() => move(1)} aria-label="Sự kiện tiếp theo">›</button>
          <div className="campaign-banner-dots" aria-label="Chọn sự kiện">
            {events.map((item, index) => (
              <button
                type="button"
                key={item.id}
                className={index === activeIndex ? 'active' : ''}
                aria-label={`Hiển thị sự kiện ${index + 1}: ${item.eventName}`}
                aria-current={index === activeIndex ? 'true' : undefined}
                onClick={() => { setImageFailed(false); setActiveIndex(index) }}
              />
            ))}
          </div>
        </>
      )}
    </section>
  )
}
