import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { fetchCategories } from '../api/catalog'
import { createSellerProduct, fetchSellerProduct, updateSellerProduct } from '../api/seller'

const empty = { sku: '', name: '', categoryId: '', brand: '', description: '', price: '', currency: 'INR', imageUrls: '' }

const SellerProductForm = () => {
  const { productId } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState(empty)
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(Boolean(productId))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchCategories().then((data) => { if (active) setCategories(Array.isArray(data) ? data : data.content || []) }).catch(() => { if (active) setCategories([]) })
    if (!productId) return () => { active = false }
    fetchSellerProduct(productId).then((product) => {
      if (active) setForm({ sku: product.sku || '', name: product.name || '', categoryId: String(product.categoryId || ''), brand: product.brand || '', description: product.description || '', price: String(product.price ?? ''), currency: product.currency || 'INR', imageUrls: (product.images || []).map((image) => image.url).join('\n') })
    }).catch((requestError) => { if (active) setError(requestError.response?.data?.detail || 'Product could not be loaded.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [productId])

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    const request = { ...form, categoryId: Number(form.categoryId), price: Number(form.price), imageUrls: form.imageUrls.split('\n').map((url) => url.trim()).filter(Boolean) }
    try {
      if (productId) await updateSellerProduct(productId, request)
      else await createSellerProduct(request)
      navigate('/seller/products')
    } catch (requestError) { setError(requestError.response?.data?.detail || 'Product could not be saved.') }
    finally { setBusy(false) }
  }

  if (loading) return <p role="status">Loading product…</p>
  return <main>
    <p><Link to="/seller/products">← Products</Link></p><h1 className="h2">{productId ? 'Edit product' : 'Add product'}</h1>
    {error && <p className="alert alert-warning" role="alert">{error}</p>}
    <form className="card card-body" onSubmit={submit}>
      <div className="row g-3">
        <div className="col-md-6"><label className="form-label" htmlFor="product-name">Product name</label><input id="product-name" className="form-control" maxLength={200} required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></div>
        <div className="col-md-6"><label className="form-label" htmlFor="product-sku">SKU</label><input id="product-sku" className="form-control" maxLength={80} required value={form.sku} onChange={(event) => setForm({ ...form, sku: event.target.value })} /></div>
        <div className="col-md-6"><label className="form-label" htmlFor="product-category">Category</label><select id="product-category" className="form-select" required value={form.categoryId} onChange={(event) => setForm({ ...form, categoryId: event.target.value })}><option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
        <div className="col-md-6"><label className="form-label" htmlFor="product-brand">Brand (optional)</label><input id="product-brand" className="form-control" maxLength={120} value={form.brand} onChange={(event) => setForm({ ...form, brand: event.target.value })} /></div>
        <div className="col-md-6"><label className="form-label" htmlFor="product-price">Price</label><input id="product-price" className="form-control" type="number" min="0.01" step="0.01" required value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} /></div>
        <div className="col-md-6"><label className="form-label" htmlFor="product-currency">Currency</label><select id="product-currency" className="form-select" value={form.currency} onChange={(event) => setForm({ ...form, currency: event.target.value })}><option value="INR">INR</option><option value="USD">USD</option></select></div>
        <div className="col-12"><label className="form-label" htmlFor="product-description">Description</label><textarea id="product-description" className="form-control" rows="6" maxLength={20000} required value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></div>
        <div className="col-12"><label className="form-label" htmlFor="product-images">Image URLs (one per line)</label><textarea id="product-images" className="form-control" rows="3" value={form.imageUrls} onChange={(event) => setForm({ ...form, imageUrls: event.target.value })} /><div className="form-text">Image upload/storage will be connected when the media service is implemented.</div></div>
      </div>
      <div className="d-flex gap-2 mt-4"><button className="btn btn-primary" type="submit" disabled={busy || categories.length === 0}>{busy ? 'Saving…' : 'Save as draft'}</button><Link className="btn btn-outline-secondary" to="/seller/products">Cancel</Link></div>
    </form>
  </main>
}

export default SellerProductForm
