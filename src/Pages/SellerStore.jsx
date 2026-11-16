import { useEffect, useState } from 'react'
import { fetchStore, updateStore } from '../api/seller'

const SellerStore = () => {
  const [form, setForm] = useState({ name: '', description: '' })
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    fetchStore().then((store) => { if (active) { setForm({ name: store.name || '', description: store.description || '' }); setLoading(false) } })
      .catch((error) => { if (active) { setMessage(error.response?.data?.detail || 'Store information could not be loaded.'); setLoading(false) } })
    return () => { active = false }
  }, [])

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    try { const store = await updateStore(form); setForm({ name: store.name, description: store.description || '' }); setMessage('Store profile saved.') }
    catch (error) { setMessage(error.response?.data?.detail || 'Store profile could not be saved.') }
    finally { setBusy(false) }
  }

  if (loading) return <p role="status">Loading store profile…</p>
  return <main><h1 className="h2 mb-4">Store management</h1>{message && <p className="alert alert-info" role="status">{message}</p>}
    <form className="card card-body" onSubmit={save}>
      <label htmlFor="store-name" className="form-label">Store name</label><input id="store-name" className="form-control mb-3" maxLength={160} required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
      <label htmlFor="store-description" className="form-label">Store description</label><textarea id="store-description" className="form-control mb-3" rows="5" maxLength={4000} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
      <button className="btn btn-primary align-self-start" type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save store profile'}</button>
    </form>
  </main>
}

export default SellerStore
