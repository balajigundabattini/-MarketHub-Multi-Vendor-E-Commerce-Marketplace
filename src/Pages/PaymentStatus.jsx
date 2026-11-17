import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchPayment, retryPayment, verifyPayment } from '../api/payments'

const PaymentStatus = () => {
  const { paymentId } = useParams()
  const [payment, setPayment] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [checkoutReady, setCheckoutReady] = useState(false)

  const load = useCallback(async () => {
    try {
      setPayment(await fetchPayment(paymentId))
      setError('')
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'Payment details could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [paymentId])

  useEffect(() => { Promise.resolve().then(load) }, [load])

  useEffect(() => {
    if (!payment?.providerOrderId || payment.status !== 'PENDING' || !payment.keyId) return undefined
    let active = true
    const openCheckout = async () => {
      try {
        if (!window.Razorpay) {
          await new Promise((resolve, reject) => {
            const script = document.createElement('script')
            script.src = 'https://checkout.razorpay.com/v1/checkout.js'
            script.onload = resolve
            script.onerror = reject
            document.body.appendChild(script)
          })
        }
        if (!active) return
        setCheckoutReady(true)
      } catch {
        if (active) setError('The payment window could not be loaded. Check your connection and try again.')
      }
    }
    openCheckout()
    return () => { active = false }
  }, [payment?.paymentId, payment?.providerOrderId, payment?.status, payment?.keyId, payment?.amount, payment?.currency])

  const retry = async () => {
    setBusy(true)
    try {
      setPayment(await retryPayment(paymentId))
      setError('')
    } catch (requestError) {
      setError(requestError.response?.data?.detail || 'Payment could not be retried.')
    } finally {
      setBusy(false)
    }
  }


  if (loading) return <main className="container py-5"><p role="status">Checking payment status…</p></main>
  if (!payment) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error}</div><Link to="/orders">View orders</Link></main>
  const state = payment.status || 'PENDING'

  return (
    <main className="container py-5" style={{ maxWidth: 700 }}>
      <section className="card card-body text-center">
        <p className="eyebrow">Payment update</p>
        <h1 className="h2">{state === 'CAPTURED' || state === 'SUCCEEDED' ? 'Payment received' : state === 'FAILED' ? 'Payment failed' : state === 'EXPIRED' ? 'Payment window expired' : 'Payment is processing'}</h1>
        <p className="fs-5">Status: <strong>{state}</strong></p>
        {payment.amount != null && <p>{new Intl.NumberFormat(undefined, { style: 'currency', currency: payment.currency || 'INR' }).format(payment.amount)}</p>}
        {error && <div className="alert alert-warning" role="alert">{error}</div>}
        {state === 'PENDING' && payment.providerOrderId && !checkoutReady && <p role="status">Preparing secure checkout…</p>}
        {state === 'PENDING' && payment.providerOrderId && checkoutReady && <button className="btn btn-primary align-self-center" type="button" onClick={() => window.Razorpay && new window.Razorpay({ key: payment.keyId, amount: payment.amountMinor, currency: payment.currency, name: 'MarketHub', order_id: payment.providerOrderId, handler: async (response) => { try { setPayment(await verifyPayment(payment.paymentId, response)); setError('') } catch (requestError) { setError(requestError.response?.data?.detail || 'Payment verification could not be completed.') } } }).open()}>Open payment window</button>}
        {state === 'FAILED' && payment.canRetry && <button className="btn btn-primary align-self-center" type="button" disabled={busy} onClick={retry}>{busy ? 'Starting…' : 'Try payment again'}</button>}
        <div className="d-flex justify-content-center gap-3 mt-3"><Link to={`/orders/${payment.orderId}`}>View order</Link><Link to="/orders">All orders</Link></div>
        <p className="small text-secondary mt-3 mb-0">The server verifies the provider result before marking an order paid. Closing this page does not change the payment status.</p>
      </section>
    </main>
  )
}

export default PaymentStatus
