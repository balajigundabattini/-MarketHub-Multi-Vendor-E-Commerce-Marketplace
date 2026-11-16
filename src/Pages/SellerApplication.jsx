import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { fetchSellerApplication, submitSellerApplication } from '../api/seller'

const SellerApplication = () => {
  const [form, setForm] = useState({ legalName: '', businessName: '' })
  const [application, setApplication] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    fetchSellerApplication().then((data) => { if (active) { setApplication(data); setLoading(false) } })
      .catch((error) => {
        if (active) {
          if (error.response?.status !== 404) setMessage(error.response?.data?.detail || 'Application status could not be loaded.')
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [])

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setMessage('')
    try {
      setApplication(await submitSellerApplication(form))
      setMessage('Your application was submitted for review.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'The application could not be submitted.')
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Checking seller application…</p></main>
  return (
    <main className="container py-5" style={{ maxWidth: 720 }}>
      <p className="eyebrow">Sell on MarketHub</p><h1 className="h2">Seller application</h1>
      <p>Submit your legal and store names. Seller tools unlock only after the marketplace approves the application.</p>
      {message && <p role="status" className="alert alert-info">{message}</p>}
      {application ? <section className="card card-body"><h2 className="h5">Application status: {application.status}</h2><p className="mb-0">{application.businessName} · {application.legalName}</p>{application.reviewNote && <p className="mt-3 mb-0">Review note: {application.reviewNote}</p>}</section> :
        <form className="card card-body" onSubmit={submit}>
          <label className="form-label" htmlFor="seller-legal-name">Legal name</label><input id="seller-legal-name" className="form-control mb-3" maxLength={160} required value={form.legalName} onChange={(event) => setForm({ ...form, legalName: event.target.value })} />
          <label className="form-label" htmlFor="seller-business-name">Business name</label><input id="seller-business-name" className="form-control mb-3" maxLength={160} required value={form.businessName} onChange={(event) => setForm({ ...form, businessName: event.target.value })} />
          <button className="btn btn-primary align-self-start" type="submit" disabled={busy}>{busy ? 'Submitting…' : 'Submit application'}</button>
        </form>}
      <p className="mt-3"><Link to="/">Back to MarketHub</Link></p>
    </main>
  )
}

export default SellerApplication
