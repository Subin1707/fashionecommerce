import { ORDER_STATUSES } from '../../utils/constants'

export function OrderStatusSelect({ value, onChange }) {
  return (
    <select value={value || 'PENDING'} onChange={(event) => onChange(event.target.value)}>
      {ORDER_STATUSES.map((status) => <option key={status}>{status}</option>)}
    </select>
  )
}
