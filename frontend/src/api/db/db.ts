import { apiClient } from '../../hook/apiClient'
import type { DbConnectionRequest, DbSummary, DbTestResponse, ExecuteResponse } from '../../types'

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
  delete: async (id: number) => {
    await apiClient.delete(`/db/${id}`)
  },
  execute: async (payload: { dbId: number; sql: string }) => {
    const res = await apiClient.post<ExecuteResponse>('/db/execute', payload)
    return res.data
  },
  refresh: async (id: number) => {
    const res = await apiClient.put<DbSummary>(`/db/refresh/${id}`)
    return res.data
  },
}
