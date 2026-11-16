import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { useAuth } from '../auth/useAuth'

const Header = () => {
  const { user, loading, signOut } = useAuth()
  const navigate = useNavigate()
  const roles = user?.roles || []

  const logout = async () => {
    try {
      await signOut()
      navigate('/', { replace: true })
    } catch {
      toast.error('Could not sign out. Please try again.')
    }
  }

  return (
    <header className="header">
      <div className="header-content">
        <Link className="brand" to="/" aria-label="MarketHub home">
          <span className="brand-mark" aria-hidden="true">M</span>
          <span>MarketHub</span>
        </Link>
        <nav className="header-actions" aria-label="Main navigation">
          <Link to="/products">Shop</Link>
          {roles.includes('CUSTOMER') && <Link to="/account">Account</Link>}
          {roles.includes('CUSTOMER') && <Link to="/seller/apply">Sell on MarketHub</Link>}
          {roles.includes('CUSTOMER') && <Link to="/wishlist">Wishlist</Link>}
          {roles.includes('CUSTOMER') && <Link to="/cart">Cart</Link>}
          {roles.includes('CUSTOMER') && <Link to="/orders">Orders</Link>}
          {roles.includes('CUSTOMER') && <Link to="/notifications">Notifications</Link>}
          {roles.includes('SELLER') && <Link to="/seller">Seller workspace</Link>}
          {roles.includes('ADMIN') && <Link to="/admin">Admin console</Link>}
          {!loading && !user && <Link to="/login">Sign in</Link>}
          {!loading && !user && <Link className="header-register" to="/register">Join MarketHub</Link>}
          {user && <span aria-label="Signed in user">{user.displayName}</span>}
          {user && <button className="btn btn-sm btn-outline-secondary" type="button" onClick={logout}>Sign out</button>}
        </nav>
      </div>
    </header>
  )
}

export default Header
