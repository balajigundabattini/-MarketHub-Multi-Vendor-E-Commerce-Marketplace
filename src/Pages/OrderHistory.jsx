import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchOrders } from '../api/orders'

const OrderHistory = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryKey = searchParams.toString()
  const [orders, setOrders] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0 })
  const [loadedKey, setLoadedKey] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    const params = Object.fromEntries(new URLSearchParams(queryKey))
    fetchOrders({ page: Number(params.page || 0), size: 10 })
      .then((data) => {
        if (active) {
          setOrders(data.content || [])
          setPageInfo({ page: data.number || 0, totalPages: data.totalPages || 0, totalElements: data.totalElements || 0 })
          setError('')
          setLoadedKey(queryKey)
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.detail || 'Your orders could not be loaded.')
          setLoadedKey(queryKey)
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [queryKey])

  const isLoading = loading || loadedKey !== queryKey
  const changePage = (page) => {
    setLoading(true)
    const next = new URLSearchParams(searchParams)
    next.set('page', String(page))
    setSearchParams(next)
  }

  return (
    <main className="container py-4" style={{ maxWidth: 960 }}>
      <h1 className="h2 mb-4">Your orders</h1>
      {isLoading && <p role="status" className="alert alert-info">Loading orders…</p>}
      {!isLoading && error && <p className="alert alert-warning" role="alert">{error}</p>}
      {!isLoading && !error && orders.length === 0 && <div className="card card-body text-center py-5"><h2 className="h5">No orders yet</h2><p className="text-secondary">Your completed checkouts will appear here.</p><Link className="btn btn-primary align-self-center" to="/products">Shop products</Link></div>}
      {!isLoading && !error && orders.length > 0 && <>
        <p className="text-secondary">{pageInfo.totalElements} order{pageInfo.totalElements === 1 ? '' : 's'}</p>
        <div className="d-grid gap-3">
          {orders.map((order) => <article className="card" key={order.id}>
            <div className="card-header d-flex flex-wrap justify-content-between gap-2">
              <span><strong>Order {order.orderNumber}</strong><br /><small>{new Date(order.createdAt).toLocaleDateString()}</small></span>
              <span className="badge text-bg-secondary align-self-center">{order.status}</span>
            </div>
            <div className="card-body d-flex flex-wrap justify-content-between align-items-center gap-3">
              <div><span>{order.sellerOrderCount ?? order.sellerOrders?.length ?? 0} seller shipment group(s)</span><br /><strong>{new Intl.NumberFormat(undefined, { style: 'currency', currency: order.currency || 'INR' }).format(order.total || 0)}</strong></div>
              <Link className="btn btn-outline-primary" to={`/orders/${order.id}`}>View order</Link>
            </div>
          </article>)}
        </div>
        {pageInfo.totalPages > 1 && <nav className="d-flex justify-content-center align-items-center gap-3 mt-4" aria-label="Order pages">
          <button className="btn btn-outline-secondary" type="button" disabled={pageInfo.page <= 0} onClick={() => changePage(pageInfo.page - 1)}>Previous</button>
          <span>Page {pageInfo.page + 1} of {pageInfo.totalPages}</span>
          <button className="btn btn-outline-secondary" type="button" disabled={pageInfo.page + 1 >= pageInfo.totalPages} onClick={() => changePage(pageInfo.page + 1)}>Next</button>
        </nav>}
      </>}
    </main>
  )
}

export default OrderHistory
