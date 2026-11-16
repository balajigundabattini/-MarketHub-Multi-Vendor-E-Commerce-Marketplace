import { useCallback, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { deactivateSellerProduct, fetchSellerProducts } from '../api/seller'

const SellerProducts = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryKey = searchParams.toString()
  const [data, setData] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [loadedKey, setLoadedKey] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const query = Object.fromEntries(new URLSearchParams(queryKey))
    try {
      setData(await fetchSellerProducts({ page: Number(query.page || 0), size: 20 }))
      setError('')
    } catch (requestError) { setError(requestError.response?.data?.detail || 'Products could not be loaded.') }
    finally { setLoading(false); setLoadedKey(queryKey) }
  }, [queryKey])

  useEffect(() => { Promise.resolve().then(load) }, [load])

  const deactivate = async (product) => {
    setBusyId(product.id)
    try { await deactivateSellerProduct(product.id); await load() }
    catch (requestError) { setError(requestError.response?.data?.detail || 'Product could not be deactivated.') }
    finally { setBusyId(null) }
  }

  const isLoading = loading || loadedKey !== queryKey
  const changePage = (page) => { setLoading(true); const next = new URLSearchParams(searchParams); next.set('page', String(page)); setSearchParams(next) }

  return <main>
    <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4"><div><h1 className="h2 mb-1">Products</h1><p className="text-secondary mb-0">Manage your listings and moderation status.</p></div><Link className="btn btn-primary" to="new">Add product</Link></div>
    {error && <p className="alert alert-warning" role="alert">{error}</p>}
    {isLoading && <p role="status">Loading products…</p>}
    {!isLoading && !error && data.content?.length === 0 && <div className="card card-body"><p className="mb-0">No products yet. Add your first listing.</p></div>}
    {!isLoading && !error && data.content?.length > 0 && <div className="table-responsive"><table className="table align-middle">
      <thead><tr><th>Product</th><th>SKU</th><th>Price</th><th>Status</th><th>Actions</th></tr></thead>
      <tbody>{data.content.map((product) => <tr key={product.id}><td>{product.name}</td><td>{product.sku}</td><td>{new Intl.NumberFormat(undefined, { style: 'currency', currency: product.currency || 'INR' }).format(product.price)}</td><td><span className="badge text-bg-secondary">{product.status}</span></td>
        <td className="d-flex gap-2"><Link className="btn btn-sm btn-outline-primary" to={`${product.id}/edit`}>Edit</Link>{product.status !== 'INACTIVE' && <button className="btn btn-sm btn-outline-danger" type="button" disabled={busyId === product.id} onClick={() => deactivate(product)}>Deactivate</button>}</td>
      </tr>)}</tbody>
    </table></div>}
    {!isLoading && data.totalPages > 1 && <nav className="d-flex justify-content-center align-items-center gap-3" aria-label="Seller product pages"><button className="btn btn-outline-secondary" type="button" disabled={data.number <= 0} onClick={() => changePage(data.number - 1)}>Previous</button><span>Page {data.number + 1} of {data.totalPages}</span><button className="btn btn-outline-secondary" type="button" disabled={data.number + 1 >= data.totalPages} onClick={() => changePage(data.number + 1)}>Next</button></nav>}
  </main>
}

export default SellerProducts
