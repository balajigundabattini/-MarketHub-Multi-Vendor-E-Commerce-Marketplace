import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchSellerDashboard } from '../api/seller'

const SellerDashboard = () => {
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchSellerDashboard().then((data) => { if (active) setSummary(data) })
      .catch((requestError) => { if (active) setError(requestError.response?.data?.detail || 'Seller summary is unavailable.') })
    return () => { active = false }
  }, [])

  return (
    <main>
      <h1 className="h2 mb-4">Seller overview</h1>
      {error && <p className="alert alert-warning" role="alert">{error}</p>}
      {!summary && !error && <p role="status">Loading seller overview…</p>}
      {summary && <div className="row g-3">
        {[["Sales this month", summary.monthlySales], ["Open seller orders", summary.openOrders], ["Active products", summary.activeProducts], ["Low stock products", summary.lowStockProducts]].map(([label, value]) => <div key={label} className="col-12 col-sm-6 col-xl-3"><section className="card card-body h-100"><p className="text-secondary mb-1">{label}</p><strong className="fs-3">{value ?? '—'}</strong></section></div>)}
      </div>}
      <div className="d-flex flex-wrap gap-2 mt-4"><Link className="btn btn-primary" to="products/new">Add product</Link><Link className="btn btn-outline-primary" to="orders">Review orders</Link><Link className="btn btn-outline-secondary" to="reports">View reports</Link></div>
    </main>
  )
}

export default SellerDashboard
