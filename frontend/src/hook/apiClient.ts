import axios, { type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/auth'
import type { AuthResponse } from '../types'

export const apiClient = axios.create({
  baseURL: '/api',
})

let refreshPromise: Promise<AuthResponse | undefined> | null = null

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as (AxiosRequestConfig & { _retry?: boolean }) | undefined
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true
      const refreshed = await refreshTokens()
      if (refreshed) {
        const retryConfig: AxiosRequestConfig = {
          ...original,
          headers: { ...(original.headers || {}), Authorization: `Bearer ${refreshed.accessToken}` },
        }
        return apiClient(retryConfig)
      }
    }
    return Promise.reject(error)
  },
)

async function refreshTokens(): Promise<AuthResponse | undefined> {
  if (refreshPromise) return refreshPromise
  refreshPromise = new Promise(async (resolve) => {
    const refreshToken = useAuthStore.getState().refreshToken
    if (!refreshToken) {
      resolve(undefined)
      refreshPromise = null
      return
    }
    try {
      const res = await axios.post<AuthResponse>('/api/auth/refresh', { refreshToken })
      useAuthStore.getState().setAuth(res.data)
      resolve(res.data)
    } catch (err) {
      useAuthStore.getState().clear()
      resolve(undefined)
    } finally {
      refreshPromise = null
    }
  })
  return refreshPromise
}
