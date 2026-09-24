import { formatCurrency } from '../../utils/formatCurrency'

export function DataTable({ rows, columns, empty, action }) {
  if (!rows?.length) return <p className="empty-state">{empty}</p>

  return (
    <div className="data-table">
      <table>
        <thead>
          <tr>
            {columns.map((column) => <th key={column}>{column}</th>)}
            {action && <th>Thao tác</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              {columns.map((column) => (
                <td key={column}>{column.toLowerCase().includes('amount') ? formatCurrency(row[column]) : String(row[column] ?? '-')}</td>
              ))}
              {action && <td>{action(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
