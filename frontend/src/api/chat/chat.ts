import { apiClient } from '../../hook/apiClient'
import type { ChatResponse, ChatSession } from '../../types'

export const ChatApi = {
  ask: async (payload: { dbId: number; message: string; sessionId?: number }) => {
    const res = await apiClient.post<ChatResponse>('/chat/ask', payload)
    return res.data
  },
  history: async (sessionId: number) => {
    const res = await apiClient.get<ChatResponse>(`/chat/history/${sessionId}`)
    return res.data
  },
  latest: async (dbId: number) => {
    const res = await apiClient.get<ChatResponse>(`/chat/latest`, { params: { dbId } })
    return res.data
  },
  sessions: async (dbId: number) => {
    const res = await apiClient.get<ChatSession[]>(`/chat/sessions`, { params: { dbId } })
    return res.data
  },
  createSession: async (payload: { dbId: number; title: string }) => {
    const res = await apiClient.post<ChatSession>('/chat/session', payload)
    return res.data
  },
  deleteSession: async (sessionId: number) => {
    await apiClient.delete(`/chat/session/${sessionId}`)
  },
}
