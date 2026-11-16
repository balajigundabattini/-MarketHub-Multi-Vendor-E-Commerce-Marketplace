import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchProduct } from '../api/catalog'

const ProductCompare = () => {
  const [searchParams] = useSearchParams()
  const queryKey = searchParams.toString()
  const ids = useMemo(() => (new URLSearchParams(queryKey).get('ids') || '').split(',').filter(Boolean).slice(0, 4), [queryKey])
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadedQueryKey, setLoadedQueryKey] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    if (ids.length < 2) {
      return () => { active = false }
    }
    Promise.all(ids.map(fetchProduct))
      .then((results) => {
        if (active) {
          setProducts(results)
          setError('')
          setLoadedQueryKey(queryKey)
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.detail || 'Some products could not be loaded for comparison.')
          setLoadedQueryKey(queryKey)
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [ids, queryKey])

  const isLoading = loading || loadedQueryKey !== queryKey
  if (ids.length < 2) return <main className="container py-5"><h1 className="h3">Compare products</h1><p>Select at least two products in the catalog.</p><Link to="/products">Browse products</Link></main>
  if (isLoading) return <main className="container py-5"><p role="status">Loading comparison…</p></main>
  if (error) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error}</div><Link to="/products">Back to products</Link></main>

  const specifications = [...new Set(products.flatMap((product) => (product.specifications || []).map((item) => item.name)))]
  return (
    <main className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-3"><h1 className="h2">Compare products</h1><Link to="/products">Back to products</Link></div>
      <div className="table-responsive">
        <table className="table table-bordered align-middle">
          <caption className="visually-hidden">Side by side product comparison</caption>
          <thead><tr><th scope="col">Feature</th>{products.map((product) => <th scope="col" key={product.id}>{product.name}</th>)}</tr></thead>
          <tbody>
            <tr><th scope="row">Image</th>{products.map((product) => <td key={product.id}>{product.images?.[0]?.url && <img src={product.images[0].url} alt={product.images[0].altText || product.name} width="100" height="100" style={{ objectFit: 'cover' }} />}</td>)}</tr>
            <tr><th scope="row">Price</th>{products.map((product) => <td key={product.id}>{new Intl.NumberFormat(undefined, { style: 'currency', currency: product.currency || 'INR' }).format(product.price)}</td>)}</tr>
            <tr><th scope="row">Brand</th>{products.map((product) => <td key={product.id}>{product.brand || '—'}</td>)}</tr>
            <tr><th scope="row">Seller</th>{products.map((product) => <td key={product.id}>{product.storeName || '—'}</td>)}</tr>
            <tr><th scope="row">Description</th>{products.map((product) => <td key={product.id}>{product.description}</td>)}</tr>
            {specifications.map((name) => <tr key={name}><th scope="row">{name}</th>{products.map((product) => <td key={product.id}>{product.specifications?.find((item) => item.name === name)?.value || '—'}</td>)}</tr>)}
          </tbody>
        </table>
      </div>
    </main>
  )
}

export default ProductCompare
