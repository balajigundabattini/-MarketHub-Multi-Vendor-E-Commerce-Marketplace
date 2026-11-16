import apiClient from './client'

export const fetchPayment = async (paymentId) => (await apiClient.get(`/payments/${encodeURIComponent(paymentId)}`)).data
export const retryPayment = async (paymentId) => (await apiClient.post(`/payments/${encodeURIComponent(paymentId)}/retry`)).data
