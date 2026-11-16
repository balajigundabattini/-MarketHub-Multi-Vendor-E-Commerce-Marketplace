import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { fetchProduct } from '../api/catalog'

const formatPrice = (price, currency) => new Intl.NumberFormat(undefined, {
  style: 'currency', currency: currency || 'INR',
}).format(price)

const ProductDetails = () => {
  const { productId } = useParams()
  const [product, setProduct] = useState(null)
  const [selectedImage, setSelectedImage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadedProductId, setLoadedProductId] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchProduct(productId)
      .then((data) => {
        if (active) {
          setProduct(data)
          setSelectedImage(data.images?.[0]?.url || null)
          setLoadedProductId(productId)
          setLoading(false)
        }
      })
      .catch((requestError) => {
        if (active) {
          setError(requestError.response?.data?.detail || 'This product could not be loaded.')
          setLoadedProductId(productId)
          setLoading(false)
        }
      })
    return () => { active = false }
  }, [productId])

  if (loading || loadedProductId !== productId) return <main className="container py-5"><p role="status">Loading product…</p></main>
  if (error || !product) return <main className="container py-5"><div className="alert alert-warning" role="alert">{error || 'Product not found.'}</div><Link to="/products">Back to products</Link></main>

  const images = product.images || []
  return (
    <main className="container py-4">
      <p><Link to="/products">← Back to products</Link></p>
      <div className="row g-4 mt-1">
        <div className="col-12 col-lg-6">
          {images.length > 0
            ? <>
              <img src={selectedImage || images[0].url} alt={images.find((image) => image.url === selectedImage)?.altText || product.name} className="img-fluid rounded w-100" />
              {images.length > 1 && <div className="d-flex flex-wrap gap-2 mt-3" aria-label="Product images">
                {images.map((image) => <button key={image.url} type="button" className={`btn p-1 ${image.url === selectedImage ? 'border-primary' : 'border'}`} onClick={() => setSelectedImage(image.url)} aria-label={`Show ${image.altText || product.name}`} aria-pressed={image.url === selectedImage}>
                  <img src={image.url} alt="" width="64" height="64" style={{ objectFit: 'cover' }} />
                </button>)}
              </div>}
            </>
            : <div className="bg-body-secondary rounded d-grid place-items-center" style={{ minHeight: 360 }}><span className="text-secondary">Image unavailable</span></div>}
        </div>
        <div className="col-12 col-lg-6">
          {product.categoryName && <p className="eyebrow">{product.categoryName}</p>}
          <h1>{product.name}</h1>
          {product.brand && <p className="text-secondary">{product.brand}</p>}
          <p className="h3 my-4">{formatPrice(product.price, product.currency)}</p>
          <p className="text-secondary">Sold by {product.storeName || 'MarketHub seller'}</p>
          <div className="border-top pt-4 mt-4"><h2 className="h5">Product details</h2><p style={{ whiteSpace: 'pre-line' }}>{product.description}</p></div>
          {product.specifications?.length > 0 && <dl className="row mt-4">
            {product.specifications.map((item) => <div className="row" key={item.name}><dt className="col-5">{item.name}</dt><dd className="col-7">{item.value}</dd></div>)}
          </dl>}
          <p className="small text-secondary">Availability and purchase options will appear here when the cart and inventory services are connected.</p>
        </div>
      </div>
    </main>
  )
}

export default ProductDetails
