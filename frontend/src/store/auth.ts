import { create } from 'zustand'
import type { AuthResponse } from '../types'

type AuthState = {
  username?: string
  accessToken?: string
  refreshToken?: string
  setAuth: (auth: AuthResponse) => void
  clear: () => void
}

const persisted = (() => {
  if (typeof localStorage === 'undefined') return {}
  const raw = localStorage.getItem('querybot_auth')
  if (!raw) return {}
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
})()

export const useAuthStore = create<AuthState>((set) => ({
  username: persisted.username,
  accessToken: persisted.accessToken,
  refreshToken: persisted.refreshToken,
  setAuth: (auth) => {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(
        'querybot_auth',
        JSON.stringify({
          username: auth.username,
          accessToken: auth.accessToken,
          refreshToken: auth.refreshToken,
        }),
      )
    }
    set({
      username: auth.username,
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
    })
  },
  clear: () => {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('querybot_auth')
    }
    set({ username: undefined, accessToken: undefined, refreshToken: undefined })
  },
}))
