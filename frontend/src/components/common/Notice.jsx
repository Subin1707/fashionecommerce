export function Notice({ children }) {
  if (!children) return null
  return <p className="notice">{children}</p>
}
