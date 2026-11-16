import apiClient from './client'

export const fetchSellerApplication = async () => (await apiClient.get('/seller-applications/me')).data
export const submitSellerApplication = async (request) => (await apiClient.post('/seller-applications', request)).data
export const fetchSellerDashboard = async () => (await apiClient.get('/seller/dashboard')).data
export const fetchStore = async () => (await apiClient.get('/seller/store')).data
export const updateStore = async (request) => (await apiClient.put('/seller/store', request)).data
export const fetchSellerProducts = async (params) => (await apiClient.get('/seller/products', { params })).data
export const fetchSellerProduct = async (id) => (await apiClient.get(`/seller/products/${encodeURIComponent(id)}`)).data
export const createSellerProduct = async (request) => (await apiClient.post('/seller/products', request)).data
export const updateSellerProduct = async (id, request) => (await apiClient.put(`/seller/products/${encodeURIComponent(id)}`, request)).data
export const deactivateSellerProduct = async (id) => apiClient.post(`/seller/products/${encodeURIComponent(id)}/deactivate`)
export const fetchSellerInventory = async () => (await apiClient.get('/seller/inventory')).data
export const adjustSellerInventory = async (id, request) => (await apiClient.patch(`/seller/inventory/${encodeURIComponent(id)}`, request)).data
export const fetchSellerOrders = async (params) => (await apiClient.get('/seller/orders', { params })).data
export const updateSellerOrderStatus = async (id, status) => apiClient.patch(`/seller/orders/${encodeURIComponent(id)}/status`, { status })
export const createShipment = async (id, request) => apiClient.post(`/seller/orders/${encodeURIComponent(id)}/shipments`, request)
export const fetchSellerReports = async (params) => (await apiClient.get('/seller/reports', { params })).data
