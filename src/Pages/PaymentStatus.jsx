import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchPayment, retryPayment } from '../api/payments'

const PaymentStatus = () => {
  const { paymentId } = useParams()
  const [payment, setPayment] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

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
        <h1 className="h2">{state === 'CAPTURED' || state === 'SUCCEEDED' ? 'Payment received' : state === 'FAILED' ? 'Payment failed' : 'Payment is processing'}</h1>
        <p className="fs-5">Status: <strong>{state}</strong></p>
        {payment.amount != null && <p>{new Intl.NumberFormat(undefined, { style: 'currency', currency: payment.currency || 'INR' }).format(payment.amount)}</p>}
        {error && <div className="alert alert-warning" role="alert">{error}</div>}
        {state === 'FAILED' && payment.canRetry && <button className="btn btn-primary align-self-center" type="button" disabled={busy} onClick={retry}>{busy ? 'Starting…' : 'Try payment again'}</button>}
        <div className="d-flex justify-content-center gap-3 mt-3"><Link to={`/orders/${payment.orderId}`}>View order</Link><Link to="/orders">All orders</Link></div>
        <p className="small text-secondary mt-3 mb-0">The server verifies the provider result before marking an order paid. Closing this page does not change the payment status.</p>
      </section>
    </main>
  )
}

export default PaymentStatus
