import {
  Box,
  Button,
  Card,
  CardBody,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Stack,
  Text,
  useToast,
} from '@chakra-ui/react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthApi } from '../api/auth/auth'
import type { AuthResponse } from '../types'

type Props = {
  onAuth: (auth: AuthResponse) => void
}

export function LoginPage({ onAuth }: Props) {
  const toast = useToast()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    setLoading(true)
    try {
      const res = await AuthApi.login(username, password)
      onAuth(res)
      toast({ title: '로그인 성공', status: 'success' })
      navigate('/')
    } catch (err: any) {
      toast({ title: '로그인 실패', description: err?.response?.data ?? '아이디/비밀번호를 확인하세요.', status: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box maxW="md" mx="auto" pt={16}>
      <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
        <CardBody>
          <Stack spacing={5}>
            <Heading size="lg">관리자 로그인</Heading>
            <Text color="gray.300">초기 설정에서 만든 아이디와 비밀번호를 입력하세요.</Text>
            <FormControl>
              <FormLabel>아이디</FormLabel>
              <Input value={username} onChange={(e) => setUsername(e.target.value)} />
            </FormControl>
            <FormControl>
              <FormLabel>비밀번호</FormLabel>
              <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </FormControl>
            <Button colorScheme="teal" onClick={submit} isLoading={loading} isDisabled={!username || !password}>
              로그인
            </Button>
          </Stack>
        </CardBody>
      </Card>
    </Box>
  )
}
