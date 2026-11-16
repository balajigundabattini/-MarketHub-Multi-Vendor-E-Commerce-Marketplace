import apiClient from './client'

export const fetchOrders = async (params) => (await apiClient.get('/orders', { params })).data
export const fetchOrder = async (orderId) => (await apiClient.get(`/orders/${encodeURIComponent(orderId)}`)).data
export const cancelOrderItem = async (orderId, itemId, reason) =>
  apiClient.post(`/orders/${encodeURIComponent(orderId)}/items/${encodeURIComponent(itemId)}/cancel`, { reason })
export const requestReturn = async (orderId, itemId, reason) =>
  apiClient.post(`/orders/${encodeURIComponent(orderId)}/items/${encodeURIComponent(itemId)}/returns`, { reason })
export const submitReview = async (productId, review) =>
  apiClient.post(`/products/${encodeURIComponent(productId)}/reviews`, review)
