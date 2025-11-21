import {
  Box,
  Button,
  Center,
  Container,
  Heading,
  Spinner,
  Text,
  VStack,
} from "@chakra-ui/react";
import { isAxiosError } from "axios";
import { useEffect, useState } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { ChatPage } from "./pages/ChatPage";
import { LoginPage } from "./pages/LoginPage";
import { OnboardingPage } from "./pages/OnboardingPage";
import "./App.css";
import { useAuthStore } from "./store/auth";
import { InitApi } from "./api/init/init";
import type { AuthResponse } from "./types";
import { extractErrorMessage } from "./utils/error";

function App() {
  const [loading, setLoading] = useState(true);
  const [initialized, setInitialized] = useState(false);
  const [serverDown, setServerDown] = useState(false);
  const [serverErrorMessage, setServerErrorMessage] = useState<string | null>(
    null,
  );
  const setAuth = useAuthStore((state) => state.setAuth);
  const accessToken = useAuthStore((state) => state.accessToken);
  const username = useAuthStore((state) => state.username);
  const navigate = useNavigate();

  useEffect(() => {
    const check = async () => {
      try {
        const res = await InitApi.getStatus();
        setInitialized(res.initialized);
        setServerDown(false);
        setServerErrorMessage(null);
      } catch (err) {
        const is503 = isAxiosError(err) && err.response?.status === 502;
        setServerDown(is503);
        setServerErrorMessage(extractErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    check();
  }, []);

  const handleAuth = (auth: AuthResponse) => {
    setAuth(auth);
    setInitialized(true);
    navigate("/");
  };

  if (loading) {
    return (
      <Center minH="100vh">
        <Spinner size="xl" color="teal.300" />
      </Center>
    );
  }

  if (serverDown) {
    return (
      <Center minH="100vh" px={{ base: 6, md: 12 }}>
        <VStack spacing={4} maxW="3xl" textAlign="center">
          <Heading size="lg">서버가 작동하지 않습니다</Heading>
          <Text color="gray.200">
            잠시 후 다시 시도하거나 서버 상태를 확인해주세요.
          </Text>
          {serverErrorMessage && (
            <Text color="gray.400" fontSize="sm">
              상세: {serverErrorMessage}
            </Text>
          )}
          <Button colorScheme="teal" onClick={() => window.location.reload()}>
            다시 시도
          </Button>
        </VStack>
      </Center>
    );
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
              <Box
                minH="100vh"
                px={{ base: 4, md: 8 }}
                py={{ base: 8, md: 12 }}
              >
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
          <Route
            path="*"
            element={<Navigate to={accessToken ? "/" : "/login"} replace />}
          />
        </>
      )}
    </Routes>
  );
}

export default App;
