import { apiClient } from '../hook/apiClient'
import type { MetabaseQuestionResponse, MetabaseStatus } from '../types'

export const MetabaseApi = {
  status: async () => {
    const res = await apiClient.get<MetabaseStatus>('/metabase/status')
    return res.data
  },
  sendQuery: async (payload: { sessionId: number; query: string; title?: string; description?: string }) => {
    const res = await apiClient.post<MetabaseQuestionResponse>('/metabase/card', payload)
    return res.data
  },
}
