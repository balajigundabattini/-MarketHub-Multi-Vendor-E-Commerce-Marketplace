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
      <Route path="/forbidden" element={<main className="container py-5"><h1>Access denied</h1><p>Your account is not permitted to open this page.</p></main>} />
      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default Routings
