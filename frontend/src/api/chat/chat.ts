import { apiClient } from '../../hook/apiClient'
import type { ChatResponse } from '../../types'

export const ChatApi = {
  ask: async (payload: { dbId: number; message: string; sessionId?: number }) => {
    const res = await apiClient.post<ChatResponse>('/chat/ask', payload)
    return res.data
  },
  history: async (sessionId: number) => {
    const res = await apiClient.get<ChatResponse>(`/chat/history/${sessionId}`)
    return res.data
  },
}
