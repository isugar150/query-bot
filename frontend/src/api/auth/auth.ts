import { apiClient } from '../../hook/apiClient'
import type { AuthResponse } from '../../types'

export const AuthApi = {
  login: async (username: string, password: string) => {
    const res = await apiClient.post<AuthResponse>('/auth/login', { username, password })
    return res.data
  },
}
