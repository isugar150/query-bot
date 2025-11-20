import { apiClient } from '../../hook/apiClient'
import type { DbConnectionRequest, DbSummary, DbTestResponse } from '../../types'

export const DbApi = {
  testConnection: async (payload: DbConnectionRequest) => {
    const res = await apiClient.post<DbTestResponse>('/db/test', payload)
    return res.data
  },
  register: async (payload: DbConnectionRequest) => {
    const res = await apiClient.post<DbSummary>('/db/register', payload)
    return res.data
  },
  list: async () => {
    const res = await apiClient.get<DbSummary[]>('/db/list')
    return res.data
  },
}
