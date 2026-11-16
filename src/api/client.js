import axios from 'axios'
import { clearAccessToken, getAccessToken, setAccessToken } from './accessToken'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  headers: {
    'Content-Type': 'application/json',
  },
})

let refreshRequest = null

export const restoreSession = async () => {
  if (!refreshRequest) {
    refreshRequest = apiClient.post('/auth/refresh')
      .then(({ data }) => {
        setAccessToken(data.accessToken)
        return data.accessToken
      })
      .finally(() => {
        refreshRequest = null
      })
  }
  return refreshRequest
}

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config
    const isAuthRequest = request?.url?.includes('/auth/')
    if (error.response?.status !== 401 || !request || request._marketHubRetried || isAuthRequest) {
      return Promise.reject(error)
    }

    request._marketHubRetried = true
    try {
      const token = await restoreSession()
      request.headers.Authorization = `Bearer ${token}`
      return apiClient(request)
    } catch {
      clearAccessToken()
      return Promise.reject(error)
    }
  },
)

export default apiClient
