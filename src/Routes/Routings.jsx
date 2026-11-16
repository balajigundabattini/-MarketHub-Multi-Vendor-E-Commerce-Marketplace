import { Navigate, Route, Routes } from 'react-router-dom'
import Register from '../Pages/Register'
import Login from '../Pages/Login'
import Home from '../Pages/Home'
import VerifyEmail from '../Pages/VerifyEmail'
import ForgotPassword from '../Pages/ForgotPassword'
import ResetPassword from '../Pages/ResetPassword'
import Account from '../Pages/Account'
import ProtectedRoute from '../auth/ProtectedRoute'
import Catalog from '../Pages/Catalog'
import ProductDetails from '../Pages/ProductDetails'
import ProductCompare from '../Pages/ProductCompare'
import Cart from '../Pages/Cart'
import Wishlist from '../Pages/Wishlist'
import Checkout from '../Pages/Checkout'
import OrderHistory from '../Pages/OrderHistory'
import OrderDetails from '../Pages/OrderDetails'
import Notifications from '../Pages/Notifications'
import PaymentStatus from '../Pages/PaymentStatus'

const Routings = () => {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/products" element={<Catalog />} />
      <Route path="/products/:productId" element={<ProductDetails />} />
      <Route path="/compare" element={<ProductCompare />} />
      <Route path="/register" element={<Register />} />
      <Route path="/login" element={<Login />} />
      <Route path="/verify-email" element={<VerifyEmail />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/account" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><Account /></ProtectedRoute>} />
      <Route path="/cart" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><Cart /></ProtectedRoute>} />
      <Route path="/wishlist" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><Wishlist /></ProtectedRoute>} />
      <Route path="/checkout" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><Checkout /></ProtectedRoute>} />
      <Route path="/orders" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><OrderHistory /></ProtectedRoute>} />
      <Route path="/orders/:orderId" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><OrderDetails /></ProtectedRoute>} />
      <Route path="/payments/:paymentId" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><PaymentStatus /></ProtectedRoute>} />
      <Route path="/notifications" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><Notifications /></ProtectedRoute>} />
      <Route path="/forbidden" element={<main className="container py-5"><h1>Access denied</h1><p>Your account is not permitted to open this page.</p></main>} />
      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default Routings
