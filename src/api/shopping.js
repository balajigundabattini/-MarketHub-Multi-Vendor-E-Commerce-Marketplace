import apiClient from './client'

export const fetchCart = async () => (await apiClient.get('/cart')).data
export const updateCartItem = async (itemId, quantity) =>
  (await apiClient.put(`/cart/items/${encodeURIComponent(itemId)}`, { quantity })).data
export const removeCartItem = async (itemId) =>
  (await apiClient.delete(`/cart/items/${encodeURIComponent(itemId)}`)).data
export const addToCart = async (productId, quantity = 1) =>
  (await apiClient.post('/cart/items', { productId, quantity })).data

export const fetchWishlist = async () => (await apiClient.get('/wishlist')).data
export const addToWishlist = async (productId) =>
  (await apiClient.post('/wishlist/items', { productId })).data
export const removeFromWishlist = async (productId) =>
  apiClient.delete(`/wishlist/items/${encodeURIComponent(productId)}`)
