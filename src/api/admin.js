import apiClient from './client'

export const fetchAdminDashboard = async () => (await apiClient.get('/admin/dashboard')).data
export const fetchAdminCustomers = async (params) => (await apiClient.get('/admin/customers', { params })).data
export const updateAdminCustomerStatus = async (id, status) => (await apiClient.patch(`/admin/customers/${encodeURIComponent(id)}/status`, { status })).data
export const fetchAdminSellerApplications = async (params) => (await apiClient.get('/admin/seller-applications', { params })).data
export const reviewSellerApplication = async (id, status, reason) => (await apiClient.patch(`/admin/seller-applications/${encodeURIComponent(id)}/decision`, { status, reason })).data
export const fetchAdminCategories = async () => (await apiClient.get('/admin/categories')).data
export const saveAdminCategory = async (id, request) => id ? (await apiClient.put(`/admin/categories/${encodeURIComponent(id)}`, request)).data : (await apiClient.post('/admin/categories', request)).data
export const fetchAdminProducts = async (params) => (await apiClient.get('/admin/products', { params })).data
export const moderateAdminProduct = async (id, status, reason) => (await apiClient.patch(`/admin/products/${encodeURIComponent(id)}/moderation`, { status, reason })).data
export const fetchAdminOrders = async (params) => (await apiClient.get('/admin/orders', { params })).data
export const refundAdminOrder = async (id, request) => (await apiClient.post(`/admin/orders/${encodeURIComponent(id)}/refunds`, request)).data
export const fetchAdminCoupons = async () => (await apiClient.get('/admin/coupons')).data
export const saveAdminCoupon = async (id, request) => id ? (await apiClient.put(`/admin/coupons/${encodeURIComponent(id)}`, request)).data : (await apiClient.post('/admin/coupons', request)).data
export const fetchAdminReports = async (params) => (await apiClient.get('/admin/reports', { params })).data
