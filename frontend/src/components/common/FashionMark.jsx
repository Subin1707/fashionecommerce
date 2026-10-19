export function FashionMark({ className }) {
  return (
    <span className={`fashion-logo-mark ${className}`} aria-hidden="true">
      <svg viewBox="0 0 40 40" focusable="false">
        <path d="M12 30V10h17v4H17v4h10v4H17v8h-5Z" fill="currentColor" />
      </svg>
    </span>
  )
}
