import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchNotifications, markAllNotificationsRead, markNotificationRead } from '../api/notifications'

const Notifications = () => {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const data = await fetchNotifications({ page: 0, size: 30 })
      setItems(data.content || [])
      setError('')
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'Notifications could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { Promise.resolve().then(load) }, [load])

  const markRead = async (id) => {
    try {
      await markNotificationRead(id)
      setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item))
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'Notification could not be marked as read.')
    }
  }

  const markAllRead = async () => {
    try {
      await markAllNotificationsRead()
      setItems((current) => current.map((item) => ({ ...item, read: true })))
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'Notifications could not be marked as read.')
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Loading notifications…</p></main>
  return (
    <main className="container py-4" style={{ maxWidth: 900 }}>
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4 "><h1 className="h2 mb-0">Notifications</h1><button type="button" className="btn btn-outline-secondary" disabled={!items.some((item) => !item.read)} onClick={markAllRead}>Mark all as read</button></div>
      {error && <div className="alert alert-warning" role="alert">{error}</div>}
      {items.length === 0 ? <div className="card card-body text-center py-5"><h2 className="h5">You’re all caught up</h2><p className="mb-0 text-secondary">Order and account updates will appear here.</p></div> :
        <ul className="list-group">{items.map((item) => <li className={`list-group-item ${item.read ? '' : 'list-group-item-light border-start border-4 border-primary mb-2'}`} key={item.id}>
            <div className="d-flex flex-wrap justify-content-between align-items-start gap-3"><div><h2 className="h6 mb-1">{item.title}</h2><p className="mb-1">{item.message}</p><small className="text-secondary">{new Date(item.createdAt).toLocaleString()}</small>
              {item.orderId && <p className="mt-2 mb-0"><Link to={`/orders/${item.orderId}`}>View order</Link></p>}
              {item.paymentId && <p className="mt-2 mb-0"><Link to={`/payments/${item.paymentId}`}>View payment</Link></p>}
            </div>
            {!item.read && <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => markRead(item.id)}>Mark read</button>}</div>
        </li>)}</ul>}
    </main>
  )
}

export default Notifications
