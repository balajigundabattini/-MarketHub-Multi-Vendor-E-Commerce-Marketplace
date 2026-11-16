import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { addToCart, fetchWishlist, removeFromWishlist } from '../api/shopping'

const Wishlist = () => {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    fetchWishlist().then((data) => {
      if (active) { setItems(data.items || []); setLoading(false) }
    }).catch((error) => {
      if (active) {
        setMessage(error.response?.data?.detail || 'Your wishlist could not be loaded.')
        setLoading(false)
      }
    })
    return () => { active = false }
  }, [])

  const remove = async (item) => {
    setBusyId(item.productId)
    setMessage('')
    try {
      await removeFromWishlist(item.productId)
      setItems((current) => current.filter((saved) => saved.productId !== item.productId))
    } catch (error) {
      setMessage(error.response?.data?.detail || 'The item could not be removed.')
    } finally {
      setBusyId(null)
    }
  }

  const moveToCart = async (item) => {
    setBusyId(item.productId)
    setMessage('')
    try {
      await addToCart(item.productId)
      setMessage(`${item.name} was added to your cart.`)
    } catch (error) {
      setMessage(error.response?.data?.detail || 'This item could not be moved to your cart.')
    } finally {
      setBusyId(null)
    }
  }

  if (loading) return <main className="container py-5"><p role="status">Loading your wishlist…</p></main>
  return (
    <main className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4"><h1 className="h2">Your wishlist</h1><Link to="/products">Browse products</Link></div>
      {message && <p className="alert alert-info" role="status">{message}</p>}
      {items.length === 0 ? <div className="card card-body text-center py-5"><h2 className="h5">No saved products yet</h2><p className="text-secondary">Save products you want to find later.</p><Link className="btn btn-primary align-self-center" to="/products">Discover products</Link></div> :
        <div className="row g-3">
          {items.map((item) => <article className="col-12 col-sm-6 col-lg-4 col-xl-3" key={item.productId}>
            <div className="card h-100">
              <Link to={`/products/${item.productId}`} className="text-decoration-none text-reset">
                {item.imageUrl && <img src={item.imageUrl} className="card-img-top" alt={item.imageAlt || item.name} style={{ height: 220, objectFit: 'cover' }} />}
                <div className="card-body"><h2 className="h6">{item.name}</h2><p className="fw-semibold mb-0">{new Intl.NumberFormat(undefined, { style: 'currency', currency: item.currency || 'INR' }).format(item.price)}</p></div>
              </Link>
              <div className="card-footer bg-white d-flex gap-2">
                <button className="btn btn-sm btn-primary" type="button" disabled={busyId === item.productId} onClick={() => moveToCart(item)}>Add to cart</button>
                <button className="btn btn-sm btn-outline-danger" type="button" disabled={busyId === item.productId} onClick={() => remove(item)}>Remove</button>
              </div>
            </div>
          </article>)}
        </div>}
    </main>
  )
}

export default Wishlist
