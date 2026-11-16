import { NavLink, Outlet } from 'react-router-dom'

const navItems = [
  ['/seller', 'Overview', true], ['/seller/store', 'Store'], ['/seller/products', 'Products'],
  ['/seller/inventory', 'Inventory'], ['/seller/orders', 'Orders'], ['/seller/reports', 'Sales & commission'],
]

const SellerLayout = () => (
  <div className="container-fluid py-4">
    <div className="row g-4">
      <aside className="col-12 col-lg-3 col-xl-2">
        <h1 className="h4">Seller workspace</h1>
        <nav className="nav nav-pills flex-row flex-lg-column gap-1" aria-label="Seller navigation">
          {navItems.map(([to, label, end]) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>{label}</NavLink>)}
        </nav>
      </aside>
      <div className="col-12 col-lg-9 col-xl-10"><Outlet /></div>
    </div>
  </div>
)

export default SellerLayout
