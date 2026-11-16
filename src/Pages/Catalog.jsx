import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchCategories, fetchProducts } from '../api/catalog'

const SORTS = [
  ['newest', 'Newest'],
  ['price_asc', 'Price: low to high'],
  ['price_desc', 'Price: high to low'],
  ['name_asc', 'Name: A to Z'],
]

const formatPrice = (price, currency) => new Intl.NumberFormat(undefined, {
  style: 'currency', currency: currency || 'INR',
}).format(price)

const Catalog = () => {
  const [searchParams, setSearchParams] = useSearchParams()
  const queryKey = searchParams.toString()
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [pageInfo, setPageInfo] = useState({ page: 0, totalPages: 0, totalElements: 0, size: 12 })
  const [loading, setLoading] = useState(true)
  const [loadedQueryKey, setLoadedQueryKey] = useState(null)
  const [error, setError] = useState('')
  const [compareIds, setCompareIds] = useState([])
  const [compareError, setCompareError] = useState('')

  useEffect(() => {
    let active = true
    fetchCategories().then((data) => {
      if (active) setCategories(Array.isArray(data) ? data : data.content || [])
    }).catch(() => {
      if (active) setCategories([])
    })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    const params = Object.fromEntries(new URLSearchParams(queryKey))
    fetchProducts({ ...params, page: Number(params.page || 0), size: Number(params.size || 12) })
      .then((data) => {
        if (!active) return
        setProducts(data.content || [])
        setPageInfo({
          page: data.number ?? Number(params.page || 0),
          totalPages: data.totalPages ?? 0,
          totalElements: data.totalElements ?? data.content?.length ?? 0,
          size: data.size ?? Number(params.size || 12),
        })
        setError('')
        setLoading(false)
        setLoadedQueryKey(queryKey)
      })
      .catch((requestError) => {
        if (!active) return
        setProducts([])
        setError(requestError.response?.data?.detail || 'The catalog could not be loaded. Try again in a moment.')
        setLoading(false)
        setLoadedQueryKey(queryKey)
      })
    return () => { active = false }
  }, [queryKey])

  const submitFilters = (event) => {
    event.preventDefault()
    setLoading(true)
    const form = new FormData(event.currentTarget)
    const next = new URLSearchParams()
    const values = {
      search: form.get('search'),
      category: form.get('category'),
      minPrice: form.get('minPrice'),
      maxPrice: form.get('maxPrice'),
      sort: form.get('sort'),
      size: searchParams.get('size') || '12',
      page: '0',
    }
    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && String(value).trim() !== '') next.set(key, String(value).trim())
    })
    setSearchParams(next)
  }

  const setPage = (page) => {
    const next = new URLSearchParams(searchParams)
    next.set('page', String(page))
    setSearchParams(next)
  }

  const toggleCompare = (productId) => {
    setCompareError('')
    if (compareIds.includes(productId)) {
      setCompareIds(compareIds.filter((id) => id !== productId))
    } else if (compareIds.length >= 4) {
      setCompareError('Compare up to four products at a time.')
    } else {
      setCompareIds([...compareIds, productId])
    }
  }

  const currentCategory = searchParams.get('category') || ''
  const currentSort = searchParams.get('sort') || 'newest'
  const isLoading = loading || loadedQueryKey !== queryKey

  return (
    <main className="container py-4">
      <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div><p className="eyebrow mb-2">MarketHub marketplace</p><h1 className="mb-0">Discover products</h1></div>
        <span className="text-secondary">{pageInfo.totalElements} products</span>
      </div>

      <form className="card card-body mb-4" onSubmit={submitFilters}>
        <div className="row g-3 align-items-end">
          <div className="col-12 col-lg-4">
            <label htmlFor="catalog-search" className="form-label">Search</label>
            <input id="catalog-search" name="search" className="form-control" type="search" key={queryKey}
              defaultValue={searchParams.get('search') || ''} placeholder="Search products or brands" />
          </div>
          <div className="col-12 col-sm-6 col-lg-2">
            <label htmlFor="catalog-category" className="form-label">Category</label>
            <select id="catalog-category" name="category" className="form-select" defaultValue={currentCategory}>
              <option value="">All categories</option>
              {categories.map((category) => <option key={category.id} value={category.slug}>{category.name}</option>)}
            </select>
          </div>
          <div className="col-6 col-lg-1">
            <label htmlFor="catalog-min" className="form-label">Min price</label>
            <input id="catalog-min" name="minPrice" type="number" min="0" step="0.01" className="form-control" defaultValue={searchParams.get('minPrice') || ''} />
          </div>
          <div className="col-6 col-lg-1">
            <label htmlFor="catalog-max" className="form-label">Max price</label>
            <input id="catalog-max" name="maxPrice" type="number" min="0" step="0.01" className="form-control" defaultValue={searchParams.get('maxPrice') || ''} />
          </div>
          <div className="col-12 col-sm-6 col-lg-2">
            <label htmlFor="catalog-sort" className="form-label">Sort by</label>
            <select id="catalog-sort" name="sort" className="form-select" defaultValue={currentSort}>
              {SORTS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          <div className="col-12 col-sm-6 col-lg-2"><button className="btn btn-primary w-100" type="submit">Apply filters</button></div>
        </div>
      </form>

      {isLoading && <p role="status" className="alert alert-info">Loading products…</p>}
      {!isLoading && error && <div className="alert alert-warning" role="alert">{error}</div>}
      {compareError && <div className="alert alert-warning" role="alert">{compareError}</div>}
      {compareIds.length > 0 && <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 alert alert-light border">
        <span>{compareIds.length} product{compareIds.length === 1 ? '' : 's'} selected to compare</span>
        <div className="d-flex gap-2">
          {compareIds.length >= 2 && <Link className="btn btn-sm btn-primary" to={`/compare?ids=${compareIds.map(encodeURIComponent).join(',')}`}>Compare products</Link>}
          <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => setCompareIds([])}>Clear</button>
        </div>
      </div>}
      {!isLoading && !error && products.length === 0 && <div className="card card-body text-center py-5"><h2 className="h5">No products found</h2><p className="mb-0 text-secondary">Try a different search or remove some filters.</p></div>}

      {!isLoading && !error && products.length > 0 && <div className="row g-3">
        {products.map((product) => <div key={product.id} className="col-12 col-sm-6 col-lg-4 col-xl-3">
          <article className="card h-100">
            <Link to={`/products/${product.id}`} className="text-decoration-none text-reset">
              {product.imageUrl
                ? <img src={product.imageUrl} className="card-img-top" alt={product.imageAlt || product.name} style={{ height: 220, objectFit: 'cover' }} />
                : <div className="bg-body-secondary d-flex align-items-center justify-content-center" style={{ height: 220 }} aria-label="No product image available"><span className="text-secondary">Image unavailable</span></div>}
              <div className="card-body">
                {product.brand && <p className="small text-secondary mb-1">{product.brand}</p>}
                <h2 className="h6">{product.name}</h2>
                <p className="fw-bold mb-1">{formatPrice(product.price, product.currency)}</p>
                <p className="small text-secondary mb-0">{product.storeName ? `Sold by ${product.storeName}` : 'Marketplace seller'}</p>
              </div>
            </Link>
            <div className="card-footer bg-white border-0 pt-0">
              <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => toggleCompare(product.id)}>
                {compareIds.includes(product.id) ? 'Remove from compare' : 'Compare'}
              </button>
            </div>
          </article>
        </div>)}
      </div>}

      {!isLoading && pageInfo.totalPages > 1 && <nav className="d-flex justify-content-center align-items-center gap-3 mt-4" aria-label="Product pages">
        <button type="button" className="btn btn-outline-secondary" disabled={pageInfo.page <= 0} onClick={() => { setLoading(true); setPage(pageInfo.page - 1) }}>Previous</button>
        <span>Page {pageInfo.page + 1} of {pageInfo.totalPages}</span>
        <button type="button" className="btn btn-outline-secondary" disabled={pageInfo.page + 1 >= pageInfo.totalPages} onClick={() => { setLoading(true); setPage(pageInfo.page + 1) }}>Next</button>
      </nav>}
    </main>
  )
}

export default Catalog
