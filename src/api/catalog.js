import apiClient from './client'

export const fetchProducts = async (params) => {
  const { data } = await apiClient.get('/catalog/products', { params })
  return data
}

export const fetchProduct = async (productId) => {
  const { data } = await apiClient.get(`/catalog/products/${encodeURIComponent(productId)}`)
  return data
}

export const fetchProductReviews = async (productId, params) => {
  const { data } = await apiClient.get(`/catalog/products/${encodeURIComponent(productId)}/reviews`, { params })
  return data
}

export const fetchCategories = async () => {
  const { data } = await apiClient.get('/catalog/categories')
  return data
}
