import { useEffect, useState } from 'react'
import { DataTable } from './DataTable'

export function CrudPanel({ title, empty, columns, initialForm, load, create, update, remove, setNotice }) {
  const [rows, setRows] = useState([])
  const [form, setForm] = useState(initialForm)
  const [editingId, setEditingId] = useState(null)

  useEffect(() => {
    async function refresh() {
      try {
        setRows(await load())
      } catch (error) {
        setNotice(`Không đọc được ${title}: ${error.message}`)
      }
    }
    refresh()
  }, [load, setNotice, title])

  async function submit(event) {
    event.preventDefault()
    try {
      if (editingId) {
        const updated = await update(editingId, form)
        setRows(rows.map((row) => (row.id === editingId ? updated : row)))
        setNotice(`Đã cập nhật ${title} #${editingId}.`)
      } else {
        const created = await create(form)
        setRows([created, ...rows])
        setNotice(`Đã tạo ${title} #${created.id}.`)
      }
      setForm(initialForm)
      setEditingId(null)
    } catch (error) {
      setNotice(`Không lưu được ${title}: ${error.message}`)
    }
  }

  async function deleteRow(id) {
    try {
      await remove(id)
      setRows(rows.filter((row) => row.id !== id))
      setNotice(`Đã xóa ${title} #${id}.`)
    } catch (error) {
      setNotice(`Không xóa được ${title}: ${error.message}`)
    }
  }

  return (
    <section className="two-column">
      <form className="checkout-panel" onSubmit={submit}>
        <h2>{editingId ? `Sửa ${title}` : `Thêm ${title}`}</h2>
        {Object.entries(form).map(([field, value]) => (
          <label key={field}>{field}
            <input value={value ?? ''} onChange={(event) => setForm({ ...form, [field]: event.target.value })} />
          </label>
        ))}
        <button type="submit">{editingId ? 'Cập nhật' : 'Tạo mới'}</button>
        {editingId && <button type="button" className="ghost" onClick={() => { setEditingId(null); setForm(initialForm) }}>Hủy</button>}
      </form>

      <div className="table-panel">
        <h2>{title}</h2>
        <DataTable
          rows={rows}
          columns={columns}
          empty={empty}
          action={(row) => (
            <div className="row-actions">
              <button type="button" onClick={() => { setEditingId(row.id); setForm(pickFields(row, initialForm)) }}>Sửa</button>
              <button type="button" className="danger" onClick={() => deleteRow(row.id)}>Xóa</button>
            </div>
          )}
        />
      </div>
    </section>
  )
}

function pickFields(row, template) {
  return Object.keys(template).reduce((picked, key) => ({ ...picked, [key]: row[key] ?? '' }), {})
}
