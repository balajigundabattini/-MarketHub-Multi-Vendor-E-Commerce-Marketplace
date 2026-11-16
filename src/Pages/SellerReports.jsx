import { useCallback, useEffect, useState } from 'react'
import { fetchSellerReports } from '../api/seller'

const initialRange = () => {
  const end = new Date()
  const start = new Date(end)
  start.setDate(end.getDate() - 29)
  return { from: start.toISOString().slice(0, 10), to: end.toISOString().slice(0, 10) }
}

const SellerReports = () => {
  const [range, setRange] = useState(initialRange)
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (params) => {
    setBusy(true)
    setError('')
    try { setReport(await fetchSellerReports(params)) }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Reports could not be loaded.') }
    finally { setBusy(false); setLoading(false) }
  }, [])
  useEffect(() => { Promise.resolve().then(() => load(initialRange())) }, [load])

  const submit = (event) => { event.preventDefault(); load(range) }
  const currency = report?.currency || 'INR'
  const amount = (value) => new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(value || 0)

  return <main><h1 className="h2 mb-4">Sales and commission reports</h1>
    <form className="card card-body mb-4" onSubmit={submit}><div className="row g-3 align-items-end">
      <div className="col-sm-5"><label className="form-label" htmlFor="report-from">From</label><input id="report-from" type="date" className="form-control" required value={range.from} onChange={(event) => setRange({ ...range, from: event.target.value })} /></div>
      <div className="col-sm-5"><label className="form-label" htmlFor="report-to">To</label><input id="report-to" type="date" className="form-control" required value={range.to} onChange={(event) => setRange({ ...range, to: event.target.value })} /></div>
      <div className="col-sm-2"><button className="btn btn-primary w-100" type="submit" disabled={busy}>Apply</button></div>
    </div></form>
    {error && <p className="alert alert-warning" role="alert">{error}</p>}
    {loading && <p role="status">Loading reports…</p>}
    {report && <>
      <div className="row g-3 mb-4">{[["Gross sales", report.grossSales], ["Commission", report.commission], ["Estimated seller earnings", report.netEarnings], ["Orders", report.orderCount]].map(([label, value]) => <div key={label} className="col-12 col-sm-6 col-xl-3"><section className="card card-body h-100"><span className="small text-secondary">{label}</span><strong className="fs-4">{label === 'Orders' ? value ?? '—' : amount(value)}</strong></section></div>)}</div>
      <section className="card card-body"><h2 className="h5">Daily sales</h2>{(report.dailySales || []).length === 0 ? <p className="text-secondary mb-0">No sales recorded for this period.</p> : <div className="table-responsive"><table className="table"><thead><tr><th>Date</th><th>Orders</th><th>Gross sales</th><th>Commission</th><th>Net earnings</th></tr></thead><tbody>{report.dailySales.map((day) => <tr key={day.date}><td>{day.date}</td><td>{day.orderCount}</td><td>{amount(day.grossSales)}</td><td>{amount(day.commission)}</td><td>{amount(day.netEarnings)}</td></tr>)}</tbody></table></div>}</section>
    </>}
  </main>
}

export default SellerReports
