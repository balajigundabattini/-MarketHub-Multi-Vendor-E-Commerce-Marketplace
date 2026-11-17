import { useCallback, useEffect, useState } from 'react'
import { createShipment, decideSellerReturn, fetchSellerOrders, updateSellerOrderStatus } from '../api/seller'

const SellerOrders = () => {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [shipmentForms, setShipmentForms] = useState({})
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const data = await fetchSellerOrders({ page: 0, size: 30 })
      setOrders(data.content || [])
      setError('')
    } catch (requestError) { setError(requestError.response?.data?.detail || 'Seller orders could not be loaded.') }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { Promise.resolve().then(load) }, [load])

  const setStatus = async (order, status) => {
    setBusyId(order.id)
    setError('')
    try { await updateSellerOrderStatus(order.id, status); await load(); setMessage(`Order ${order.orderNumber} updated.`) }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Order status could not be updated.') }
    finally { setBusyId(null) }
  }

  const ship = async (order) => {
    const details = shipmentForms[order.id] || {}
    if (!details.carrier?.trim() || !details.trackingNumber?.trim()) { setError('Enter both carrier and tracking number.'); return }
    setBusyId(order.id)
    setError('')
    try { await createShipment(order.id, details); await load(); setMessage(`Shipment recorded for order ${order.orderNumber}.`) }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Shipment could not be recorded.') }
    finally { setBusyId(null) }
  }

  const decideReturn = async (order, request, decision) => {
    setBusyId(order.id)
    setError('')
    try { await decideSellerReturn(order.id, request.id, decision); await load(); setMessage(`Return request ${decision.toLowerCase()}.`) }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Return request could not be updated.') }
    finally { setBusyId(null) }
  }

  return <main><h1 className="h2 mb-4">Seller orders</h1>{message && <p className="alert alert-info" role="status">{message}</p>}{error && <p className="alert alert-warning" role="alert">{error}</p>}
    {loading ? <p role="status">Loading seller orders…</p> : orders.length === 0 ? <div className="card card-body">No seller orders have arrived yet.</div> : <div className="d-grid gap-3">
      {orders.map((order) => <article className="card" key={order.id}>
        <div className="card-header d-flex justify-content-between gap-2"><strong>Order {order.orderNumber}</strong><span>{order.status}</span></div>
        <div className="card-body"><p className="text-secondary">Placed {new Date(order.createdAt).toLocaleString()} · {order.items?.length || 0} item(s)</p>
          <ul>{(order.items || []).map((item) => <li key={item.id}>{item.name} × {item.quantity}</li>)}</ul>
          {(order.shipments || []).map((shipment) => <p className="small mb-2" key={shipment.id}>Shipment {shipment.status} · {shipment.carrier} · {shipment.trackingNumber}</p>)}
          <div className="d-flex flex-wrap gap-2">
            {order.canAccept && <button className="btn btn-sm btn-primary" type="button" disabled={busyId === order.id} onClick={() => setStatus(order, 'ACCEPTED')}>Accept order</button>}
            {order.canReject && <button className="btn btn-sm btn-outline-danger" type="button" disabled={busyId === order.id} onClick={() => setStatus(order, 'REJECTED')}>Reject order</button>}
            {order.canStartProcessing && <button className="btn btn-sm btn-outline-primary" type="button" disabled={busyId === order.id} onClick={() => setStatus(order, 'PROCESSING')}>Start processing</button>}
            {order.canApproveCancellation && <button className="btn btn-sm btn-outline-danger" type="button" disabled={busyId === order.id} onClick={() => setStatus(order, 'CANCELLED')}>Approve cancellation</button>}
            {order.canMarkDelivered && <button className="btn btn-sm btn-success" type="button" disabled={busyId === order.id} onClick={() => setStatus(order, 'DELIVERED')}>Mark delivered</button>}
          </div>
          {(order.returnRequests || []).map((request) => <section className="border rounded p-3 mt-3" key={request.id} aria-label="Return request">
            <div className="d-flex flex-wrap justify-content-between gap-2"><div><strong>Return requested: {request.itemName} × {request.quantity}</strong><p className="mb-1">{request.reason}</p><small className="text-secondary">{new Date(request.createdAt).toLocaleString()}</small></div>
              <div className="d-flex align-items-start gap-2"><button className="btn btn-sm btn-primary" type="button" disabled={busyId === order.id} onClick={() => decideReturn(order, request, 'APPROVED')}>Approve</button><button className="btn btn-sm btn-outline-danger" type="button" disabled={busyId === order.id} onClick={() => decideReturn(order, request, 'REJECTED')}>Reject</button></div></div>
            <p className="small text-secondary mt-2 mb-0">Approval authorizes the return; refund and inventory restock are handled after receipt through the applicable refund process.</p>
          </section>)}
          {order.canShip && <div className="row g-2 mt-2 align-items-end">
            <div className="col-sm-4"><label className="form-label" htmlFor={`carrier-${order.id}`}>Carrier</label><input id={`carrier-${order.id}`} className="form-control" maxLength={100} value={shipmentForms[order.id]?.carrier || ''} onChange={(event) => setShipmentForms({ ...shipmentForms, [order.id]: { ...shipmentForms[order.id], carrier: event.target.value } })} /></div>
            <div className="col-sm-5"><label className="form-label" htmlFor={`tracking-${order.id}`}>Tracking number</label><input id={`tracking-${order.id}`} className="form-control" maxLength={160} value={shipmentForms[order.id]?.trackingNumber || ''} onChange={(event) => setShipmentForms({ ...shipmentForms, [order.id]: { ...shipmentForms[order.id], trackingNumber: event.target.value } })} /></div>
            <div className="col-sm-3"><button className="btn btn-outline-primary w-100" type="button" disabled={busyId === order.id} onClick={() => ship(order)}>Record shipment</button></div>
          </div>}
        </div>
      </article>)}
    </div>}
  </main>
}

export default SellerOrders
