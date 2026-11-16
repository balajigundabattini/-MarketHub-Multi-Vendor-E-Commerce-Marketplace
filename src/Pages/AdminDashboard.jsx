import { useEffect, useState } from 'react'
import { fetchAdminDashboard } from '../api/admin'

const AdminDashboard = () => {
  const [data, setData] = useState(null); const [error, setError] = useState('')
  useEffect(() => { let active = true; fetchAdminDashboard().then((result) => { if (active) setData(result) }).catch((e) => { if (active) setError(e.response?.data?.detail || 'Dashboard metrics could not be loaded.') }); return () => { active = false } }, [])
  return <main><h1 className="h2">Platform overview</h1><p className="text-secondary">Current marketplace activity and queues.</p>
    {error && <p className="alert alert-warning" role="alert">{error}</p>}{!data && !error && <p role="status">Loading dashboard…</p>}
    {data && <div className="row g-3">{[['Gross sales', data.grossSales, data.currency], ['Orders', data.orderCount], ['Customers', data.customerCount], ['Active sellers', data.activeSellerCount], ['Pending sellers', data.pendingSellerApplications], ['Products awaiting review', data.productsAwaitingModeration]].map(([label, value, currency]) => <div className="col-12 col-sm-6 col-xl-4" key={label}><section className="card card-body h-100"><p className="text-secondary mb-2">{label}</p><strong className="fs-3">{value == null ? '—' : currency ? new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(value) : value}</strong></section></div>)}</div>}
  </main>
}
export default AdminDashboard
