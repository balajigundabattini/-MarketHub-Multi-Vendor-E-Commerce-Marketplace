import { useCallback, useEffect, useState } from 'react'
import { createAdminSettlement, fetchAdminSettlementSellers, fetchAdminSettlements, markAdminSettlementPaid } from '../api/admin'

const dateRange = () => { const to = new Date(); const from = new Date(to); from.setDate(1); return { periodFrom: from.toISOString().slice(0, 10), periodTo: to.toISOString().slice(0, 10) } }

const AdminSettlements = () => {
  const [rows, setRows] = useState([])
  const [sellers, setSellers] = useState([])
  const [form, setForm] = useState({ sellerId: '', ...dateRange() })
  const [references, setReferences] = useState({})
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const load = useCallback(async () => { try { const [settlementRows, sellerPage] = await Promise.all([fetchAdminSettlements(), fetchAdminSettlementSellers()]); setRows(settlementRows); setSellers(sellerPage.content || []); setError('') } catch (e) { setError(e.response?.data?.detail || 'Settlements could not be loaded.') } finally { setLoading(false) } }, [])
  useEffect(() => { Promise.resolve().then(load) }, [load])
  const create = async (event) => { event.preventDefault(); setBusy('create'); setError(''); setMessage(''); try { await createAdminSettlement({ sellerId: Number(form.sellerId), periodFrom: form.periodFrom, periodTo: form.periodTo }); setMessage('Settlement created.'); await load() } catch (e) { setError(e.response?.data?.detail || 'Settlement could not be created.') } finally { setBusy('') } }
  const pay = async (row) => { const reference = references[row.id]?.trim(); if (!reference) { setError('Enter a payout reference first.'); return } setBusy(String(row.id)); setError(''); try { await markAdminSettlementPaid(row.id, reference); setMessage(`Settlement ${row.id} marked paid.`); await load() } catch (e) { setError(e.response?.data?.detail || 'Settlement could not be updated.') } finally { setBusy('') } }
  const money = (value, currency) => new Intl.NumberFormat(undefined, { style: 'currency', currency: currency || 'INR' }).format(value || 0)
  return <main><h1 className="h2 mb-4">Seller settlements</h1>
    {message && <p className="alert alert-success" role="status">{message}</p>}{error && <p className="alert alert-warning" role="alert">{error}</p>}
    <form className="card card-body mb-4" onSubmit={create}><h2 className="h5">Create settlement</h2><p className="small text-secondary">Enter the approved seller ID. The settlement captures eligible sales and commission for the selected dates.</p><div className="row g-3 align-items-end">
      <div className="col-md-3"><label className="form-label" htmlFor="settlement-seller">Approved seller</label><select id="settlement-seller" className="form-select" required value={form.sellerId} onChange={(e) => setForm({ ...form, sellerId: e.target.value })}><option value="">Select seller</option>{sellers.map((seller) => <option key={seller.id} value={seller.id}>{seller.businessName} (#{seller.id})</option>)}</select></div>
      <div className="col-md-3"><label className="form-label" htmlFor="settlement-from">From</label><input id="settlement-from" className="form-control" type="date" required value={form.periodFrom} onChange={(e) => setForm({ ...form, periodFrom: e.target.value })} /></div>
      <div className="col-md-3"><label className="form-label" htmlFor="settlement-to">To</label><input id="settlement-to" className="form-control" type="date" required value={form.periodTo} onChange={(e) => setForm({ ...form, periodTo: e.target.value })} /></div>
      <div className="col-md-3"><button className="btn btn-primary w-100" disabled={busy === 'create'}>Create settlement</button></div>
    </div></form>
    {loading ? <p role="status">Loading settlements…</p> : rows.length === 0 ? <p className="card card-body">No settlements have been created.</p> : <div className="table-responsive"><table className="table align-middle"><thead><tr><th>Seller</th><th>Period</th><th>Gross</th><th>Commission</th><th>Net</th><th>Status / reference</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td>{row.businessName} <span className="text-secondary">(#{row.sellerId})</span></td><td>{row.periodFrom} – {row.periodTo}</td><td>{money(row.grossAmount, row.currency)}</td><td>{money(row.commissionAmount, row.currency)}</td><td>{money(row.netAmount, row.currency)}</td><td>{row.status === 'PAID' ? <><span className="badge text-bg-success">PAID</span><div className="small">{row.payoutReference}</div></> : <div className="d-flex gap-2"><input aria-label={`Payout reference for settlement ${row.id}`} className="form-control form-control-sm" maxLength={120} value={references[row.id] || ''} onChange={(e) => setReferences({ ...references, [row.id]: e.target.value })} /><button className="btn btn-sm btn-success text-nowrap" type="button" disabled={Boolean(busy)} onClick={() => pay(row)}>Mark paid</button></div>}</td></tr>)}</tbody></table></div>}
  </main>
}
export default AdminSettlements
