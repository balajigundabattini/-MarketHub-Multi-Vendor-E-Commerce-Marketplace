import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import { placeOrder, requestCheckoutQuote } from '../api/checkout'
import { fetchCart } from '../api/shopping'

const money = (value, currency) => new Intl.NumberFormat(undefined, {
  style: 'currency', currency: currency || 'INR',
}).format(value || 0)

const Checkout = () => {
  const navigate = useNavigate()
  const [addresses, setAddresses] = useState([])
  const [cartItems, setCartItems] = useState([])
  const [addressId, setAddressId] = useState('')
  const [couponCode, setCouponCode] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('RAZORPAY')
  const [quote, setQuote] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    Promise.all([apiClient.get('/customer/addresses'), fetchCart()])
      .then(([addressResponse, cart]) => {
        if (!active) return
        const savedAddresses = addressResponse.data || []
        setAddresses(savedAddresses)
        setAddressId(String(savedAddresses.find((address) => address.isDefault)?.id || savedAddresses[0]?.id || ''))
        setCartItems(cart.items || [])
        setLoading(false)
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.detail || 'Checkout could not be loaded.')
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [])

  const getQuote = async (event) => {
    event.preventDefault()
    if (!addressId) {
      setError('Add a delivery address before requesting a quote.')
      return
    }
    setBusy(true)
    setError('')
    setQuote(null)
    try {
      setQuote(await requestCheckoutQuote({ addressId: Number(addressId), couponCode: couponCode.trim() || null }))
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'The order total could not be quoted.')
    } finally {
      setBusy(false)
    }
  }

  const submitOrder = async () => {
    if (!quote?.quoteId) return
    setBusy(true)
    setError('')
    try {
      const order = await placeOrder({ quoteId: quote.quoteId, addressId: Number(addressId), paymentMethod })
      navigate(order.paymentId ? `/payments/${order.paymentId}` : `/orders/${order.orderId}`, { replace: true })
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'The order could not be placed. Your cart has not been charged.')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Preparing checkout…</p></main>
  if (error && addresses.length === 0 && cartItems.length === 0) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error}</div><Link to="/cart">Back to cart</Link></main>
  if (cartItems.length === 0) return <main className="container py-5"><h1 className="h3">Your cart is empty</h1><Link to="/products">Browse products</Link></main>

  return (
    <main className="container py-4" style={{ maxWidth: 960 }}>
      <h1 className="h2 mb-4">Checkout</h1>
      {error && <div className="alert alert-warning" role="alert">{error}</div>}
      <div className="row g-4">
        <section className="col-12 col-lg-7">
          <form className="card card-body mb-4" onSubmit={getQuote}>
            <h2 className="h5">Delivery address</h2>
            {addresses.length === 0 ? <p>No address saved. <Link to="/account">Add a delivery address</Link>.</p> : <>
              <label htmlFor="checkout-address" className="form-label">Choose an address</label>
              <select id="checkout-address" className="form-select mb-3" value={addressId} required onChange={(event) => { setAddressId(event.target.value); setQuote(null) }}>
                {addresses.map((address) => <option key={address.id} value={address.id}>{address.recipientName} — {address.line1}, {address.city} {address.postalCode}{address.isDefault ? ' (default)' : ''}</option>)}
              </select>
              <label htmlFor="checkout-coupon" className="form-label">Coupon code (optional)</label>
              <input id="checkout-coupon" className="form-control mb-3" maxLength={80} value={couponCode} onChange={(event) => { setCouponCode(event.target.value); setQuote(null) }} />
              <button className="btn btn-outline-primary align-self-start" type="submit" disabled={busy}>Calculate server quote</button>
            </>}
          </form>
          <section className="card card-body">
            <h2 className="h5">Payment method</h2>
            <label htmlFor="payment-method" className="form-label">Choose a method</label>
            <select id="payment-method" className="form-select" value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}>
              <option value="RAZORPAY">Online payment</option>
              <option value="COD">Cash on delivery</option>
            </select>
            <p className="small text-secondary mt-2 mb-0">Payment will be handled and verified by the server-side payment workflow.</p>
          </section>
        </section>
        <aside className="col-12 col-lg-5">
          <section className="card card-body">
            <h2 className="h5">Order summary</h2>
            <ul className="list-group list-group-flush mb-3">
              {cartItems.map((item) => <li className="list-group-item px-0 d-flex justify-content-between gap-3" key={item.id}><span>{item.name} × {item.quantity}</span><span>{money(item.lineTotal, item.currency)}</span></li>)}
            </ul>
            {!quote ? <p className="text-secondary">Request a server quote to see shipping, discounts, taxes, and the payable total.</p> : <>
              <div className="d-flex justify-content-between"><span>Items</span><span>{money(quote.subtotal, quote.currency)}</span></div>
              <div className="d-flex justify-content-between"><span>Shipping</span><span>{money(quote.shipping, quote.currency)}</span></div>
              <div className="d-flex justify-content-between"><span>Tax</span><span>{money(quote.tax, quote.currency)}</span></div>
              <div className="d-flex justify-content-between"><span>Discount</span><span>−{money(quote.discount, quote.currency)}</span></div>
              <hr /><div className="d-flex justify-content-between fs-5"><strong>Total</strong><strong>{money(quote.total, quote.currency)}</strong></div>
              <p className="small text-secondary mt-2">Quote expires {quote.expiresAt ? new Date(quote.expiresAt).toLocaleString() : 'soon'}. The server revalidates stock and price when placing the order.</p>
              <button className="btn btn-primary w-100" type="button" disabled={busy || !addressId} onClick={submitOrder}>Place order</button>
            </>}
          </section>
        </aside>
      </div>
    </main>
  )
}

export default Checkout
