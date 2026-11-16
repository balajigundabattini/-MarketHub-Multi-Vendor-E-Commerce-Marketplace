import apiClient from './client'

export const fetchNotifications = async (params) => (await apiClient.get('/notifications', { params })).data
export const markNotificationRead = async (id) => apiClient.put(`/notifications/${encodeURIComponent(id)}/read`)
export const markAllNotificationsRead = async () => apiClient.put('/notifications/read-all')
