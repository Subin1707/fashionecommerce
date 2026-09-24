import { ADMIN_NAV } from '../utils/constants'

export function AdminLayout({ route, setRoute, children }) {
  return (
    <section className="admin-space">
      <div className="sub-nav">
        {ADMIN_NAV.map(([label, value]) => (
          <button key={value} className={route === value ? 'active' : ''} onClick={() => setRoute(value)}>
            {label}
          </button>
        ))}
      </div>
      {children}
    </section>
  )
}
