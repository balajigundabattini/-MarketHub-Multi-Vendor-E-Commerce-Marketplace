import apiClient from './client'

export const fetchPayment = async (paymentId) => (await apiClient.get(`/payments/${encodeURIComponent(paymentId)}`)).data
export const retryPayment = async (paymentId) => (await apiClient.post(`/payments/${encodeURIComponent(paymentId)}/retry`)).data
export const verifyPayment = async (paymentId, response) => (await apiClient.post(`/payments/${encodeURIComponent(paymentId)}/verify`, {
  razorpayPaymentId: response.razorpay_payment_id,
  razorpaySignature: response.razorpay_signature,
})).data
