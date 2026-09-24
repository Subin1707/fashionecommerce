import { useEffect, useState } from 'react'
import { DataTable } from '../../components/common/DataTable'
import * as userService from '../../services/userService'

export function UserManagementPage({ setNotice }) {
  const [users, setUsers] = useState([])

  useEffect(() => {
    userService.getUsers()
      .then(setUsers)
      .catch((error) => setNotice(`Chưa đọc được tài khoản: ${error.message}`))
  }, [setNotice])

  return (
    <section className="table-panel">
      <div className="admin-section-heading">
        <div>
          <p className="eyebrow">Quản trị người dùng</p>
          <h2>Tài khoản và phân quyền</h2>
        </div>
        <span className="session-pill">Hiển thị {users.length}/10 tài khoản</span>
      </div>
      <DataTable rows={users} columns={['id', 'fullName', 'email', 'role', 'status']} empty="Chưa có tài khoản." />
    </section>
  )
}
