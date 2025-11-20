import {
  Badge,
  Box,
  Button,
  Card,
  CardBody,
  Divider,
  Flex,
  FormControl,
  FormLabel,
  Grid,
  GridItem,
  HStack,
  Heading,
  Input,
  Select,
  Stack,
  Tag,
  TagLabel,
  Text,
  Textarea,
  useDisclosure,
  useToast,
  VStack,
} from '@chakra-ui/react'
import { FiArrowRight, FiPlus, FiRefreshCw } from 'react-icons/fi'
import { useEffect, useMemo, useState } from 'react'
import { ChatApi } from '../api/chat/chat'
import { DbApi } from '../api/db/db'
import { useAuthStore } from '../store/auth'
import type { ChatMessage, DbConnectionRequest, DbSummary, DbTestResponse } from '../types'

const emptyDbForm: DbConnectionRequest = {
  name: '신규 데이터베이스',
  dbType: 'POSTGRESQL',
  host: 'localhost',
  port: 5432,
  databaseName: 'app',
  username: 'postgres',
  password: 'postgres',
}

type Props = {
  user?: string
}

export function ChatPage({ user }: Props) {
  const toast = useToast()
  const logout = useAuthStore((state) => state.clear)
  const [databases, setDatabases] = useState<DbSummary[]>([])
  const [selectedDb, setSelectedDb] = useState<number | undefined>()
  const [sessionId, setSessionId] = useState<number | undefined>()
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)

  const { isOpen, onToggle, onClose } = useDisclosure()
  const [dbForm, setDbForm] = useState<DbConnectionRequest>(emptyDbForm)
  const [dbTesting, setDbTesting] = useState(false)
  const [dbTestResult, setDbTestResult] = useState<DbTestResponse | null>(null)
  const [dbSaving, setDbSaving] = useState(false)

  const selectedDbName = useMemo(() => databases.find((d) => d.id === selectedDb)?.name, [databases, selectedDb])

  useEffect(() => {
    const fetchDbs = async () => {
      try {
        const res = await DbApi.list()
        setDatabases(res)
        if (!selectedDb && res.length > 0) {
          setSelectedDb(res[0].id)
        }
      } catch (err: any) {
        toast({ title: 'DB 목록을 불러오지 못했습니다', description: err?.response?.data?.message ?? err.message, status: 'error' })
      }
    }
    fetchDbs()
  }, [])

  const handleSend = async () => {
    if (!input.trim() || !selectedDb) {
      toast({ title: '데이터베이스와 질문을 확인하세요.', status: 'warning' })
      return
    }
    const selected = databases.find((d) => d.id === selectedDb)
    if (selected && !selected.schemaReady) {
      toast({ title: '스키마 수집 중입니다.', description: '스키마가 준비될 때까지 잠시만 기다려주세요.', status: 'info' })
      return
    }
    const optimistic: ChatMessage = { role: 'USER', content: input, createdAt: new Date().toISOString() }
    setMessages((prev) => [...prev, optimistic])
    setSending(true)
    try {
      const res = await ChatApi.ask({ dbId: selectedDb, message: input, sessionId })
      setMessages(res.history)
      setSessionId(res.sessionId)
      setInput('')
    } catch (err: any) {
      toast({ title: '질문 전송 실패', description: err?.response?.data?.message ?? err.message, status: 'error' })
    } finally {
      setSending(false)
    }
  }

  const newSession = () => {
    setSessionId(undefined)
    setMessages([])
  }

  const formatDate = (val: string) => new Date(val).toLocaleTimeString()

  const handleDbTest = async () => {
    setDbTesting(true)
    setDbTestResult(null)
    try {
      const res = await DbApi.testConnection(dbForm)
      setDbTestResult(res)
      toast({ title: res.success ? 'DB 연결 성공' : 'DB 연결 실패', description: res.message, status: res.success ? 'success' : 'error' })
    } catch (err: any) {
      toast({ title: '테스트 실패', description: err?.response?.data?.message ?? err.message, status: 'error' })
    } finally {
      setDbTesting(false)
    }
  }

  const handleDbSave = async () => {
    setDbSaving(true)
    try {
      const res = await DbApi.register(dbForm)
      setDatabases((prev) => [...prev, res])
      setSelectedDb(res.id)
      setDbForm(emptyDbForm)
      setDbTestResult(null)
      onClose()
      toast({ title: 'DB 등록 완료', status: 'success' })
    } catch (err: any) {
      toast({ title: 'DB 저장 실패', description: err?.response?.data?.message ?? err.message, status: 'error' })
    } finally {
      setDbSaving(false)
    }
  }

  const updateDbForm = (patch: Partial<DbConnectionRequest>) => setDbForm({ ...dbForm, ...patch })

  const handleDbDelete = async () => {
    if (!selectedDb) {
      toast({ title: '삭제할 DB를 선택하세요.', status: 'warning' })
      return
    }
    const target = databases.find((d) => d.id === selectedDb)
    const ok = window.confirm(`'${target?.name ?? '선택된 DB'}'을 삭제하시겠습니까? 관련 대화 기록도 함께 삭제됩니다.`)
    if (!ok) return
    try {
      await DbApi.delete(selectedDb)
      const remaining = databases.filter((d) => d.id !== selectedDb)
      setDatabases(remaining)
      setSelectedDb(remaining[0]?.id)
      setSessionId(undefined)
      setMessages([])
      toast({ title: 'DB 삭제 완료', status: 'success' })
    } catch (err: any) {
      toast({ title: 'DB 삭제 실패', description: err?.response?.data?.message ?? err.message, status: 'error' })
    }
  }

  return (
    <Stack spacing={6}>
      <Flex justify="space-between" align="center">
        <Stack spacing={2}>
          <Heading size="lg">SQL Query Bot</Heading>
          <Text color="gray.300">스키마를 이해하는 AI가 안전하게 SELECT 쿼리를 제안합니다.</Text>
        </Stack>
        <HStack spacing={3}>
          <Tag size="lg" variant="subtle" colorScheme="purple">
            <TagLabel>{user ?? 'admin'}</TagLabel>
          </Tag>
          <Button variant="outline" onClick={logout}>
            로그아웃
          </Button>
        </HStack>
      </Flex>

      <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
        <CardBody>
          <Grid templateColumns={{ base: '1fr', md: '2fr 1fr auto auto auto' }} gap={4} alignItems="center">
            <GridItem>
              <FormControl>
                <FormLabel color="gray.200">데이터베이스 선택</FormLabel>
                <Select value={selectedDb ?? ''} onChange={(e) => setSelectedDb(Number(e.target.value))} placeholder="선택하세요">
                  {databases.map((db) => (
                    <option key={db.id} value={db.id}>
                      {db.name} ({db.dbType})
                    </option>
                  ))}
                </Select>
              </FormControl>
            </GridItem>
            <GridItem>
              {selectedDbName && (
                <Text color="gray.300" fontSize="sm">
                  {selectedDbName}와 연결된 세션에서 질의 중입니다.
                </Text>
              )}
            </GridItem>
            <GridItem>
              <Button leftIcon={<FiPlus />} variant="ghost" onClick={onToggle}>
                새 DB 추가
              </Button>
            </GridItem>
            <GridItem>
              <Button leftIcon={<FiRefreshCw />} variant="outline" onClick={newSession}>
                새 대화
              </Button>
            </GridItem>
            <GridItem>
              <Button colorScheme="red" variant="outline" onClick={handleDbDelete} isDisabled={!selectedDb}>
                선택 DB 삭제
              </Button>
            </GridItem>
          </Grid>
          {isOpen && (
            <Box mt={6} p={4} borderRadius="md" borderWidth="1px" borderColor="whiteAlpha.200" bg="blackAlpha.300">
              <HStack justify="space-between" mb={3}>
                <Heading size="sm">데이터베이스 등록</Heading>
                {dbTestResult && <Badge colorScheme={dbTestResult.success ? 'green' : 'red'}>{dbTestResult.success ? '테스트 통과' : '테스트 실패'}</Badge>}
              </HStack>
              <Grid templateColumns={{ base: '1fr', md: 'repeat(3, 1fr)' }} gap={3}>
                <FormControl>
                  <FormLabel>이름</FormLabel>
                  <Input value={dbForm.name} onChange={(e) => updateDbForm({ name: e.target.value })} />
                </FormControl>
                <FormControl>
                  <FormLabel>타입</FormLabel>
                  <Select value={dbForm.dbType} onChange={(e) => updateDbForm({ dbType: e.target.value as DbConnectionRequest['dbType'] })}>
                    <option value="POSTGRESQL">PostgreSQL</option>
                    <option value="MYSQL">MySQL</option>
                    <option value="MARIADB">MariaDB</option>
                  </Select>
                </FormControl>
                <FormControl>
                  <FormLabel>DB 이름</FormLabel>
                  <Input value={dbForm.databaseName} onChange={(e) => updateDbForm({ databaseName: e.target.value })} />
                </FormControl>
                <FormControl>
                  <FormLabel>호스트</FormLabel>
                  <Input value={dbForm.host} onChange={(e) => updateDbForm({ host: e.target.value })} />
                </FormControl>
                <FormControl>
                  <FormLabel>포트</FormLabel>
                  <Input
                    type="number"
                    value={dbForm.port ?? ''}
                    onChange={(e) => updateDbForm({ port: e.target.value ? Number(e.target.value) : undefined })}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>아이디</FormLabel>
                  <Input value={dbForm.username} onChange={(e) => updateDbForm({ username: e.target.value })} />
                </FormControl>
                <FormControl>
                  <FormLabel>비밀번호</FormLabel>
                  <Input type="password" value={dbForm.password} onChange={(e) => updateDbForm({ password: e.target.value })} />
                </FormControl>
              </Grid>
              <HStack mt={4} spacing={3}>
                <Button onClick={handleDbTest} isLoading={dbTesting} variant="outline">
                  연결 테스트
                </Button>
                <Button colorScheme="teal" onClick={handleDbSave} isLoading={dbSaving}>
                  저장
                </Button>
                <Button variant="ghost" onClick={onClose}>
                  닫기
                </Button>
              </HStack>
            </Box>
          )}
        </CardBody>
      </Card>

      <Grid templateColumns={{ base: '1fr', md: '2fr 1fr' }} gap={4}>
        <GridItem>
          <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px" minH="60vh">
            <CardBody>
              <VStack align="stretch" spacing={4}>
                <Box bg="blackAlpha.500" borderRadius="md" p={3}>
                  <Text color="gray.200" fontSize="sm">
                    AI는 스키마를 기반으로 쿼리를 제안하며 모호한 질문에는 추가 정보를 요청합니다. 쿼리가 명확하면 SQL만 반환합니다.
                  </Text>
                </Box>
                <Stack spacing={4} maxH="50vh" overflowY="auto" pr={2}>
                  {messages.length === 0 && <Text color="gray.400">대화를 시작해보세요. 이전 세션은 새 메시지 이후 표시됩니다.</Text>}
                  {messages.map((msg, idx) => (
                    <Box key={idx} bg={msg.role === 'USER' ? 'teal.900' : 'gray.800'} borderRadius="md" p={3} borderWidth="1px" borderColor="whiteAlpha.200">
                      <HStack justify="space-between" mb={2}>
                        <Badge colorScheme={msg.role === 'USER' ? 'teal' : 'purple'}>{msg.role === 'USER' ? '나' : 'AI'}</Badge>
                        <Text fontSize="xs" color="gray.400">
                          {formatDate(msg.createdAt)}
                        </Text>
                      </HStack>
                      <Text whiteSpace="pre-wrap" fontFamily={msg.role === 'ASSISTANT' ? 'mono' : 'body'}>
                        {msg.content}
                      </Text>
                    </Box>
                  ))}
                </Stack>
                <Divider />
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="예: 지난주 신규 가입자 수를 반환하는 SELECT 쿼리를 알려줘"
                  bg="blackAlpha.500"
                  borderColor="whiteAlpha.200"
                  minH="120px"
                />
                <Button colorScheme="teal" rightIcon={<FiArrowRight />} alignSelf="flex-end" onClick={handleSend} isLoading={sending}>
                  전송
                </Button>
              </VStack>
            </CardBody>
          </Card>
        </GridItem>
        <GridItem>
          <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
            <CardBody>
              <Stack spacing={3}>
                <Heading size="sm">사용 가이드</Heading>
                <Text color="gray.300">1) DB를 선택하고, 2) 질문을 입력하세요. 동일 세션에서 맥락이 유지됩니다.</Text>
                <Text color="gray.300">쿼리가 모호하면 AI가 한국어로 추가 정보를 요청합니다.</Text>
                <Text color="gray.300">명확한 경우 오류 없이 실행 가능한 SELECT 쿼리만 돌려줍니다.</Text>
              </Stack>
            </CardBody>
          </Card>
        </GridItem>
      </Grid>
    </Stack>
  )
}
