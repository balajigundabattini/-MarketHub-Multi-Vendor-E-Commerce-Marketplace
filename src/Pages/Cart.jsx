import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchCart, removeCartItem, updateCartItem } from '../api/shopping'

const money = (value, currency) => new Intl.NumberFormat(undefined, {
  style: 'currency', currency: currency || 'INR',
}).format(value || 0)

const Cart = () => {
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busyItem, setBusyItem] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchCart().then((data) => {
      if (active) { setCart(data); setLoading(false) }
    }).catch((requestError) => {
      if (active) {
        setError(requestError.response?.data?.detail || 'Your cart could not be loaded.')
        setLoading(false)
      }
    })
    return () => { active = false }
  }, [])

  const changeQuantity = async (item, quantity) => {
    setBusyItem(item.id)
    setError('')
    try {
      setCart(await updateCartItem(item.id, quantity))
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'The quantity could not be updated.')
    } finally {
      setBusyItem(null)
    }
  }

  const removeItem = async (item) => {
    setBusyItem(item.id)
    setError('')
    try {
      setCart(await removeCartItem(item.id))
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'The item could not be removed.')
    } finally {
      setBusyItem(null)
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Loading your cart…</p></main>
  if (error && !cart) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error}</div><Link to="/products">Continue shopping</Link></main>
  const items = cart?.items || []

  return (
    <main className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4"><h1 className="h2">Your cart</h1><Link to="/products">Continue shopping</Link></div>
      {error && <div className="alert alert-warning" role="alert">{error}</div>}
      {items.length === 0 ? <div className="card card-body text-center py-5"><h2 className="h5">Your cart is empty</h2><p className="text-secondary">Browse products and add something you like.</p><Link className="btn btn-primary align-self-center" to="/products">Browse products</Link></div> :
        <div className="row g-4">
          <section className="col-12 col-lg-8" aria-label="Cart items">
            <ul className="list-group">
              {items.map((item) => <li className="list-group-item p-3" key={item.id}>
                <div className="d-flex flex-wrap gap-3 align-items-center">
                  {item.imageUrl && <img src={item.imageUrl} alt={item.imageAlt || item.name} width="96" height="96" className="rounded" style={{ objectFit: 'cover' }} />}
                  <div className="flex-grow-1"><Link to={`/products/${item.productId}`} className="fw-semibold">{item.name}</Link>
                    {item.sellerName && <p className="small text-secondary mb-1">Sold by {item.sellerName}</p>}
                    <p className="mb-0">{money(item.unitPrice, item.currency)} each</p>
                  </div>
                  <div className="d-flex align-items-center gap-2" role="group" aria-label={`Quantity for ${item.name}`}>
                    <button className="btn btn-sm btn-outline-secondary" type="button" aria-label={`Decrease ${item.name} quantity`} disabled={busyItem === item.id || item.quantity <= 1} onClick={() => changeQuantity(item, item.quantity - 1)}>−</button>
                    <span aria-live="polite" className="px-2">{item.quantity}</span>
                    <button className="btn btn-sm btn-outline-secondary" type="button" aria-label={`Increase ${item.name} quantity`} disabled={busyItem === item.id || (item.availableQuantity && item.quantity >= item.availableQuantity)} onClick={() => changeQuantity(item, item.quantity + 1)}>+</button>
                    <button className="btn btn-outline-danger" type="button" disabled={busyItem === item.id} onClick={() => removeItem(item)}>Remove</button>
                  </div>
                  <strong className="ms-auto">{money(item.lineTotal, item.currency)}</strong>
                </div>
              </li>)}
            </ul>
          </section>
          <aside className="col-12 col-lg-4">
            <div className="card card-body">
              <h2 className="h5">Order summary</h2>
              <div className="d-flex justify-content-between"><span>Subtotal</span><strong>{money(cart.subtotal, cart.currency)}</strong></div>
              <p className="small text-secondary mt-3">Shipping, discounts, and taxes are calculated by the server during checkout.</p>
              <Link className="btn btn-primary" to="/checkout">Continue to checkout</Link>
            </div>
          </aside>
        </div>}
    </main>
  )
}

export default Cart
