import {
  Badge,
  Box,
  Button,
  Card,
  CardBody,
  CardHeader,
  Checkbox,
  Divider,
  FormControl,
  FormLabel,
  Grid,
  GridItem,
  HStack,
  Heading,
  Input,
  Select,
  Stack,
  Text,
  useToast,
} from '@chakra-ui/react'
import { useState } from 'react'
import { DbApi } from '../api/db/db'
import { InitApi } from '../api/init/init'
import type { AuthResponse, DbConnectionRequest, DbTestResponse } from '../types'
import { extractErrorMessage } from '../utils/error'

type Props = {
  onComplete: (auth: AuthResponse) => void
}

const defaultDb: DbConnectionRequest = {
  name: '주요 데이터베이스',
  dbType: 'POSTGRESQL',
  host: 'localhost',
  port: 5432,
  databaseName: 'app',
  username: 'postgres',
  password: 'postgres',
}

export function OnboardingPage({ onComplete }: Props) {
  const toast = useToast()
  const [adminId, setAdminId] = useState('admin')
  const [adminPw, setAdminPw] = useState('')
  const [dbInfo, setDbInfo] = useState<DbConnectionRequest>(defaultDb)
  const [includeDb, setIncludeDb] = useState(true)
  const [testing, setTesting] = useState(false)
  const [testResult, setTestResult] = useState<DbTestResponse | null>(null)
  const [saving, setSaving] = useState(false)

  const canStart = !!adminId && !!adminPw && (!includeDb || !!testResult?.success)

  const handleTest = async () => {
    setTesting(true)
    setTestResult(null)
    try {
      const result = await DbApi.testConnection(dbInfo)
      setTestResult(result)
      if (result.success) {
        toast({ title: '연결 성공', description: result.message, status: 'success' })
      } else {
        toast({ title: '연결 실패', description: result.message, status: 'error' })
      }
    } catch (err: unknown) {
      toast({ title: '테스트 실패', description: extractErrorMessage(err), status: 'error' })
    } finally {
      setTesting(false)
    }
  }

  const handleSubmit = async () => {
    setSaving(true)
    try {
      const payload = { admin: { username: adminId, password: adminPw }, database: includeDb ? dbInfo : null }
      const res = await InitApi.setup(payload)
      onComplete(res.auth)
      toast({ title: '초기 설정 완료', status: 'success' })
    } catch (err: unknown) {
      toast({ title: '설정 실패', description: extractErrorMessage(err), status: 'error' })
    } finally {
      setSaving(false)
    }
  }

  const updateDb = (patch: Partial<DbConnectionRequest>) => {
    setDbInfo({ ...dbInfo, ...patch })
    if (includeDb) {
      setTestResult(null)
    }
  }

  return (
    <Box maxW="6xl" mx="auto" py={10}>
      <Stack spacing={6} textAlign="left">
        <Heading size="xl">Query Bot 첫 설정</Heading>
        <Text color="gray.300" maxW="3xl">
          관리자 계정과 연결할 데이터베이스 정보를 입력하세요. 필요한 경우 DB 테스트를 선행해 스키마를 가져옵니다.
        </Text>
        <Grid templateColumns={{ base: '1fr', md: 'repeat(2, 1fr)' }} gap={6}>
          <GridItem>
            <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
              <CardHeader>
                <HStack justify="space-between">
                  <Heading size="md">관리자 계정</Heading>
                  <Badge colorScheme="purple">필수</Badge>
                </HStack>
              </CardHeader>
              <CardBody>
                <Stack spacing={4}>
                  <FormControl>
                    <FormLabel>아이디</FormLabel>
                    <Input value={adminId} onChange={(e) => setAdminId(e.target.value)} placeholder="admin" />
                  </FormControl>
                  <FormControl>
                    <FormLabel>비밀번호</FormLabel>
                    <Input type="password" value={adminPw} onChange={(e) => setAdminPw(e.target.value)} placeholder="●●●●●●" />
                  </FormControl>
                </Stack>
              </CardBody>
            </Card>
          </GridItem>
          <GridItem>
            <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
              <CardHeader>
                <HStack justify="space-between">
                  <Heading size="md">데이터베이스 연결</Heading>
                  <Checkbox
                    isChecked={includeDb}
                    onChange={(e) => {
                      const checked = e.target.checked
                      setIncludeDb(checked)
                      if (!checked) {
                        setTestResult(null)
                      }
                    }}
                  >
                    지금 연결
                  </Checkbox>
                </HStack>
              </CardHeader>
              <CardBody>
                <Stack spacing={3} opacity={includeDb ? 1 : 0.4} pointerEvents={includeDb ? 'auto' : 'none'}>
                  <FormControl>
                    <FormLabel>이름</FormLabel>
                    <Input value={dbInfo.name} onChange={(e) => updateDb({ name: e.target.value })} placeholder="마케팅 DB" />
                  </FormControl>
                  <FormControl>
                    <FormLabel>DB 타입</FormLabel>
                    <Select value={dbInfo.dbType} onChange={(e) => updateDb({ dbType: e.target.value as DbConnectionRequest['dbType'] })}>
                      <option value="POSTGRESQL">PostgreSQL</option>
                      <option value="MYSQL">MySQL</option>
                      <option value="MARIADB">MariaDB</option>
                    </Select>
                  </FormControl>
                  <HStack>
                    <FormControl>
                      <FormLabel>호스트</FormLabel>
                      <Input value={dbInfo.host} onChange={(e) => updateDb({ host: e.target.value })} />
                    </FormControl>
                    <FormControl>
                      <FormLabel>포트</FormLabel>
                      <Input
                        type="number"
                        value={dbInfo.port ?? ''}
                        onChange={(e) => updateDb({ port: e.target.value ? Number(e.target.value) : undefined })}
                      />
                    </FormControl>
                  </HStack>
                  <FormControl>
                    <FormLabel>DB 이름/스키마 (콤마 구분)</FormLabel>
                    <Input
                      value={dbInfo.databaseName}
                      onChange={(e) => updateDb({ databaseName: e.target.value })}
                      placeholder="app,public,analytics"
                    />
                  </FormControl>
                  <HStack>
                    <FormControl>
                      <FormLabel>아이디</FormLabel>
                      <Input value={dbInfo.username} onChange={(e) => updateDb({ username: e.target.value })} />
                    </FormControl>
                    <FormControl>
                      <FormLabel>비밀번호</FormLabel>
                      <Input type="password" value={dbInfo.password} onChange={(e) => updateDb({ password: e.target.value })} />
                    </FormControl>
                  </HStack>
                  <HStack>
                    <Button onClick={handleTest} isLoading={testing} variant="outline">
                      연결 테스트
                    </Button>
                    {testResult && (
                      <Badge colorScheme={testResult.success ? 'green' : 'red'}>{testResult.success ? '성공' : '실패'}</Badge>
                    )}
                  </HStack>
                  {testResult?.schema && (
                    <Box fontSize="sm" color="gray.200" bg="blackAlpha.400" p={3} borderRadius="md" maxH="160px" overflowY="auto">
                      <Text mb={2} fontWeight="semibold">
                        스키마 미리보기
                      </Text>
                      {testResult.schema.tables.slice(0, 5).map((table) => (
                        <Box key={table.name} mb={2}>
                          <Text fontWeight="bold">
                            {table.schema}.{table.name}
                          </Text>
                          <Text fontSize="xs" color="gray.300">
                            {table.columns.map((c) => `${c.name} (${c.type})`).join(', ')}
                          </Text>
                        </Box>
                      ))}
                    </Box>
                  )}
                </Stack>
              </CardBody>
            </Card>
          </GridItem>
        </Grid>
        <Divider opacity={0.3} />
        <Button size="lg" colorScheme="teal" onClick={handleSubmit} isLoading={saving} isDisabled={!canStart}>
          시작하기
        </Button>
      </Stack>
    </Box>
  )
}
