import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelOrderItem, fetchOrder, requestReturn, submitReview } from '../api/orders'

const OrderDetails = () => {
  const { orderId } = useParams()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyItemId, setBusyItemId] = useState(null)
  const [reasons, setReasons] = useState({})
  const [reviews, setReviews] = useState({})
  const [message, setMessage] = useState('')

  const reload = useCallback(async () => {
    const data = await fetchOrder(orderId)
    setOrder(data)
    setLoading(false)
  }, [orderId])

  useEffect(() => {
    let active = true
    fetchOrder(orderId).then((data) => {
      if (active) { setOrder(data); setLoading(false) }
    }).catch((requestError) => {
      if (active) {
        setError(requestError.response?.data?.detail || 'This order could not be loaded.')
        setLoading(false)
      }
    })
    return () => { active = false }
  }, [orderId])

  const performItemAction = async (item, action) => {
    const reason = reasons[item.id]?.trim()
    if (!reason) { setMessage('Enter a reason before submitting this request.'); return }
    setBusyItemId(item.id)
    setMessage('')
    try {
      if (action === 'cancel') await cancelOrderItem(orderId, item.id, reason)
      else await requestReturn(orderId, item.id, reason)
      await reload()
      setMessage(action === 'cancel' ? 'Cancellation request submitted.' : 'Return request submitted.')
    } catch (requestError) {
      setMessage(requestError.response?.data?.detail || 'The request could not be submitted.')
    } finally {
      setBusyItemId(null)
    }
  }

  const sendReview = async (item) => {
    const review = reviews[item.id] || {}
    if (!review.body?.trim()) { setMessage('Write a short review before submitting.'); return }
    setBusyItemId(item.id)
    setMessage('')
    try {
      await submitReview(item.productId, { orderItemId: item.id, rating: Number(review.rating || 5), title: (review.title || '').trim(), body: review.body.trim() })
      await reload()
      setMessage('Your review was submitted for moderation.')
    } catch (requestError) {
      setMessage(requestError.response?.data?.detail || 'The review could not be submitted.')
    } finally {
      setBusyItemId(null)
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Loading order…</p></main>
  if (error || !order) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error || 'Order not found.'}</div><Link to="/orders">Back to orders</Link></main>

  return (
    <main className="container py-4" style={{ maxWidth: 1000 }}>
      <p><Link to="/orders">← Your orders</Link></p>
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-4"><div><h1 className="h2 mb-1">Order {order.orderNumber}</h1><p className="text-secondary mb-0">Placed {new Date(order.createdAt).toLocaleString()}</p></div><span className="badge text-bg-secondary fs-6 align-self-start">{order.status}</span></div>
      {message && <p className="alert alert-info" role="status">{message}</p>}
      <section className="card card-body mb-4">
        <h2 className="h5">Delivery address</h2>
        {order.shippingAddress ? <address className="mb-0">{order.shippingAddress.recipientName}<br />{order.shippingAddress.line1}{order.shippingAddress.line2 ? `, ${order.shippingAddress.line2}` : ''}<br />{order.shippingAddress.city}, {order.shippingAddress.region} {order.shippingAddress.postalCode}<br />{order.shippingAddress.countryCode}</address> : <p className="mb-0">Address details unavailable.</p>}
        <p className="mt-3 mb-0">Payment: <strong>{order.paymentStatus || 'PENDING'}</strong>
          {order.paymentId && <span> · <Link to={`/payments/${order.paymentId}`}>View payment</Link></span>}
        </p>
      </section>
      {(order.sellerOrders || []).map((sellerOrder) => <section className="card mb-4" key={sellerOrder.id}>
        <div className="card-header d-flex flex-wrap justify-content-between gap-2"><strong>{sellerOrder.storeName || sellerOrder.sellerName || 'Seller order'}</strong><span>{sellerOrder.status}</span></div>
        <div className="card-body">
          {(sellerOrder.shipments || []).map((shipment) => <div className="alert alert-light border" key={shipment.id}>
            <strong>Shipment: {shipment.status}</strong>{shipment.carrier && <span> · {shipment.carrier}</span>}{shipment.trackingNumber && <span> · Tracking {shipment.trackingNumber}</span>}
          </div>)}
          {(sellerOrder.items || []).map((item) => <article className="border rounded p-3 mb-3" key={item.id}>
            <div className="d-flex flex-wrap justify-content-between gap-2"><Link to={`/products/${item.productId}`}>{item.name}</Link><span>{item.quantity} × {new Intl.NumberFormat(undefined, { style: 'currency', currency: order.currency || 'INR' }).format(item.unitPrice)}</span></div>
            <p className="small text-secondary">Item status: {item.status}</p>
            {(item.canCancel || item.canReturn) && <div className="row g-2 align-items-end">
              <div className="col-12 col-md-7"><label className="form-label" htmlFor={`reason-${item.id}`}>{item.canCancel ? 'Cancellation reason' : 'Return reason'}</label><input id={`reason-${item.id}`} className="form-control" maxLength={500} value={reasons[item.id] || ''} onChange={(event) => setReasons({ ...reasons, [item.id]: event.target.value })} /></div>
              <div className="col d-flex gap-2">
                {item.canCancel && <button className="btn btn-outline-danger" type="button" disabled={busyItemId === item.id} onClick={() => performItemAction(item, 'cancel')}>Request cancellation</button>}
                {item.canReturn && <button className="btn btn-outline-primary" type="button" disabled={busyItemId === item.id} onClick={() => performItemAction(item, 'return')}>Request return</button>}
              </div>
            </div>}
            {item.canReview && <form className="border-top mt-3 pt-3" onSubmit={(event) => { event.preventDefault(); sendReview(item) }}>
              <h3 className="h6">Review this product</h3>
              <div className="row g-2"><div className="col-4 col-md-2"><label className="form-label" htmlFor={`rating-${item.id}`}>Rating</label><select id={`rating-${item.id}`} className="form-select" value={reviews[item.id]?.rating || 5} onChange={(event) => setReviews({ ...reviews, [item.id]: { ...reviews[item.id], rating: event.target.value } })}>{[5, 4, 3, 2, 1].map((value) => <option key={value} value={value}>{value} / 5</option>)}</select></div>
                <div className="col-12"><label className="form-label" htmlFor={`review-title-${item.id}`}>Review title</label><input id={`review-title-${item.id}`} className="form-control" maxLength={120} required value={reviews[item.id]?.title || ''} onChange={(event) => setReviews({ ...reviews, [item.id]: { ...reviews[item.id], title: event.target.value } })} /></div>
                <div className="col-12"><label className="form-label" htmlFor={`review-${item.id}`}>Your review</label><textarea id={`review-${item.id}`} className="form-control" rows="3" maxLength={3000} required value={reviews[item.id]?.body || ''} onChange={(event) => setReviews({ ...reviews, [item.id]: { ...reviews[item.id], body: event.target.value } })} /></div>
                <div><button className="btn btn-outline-primary" type="submit" disabled={busyItemId === item.id}>Submit review</button></div>
              </div>
            </form>}
          </article>)}
        </div>
      </section>)}
      <section className="card card-body text-end"><span>Order total</span><strong className="fs-4">{new Intl.NumberFormat(undefined, { style: 'currency', currency: order.currency || 'INR' }).format(order.total || 0)}</strong></section>
    </main>
  )
}

export default OrderDetails
