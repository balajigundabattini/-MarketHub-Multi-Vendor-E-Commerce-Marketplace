import { useCallback, useEffect, useState } from 'react'
import { adjustSellerInventory, fetchSellerInventory } from '../api/seller'

const SellerInventory = () => {
  const [items, setItems] = useState([])
  const [quantities, setQuantities] = useState({})
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      const data = await fetchSellerInventory()
      const rows = Array.isArray(data) ? data : data.content || []
      setItems(rows)
      setQuantities(Object.fromEntries(rows.map((item) => [item.productId, String(item.onHand)])))
      setError('')
    } catch (requestError) { setError(requestError.response?.data?.detail || 'Inventory could not be loaded.') }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { Promise.resolve().then(load) }, [load])

  const save = async (item) => {
    const onHand = Number(quantities[item.productId])
    if (!Number.isInteger(onHand) || onHand < item.reserved) { setError('On-hand stock must be a whole number and cannot be below reserved stock.'); return }
    setBusyId(item.productId)
    setError('')
    try { await adjustSellerInventory(item.productId, { onHand, reason: 'SELLER_ADJUSTMENT' }); await load(); setMessage('Inventory updated.') }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Inventory could not be updated.') }
    finally { setBusyId(null) }
  }

  return <main><h1 className="h2 mb-4">Inventory</h1>{message && <p className="alert alert-info" role="status">{message}</p>}{error && <p className="alert alert-warning" role="alert">{error}</p>}
    {loading ? <p role="status">Loading inventory…</p> : items.length === 0 ? <div className="card card-body">There are no active product inventory records yet.</div> :
      <div className="table-responsive"><table className="table align-middle"><thead><tr><th>Product</th><th>SKU</th><th>On hand</th><th>Reserved</th><th>Available</th><th>Update stock</th></tr></thead>
        <tbody>{items.map((item) => <tr key={item.productId}><th scope="row">{item.name}</th><td>{item.sku}</td><td>{item.onHand}</td><td>{item.reserved}</td><td>{item.available}</td><td><div className="d-flex gap-2"><label className="visually-hidden" htmlFor={`stock-${item.productId}`}>On-hand units for {item.name}</label><input id={`stock-${item.productId}`} className="form-control" style={{ width: 100 }} type="number" min={item.reserved} step="1" value={quantities[item.productId] ?? ''} onChange={(event) => setQuantities({ ...quantities, [item.productId]: event.target.value })} /><button className="btn btn-sm btn-primary" type="button" disabled={busyId === item.productId} onClick={() => save(item)}>Save</button></div></td></tr>)}</tbody>
      </table></div>}
  </main>
}

export default SellerInventory
