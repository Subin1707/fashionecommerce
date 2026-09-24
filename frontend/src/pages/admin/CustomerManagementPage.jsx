import { useEffect, useState } from 'react'
import { DataTable } from '../../components/common/DataTable'
import * as adminService from '../../services/adminService'

export function CustomerManagementPage({ setNotice }) {
  const [customers, setCustomers] = useState([])

  useEffect(() => {
    adminService.getCustomers()
      .then(setCustomers)
      .catch((error) => setNotice(`Chưa đọc được khách hàng: ${error.message}`))
  }, [setNotice])

  async function setCustomerStatus(customer, action) {
    try {
      const updated = action === 'lock'
        ? await adminService.lockCustomer(customer.id)
        : await adminService.unlockCustomer(customer.id)
      setCustomers(customers.map((item) => (item.id === customer.id ? updated : item)))
      setNotice(`Đã ${action === 'lock' ? 'khóa' : 'mở khóa'} khách hàng #${customer.id}.`)
    } catch (error) {
      setNotice(`Không cập nhật được khách hàng: ${error.message}`)
    }
  }

  return (
    <section className="table-panel">
      <h2>Khách hàng</h2>
      <DataTable
        rows={customers}
        columns={['id', 'fullName', 'email', 'phone', 'status']}
        empty="Chưa có khách hàng."
        action={(row) => (
          <button type="button" onClick={() => setCustomerStatus(row, row.status === 'LOCKED' ? 'unlock' : 'lock')}>
            {row.status === 'LOCKED' ? 'Unlock' : 'Lock'}
          </button>
        )}
      />
    </section>
  )
}
