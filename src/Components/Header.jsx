import { Link } from 'react-router-dom'

const Header = () => (
  <header className="header">
    <div className="header-content">
      <Link className="brand" to="/" aria-label="MarketHub home">
        <span className="brand-mark" aria-hidden="true">M</span>
        <span>MarketHub</span>
      </Link>
      <nav className="header-actions" aria-label="Main navigation">
        <Link to="/login">Sign in</Link>
        <Link className="header-register" to="/register">Join MarketHub</Link>
      </nav>
    </div>
  </header>
)

export default Header
