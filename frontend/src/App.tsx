import { Box, Center, Container, Spinner, VStack } from '@chakra-ui/react'
import { useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { ChatPage } from './pages/ChatPage'
import { LoginPage } from './pages/LoginPage'
import { OnboardingPage } from './pages/OnboardingPage'
import './App.css'
import { useAuthStore } from './store/auth'
import { InitApi } from './api/init/init'
import type { AuthResponse } from './types'

function App() {
  const [loading, setLoading] = useState(true)
  const [initialized, setInitialized] = useState(false)
  const setAuth = useAuthStore((state) => state.setAuth)
  const accessToken = useAuthStore((state) => state.accessToken)
  const username = useAuthStore((state) => state.username)
  const navigate = useNavigate()

  useEffect(() => {
    const check = async () => {
      try {
        const res = await InitApi.getStatus()
        setInitialized(res.initialized)
      } finally {
        setLoading(false)
      }
    }
    check()
  }, [])

  const handleAuth = (auth: AuthResponse) => {
    setAuth(auth)
    setInitialized(true)
    navigate('/')
  }

  if (loading) {
    return (
      <Center minH="100vh">
        <Spinner size="xl" color="teal.300" />
      </Center>
    )
  }

  return (
    <Routes>
      {!initialized ? (
        <Route
          path="*"
          element={
            <Container maxW="100vw" px={{ base: 4, md: 8 }}>
              <OnboardingPage onComplete={handleAuth} />
            </Container>
          }
        />
      ) : (
        <>
          <Route
            path="/login"
            element={
              <Container maxW="lg" px={{ base: 4, md: 8 }}>
                <LoginPage onAuth={handleAuth} />
              </Container>
            }
          />
          <Route
            path="/"
            element={
              <Box minH="100vh" px={{ base: 4, md: 8 }} py={{ base: 8, md: 12 }}>
                {accessToken ? (
                  <VStack align="stretch">
                    <ChatPage user={username} />
                  </VStack>
                ) : (
                  <Navigate to="/login" replace />
                )}
              </Box>
            }
          />
          <Route path="*" element={<Navigate to={accessToken ? '/' : '/login'} replace />} />
        </>
      )}
    </Routes>
  )
}

export default App
