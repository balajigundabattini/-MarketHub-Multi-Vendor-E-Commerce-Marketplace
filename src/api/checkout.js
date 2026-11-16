import apiClient from './client'

export const requestCheckoutQuote = async (input) =>
  (await apiClient.post('/checkout/quote', input)).data

export const placeOrder = async (input) =>
  (await apiClient.post('/checkout/orders', input)).data
