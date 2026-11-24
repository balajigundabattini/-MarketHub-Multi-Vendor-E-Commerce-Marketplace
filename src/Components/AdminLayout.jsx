import { NavLink, Outlet } from 'react-router-dom'

const items = [
  ['/admin', 'Overview', true], ['/admin/customers', 'Customers'], ['/admin/sellers', 'Seller approvals'],
  ['/admin/categories', 'Categories'], ['/admin/products', 'Product moderation'], ['/admin/reviews', 'Review moderation'], ['/admin/orders', 'Orders & refunds'],
  ['/admin/coupons', 'Coupons'], ['/admin/reports', 'Reports'], ['/admin/settlements', 'Settlements'],
]

const AdminLayout = () => <div className="container-fluid py-4"><div className="row g-4">
  <aside className="col-12 col-lg-3 col-xl-2"><h1 className="h4">Admin console</h1><nav className="nav nav-pills flex-row flex-lg-column gap-1" aria-label="Admin navigation">
    {items.map(([to, label, end]) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>{label}</NavLink>)}
  </nav></aside><div className="col-12 col-lg-9 col-xl-10"><Outlet /></div>
</div></div>

export default AdminLayout
