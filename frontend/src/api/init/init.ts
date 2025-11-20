import { apiClient } from '../../hook/apiClient'
import type { InitSetupResponse, InitStatusResponse } from '../../types'
import type { DbConnectionRequest } from '../../types'

export const InitApi = {
  getStatus: async () => {
    const res = await apiClient.get<InitStatusResponse>('/init/status')
    return res.data
  },
  setup: async (payload: { admin: { username: string; password: string }; database?: DbConnectionRequest | null }) => {
    const res = await apiClient.post<InitSetupResponse>('/init/setup', payload)
    return res.data
  },
}
