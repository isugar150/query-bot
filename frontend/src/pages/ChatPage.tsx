import {
  Box,
  Button,
  ButtonGroup,
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
  Badge,
  Tag,
  TagLabel,
  Text,
  Textarea,
  useDisclosure,
  useToast,
  VStack,
  IconButton,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  AlertDialog,
  AlertDialogOverlay,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogBody,
  AlertDialogFooter,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@chakra-ui/react";
import { FiArrowRight, FiPlus, FiRefreshCw } from "react-icons/fi";
import { LuChevronDown } from "react-icons/lu";
import { useEffect, useRef, useState } from "react";
import { ChatApi } from "../api/chat/chat";
import { DbApi } from "../api/db/db";
import { MetabaseApi } from "../api/metabase";
import { ChatMessageItem } from "../components/ChatMessageItem";
import { MetabaseConfirmModal } from "../components/MetabaseConfirmModal";
import { MetabaseTitleModal } from "../components/MetabaseTitleModal";
import { useAuthStore } from "../store/auth";
import type {
  ChatMessage,
  ChatSession,
  DbConnectionRequest,
  DbSummary,
  DbTestResponse,
} from "../types";
import { ResultModal } from "../components/ResultModal";
import { extractErrorMessage } from "../utils/error";

const emptyDbForm: DbConnectionRequest = {
  name: "",
  dbType: "POSTGRESQL",
  host: "localhost",
  port: undefined,
  databaseName: "",
  username: "",
  password: "",
};

type Props = {
  user?: string;
};

export function ChatPage({ user }: Props) {
  const toast = useToast();
  const logout = useAuthStore((state) => state.clear);
  const [databases, setDatabases] = useState<DbSummary[]>([]);
  const [selectedDb, setSelectedDb] = useState<number | undefined>();
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [sessionId, setSessionId] = useState<number | undefined>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [aiTyping, setAiTyping] = useState(false);
  const [metabaseCardId, setMetabaseCardId] = useState<number | undefined>();
  const [metabaseCardUrl, setMetabaseCardUrl] = useState<string | undefined>();

  const { isOpen, onToggle, onClose } = useDisclosure();
  const [dbForm, setDbForm] = useState<DbConnectionRequest>(emptyDbForm);
  const [dbTesting, setDbTesting] = useState(false);
  const [dbTestResult, setDbTestResult] = useState<DbTestResponse | null>(null);
  const [dbSaving, setDbSaving] = useState(false);
  const [execLoading, setExecLoading] = useState(false);
  const [execResult, setExecResult] = useState<{
    columns: string[];
    rows: (string | number | boolean | null)[][];
  } | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const initialLoadRef = useRef(true);
  const [metabaseAvailable, setMetabaseAvailable] = useState(false);
  const [metabaseSending, setMetabaseSending] = useState(false);
  const [metabaseResult, setMetabaseResult] = useState<{
    id: number;
    url: string;
  } | null>(null);
  const {
    isOpen: isResultOpen,
    onOpen: openResult,
    onClose: closeResult,
  } = useDisclosure();
  const {
    isOpen: isMetabaseConfirmOpen,
    onOpen: openMetabaseConfirm,
    onClose: closeMetabaseConfirm,
  } = useDisclosure();
  const {
    isOpen: isOverwriteConfirmOpen,
    onOpen: openOverwriteConfirm,
    onClose: closeOverwriteConfirm,
  } = useDisclosure();
  const {
    isOpen: isMetabaseManageOpen,
    onOpen: openMetabaseManage,
    onClose: closeMetabaseManage,
  } = useDisclosure();
  const {
    isOpen: isMetabaseTitleOpen,
    onOpen: openMetabaseTitle,
    onClose: closeMetabaseTitle,
  } = useDisclosure();
  const [metabaseTitle, setMetabaseTitle] = useState("");
  const [pendingMetabaseSql, setPendingMetabaseSql] = useState<string | null>(
    null,
  );
  const overwriteCancelRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const fetchDbs = async () => {
      try {
        const res = await DbApi.list();
        setDatabases(res);
        if (!selectedDb && res.length > 0) {
          const first = res[0].id;
          setSelectedDb(first);
          await loadSessions(first);
        }
      } catch (err: unknown) {
        toast({
          title: "DB 목록을 불러오지 못했습니다",
          description: extractErrorMessage(err),
          status: "error",
        });
      }
    };
    fetchDbs();

    const checkMetabase = async () => {
      try {
        const res = await MetabaseApi.status();
        setMetabaseAvailable(res.available);
      } catch (err) {
        setMetabaseAvailable(false);
      }
    };
    checkMetabase();
  }, []);

  const loadSessions = async (dbId: number) => {
    try {
      const res = await ChatApi.sessions(dbId);
      setSessions(res);
      setSessionId(undefined);
      setMetabaseCardId(undefined);
      setMetabaseCardUrl(undefined);
      setMessages([]);
    } catch (err: unknown) {
      setSessions([]);
      setSessionId(undefined);
      setMetabaseCardId(undefined);
      setMetabaseCardUrl(undefined);
      setMessages([]);
      toast({
        title: "세션 목록을 불러오지 못했습니다",
        description: extractErrorMessage(err),
        status: "error",
      });
    }
  };

  const loadHistory = async (session: number) => {
    initialLoadRef.current = true;
    try {
      const res = await ChatApi.history(session);
      setSessionId(res.sessionId);
      setMessages(res.history);
      const cardId = res.metabaseCardId ?? undefined;
      const cardUrl = res.metabaseCardUrl ?? undefined;
      setMetabaseCardId(cardId);
      setMetabaseCardUrl(cardUrl);
      if (cardId && cardUrl) {
        setMetabaseResult({ id: cardId, url: cardUrl });
      } else {
        setMetabaseResult(null);
      }
    } catch (err: unknown) {
      setMessages([]);
      setMetabaseCardId(undefined);
      setMetabaseCardUrl(undefined);
      setMetabaseResult(null);
      const message = extractErrorMessage(err);
      if (message && !/404/i.test(message)) {
        toast({
          title: "대화 불러오기 실패",
          description: message,
          status: "error",
        });
      }
    }
  };

  const refreshSessions = async (dbId: number) => {
    try {
      const res = await ChatApi.sessions(dbId);
      setSessions(res);
      if (sessionId) {
        const current = res.find((s) => s.id === sessionId);
        setMetabaseCardId(current?.metabaseCardId ?? undefined);
        setMetabaseCardUrl(current?.metabaseCardUrl ?? undefined);
      }
    } catch (err: unknown) {
      toast({
        title: "세션 목록 갱신 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    }
  };

  const handleSend = async () => {
    if (!input.trim() || !selectedDb) {
      toast({ title: "데이터베이스와 질문을 확인하세요.", status: "warning" });
      return;
    }
    const selected = databases.find((d) => d.id === selectedDb);
    if (selected && !selected.schemaReady) {
      toast({
        title: "스키마 수집 중입니다.",
        description: "스키마가 준비될 때까지 잠시만 기다려주세요.",
        status: "info",
      });
      return;
    }
    setSending(true);
    setAiTyping(true);
    const optimisticUser: ChatMessage = {
      role: "USER",
      content: input,
      createdAt: new Date().toISOString(),
    };
    const previousMessages = messages;
    setMessages([...messages, optimisticUser]);
    setInput("");
    const isNewSession = !sessionId;
    try {
      const res = await ChatApi.ask({
        dbId: selectedDb,
        message: input,
        sessionId,
      });
      setMessages(res.history);
      setSessionId(res.sessionId);
      setMetabaseCardId(res.metabaseCardId ?? undefined);
      setMetabaseCardUrl(res.metabaseCardUrl ?? undefined);
      if (res.metabaseCardId && res.metabaseCardUrl) {
        setMetabaseResult({
          id: res.metabaseCardId,
          url: res.metabaseCardUrl,
        });
      }
      if (isNewSession) {
        await refreshSessions(selectedDb);
      }
    } catch (err: unknown) {
      // Rollback optimistic message on failure
      setMessages(previousMessages);
      toast({
        title: "질문 전송 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    } finally {
      setSending(false);
      setAiTyping(false);
    }
  };

  const handleExecute = async (sql: string) => {
    if (!selectedDb) {
      toast({ title: "DB를 먼저 선택하세요.", status: "warning" });
      return;
    }
    if (!isReadOnlyQuery(sql)) {
      toast({
        title: "데이터 조회 쿼리만 실행할 수 있습니다.",
        description: "INSERT/UPDATE/DELETE/DDL 쿼리는 실행이 차단됩니다.",
        status: "warning",
      });
      return;
    }
    setExecLoading(true);
    try {
      const res = await DbApi.execute({ dbId: selectedDb, sql });
      setExecResult(res);
      openResult();
    } catch (err: unknown) {
      toast({
        title: "쿼리 실행 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    } finally {
      setExecLoading(false);
    }
  };

  const handleCopyQuery = async (sql: string) => {
    if (!navigator.clipboard) {
      toast({
        title: "복사할 수 없습니다.",
        description: "브라우저가 클립보드 복사를 지원하지 않습니다.",
        status: "warning",
      });
      return;
    }
    try {
      await navigator.clipboard.writeText(sql);
      toast({
        title: "쿼리를 복사했습니다.",
        status: "success",
        duration: 1500,
      });
    } catch (err: unknown) {
      const message = extractErrorMessage(err);
      toast({
        title: "복사 실패",
        description: message || "클립보드 접근을 허용해주세요.",
        status: "error",
      });
    }
  };

  const startSendToMetabase = (sql: string) => {
    if (!sessionId) {
      toast({
        title: "세션이 없습니다.",
        description: "AI 답변을 받은 후에 Metabase로 전송할 수 있습니다.",
        status: "warning",
      });
      return;
    }
    if (!metabaseAvailable) {
      toast({
        title: "Metabase가 비활성화되었습니다.",
        description: "환경변수 METABASE_ENABLED를 확인하세요.",
        status: "warning",
      });
      return;
    }
    setPendingMetabaseSql(sql);
    if (metabaseCardId) {
      openMetabaseManage();
    } else {
      setMetabaseTitle("");
      openMetabaseTitle();
    }
  };

  const handleSendToMetabase = async (sql: string, title?: string) => {
    if (!sessionId) {
      toast({
        title: "세션이 없습니다.",
        description: "AI 답변을 받은 후에 Metabase로 전송할 수 있습니다.",
        status: "warning",
      });
      return;
    }
    if (!metabaseAvailable) {
      toast({
        title: "Metabase가 비활성화되었습니다.",
        description: "환경변수 METABASE_ENABLED를 확인하세요.",
        status: "warning",
      });
      return;
    }
    setMetabaseSending(true);
    try {
      const res = await MetabaseApi.sendQuery({
        sessionId,
        query: sql,
        title: title && title.trim() ? title.trim() : undefined,
      });
      setMetabaseResult({ id: res.id, url: res.url });
      setMetabaseCardId(res.id);
      setMetabaseCardUrl(res.url);
      setSessions((prev) =>
        prev.map((s) =>
          s.id === sessionId
            ? { ...s, metabaseCardId: res.id, metabaseCardUrl: res.url }
            : s,
        ),
      );
      openMetabaseConfirm();
      return true;
    } catch (err: unknown) {
      toast({
        title: "Metabase 전송 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
      return false;
    } finally {
      setMetabaseSending(false);
    }
  };

  const handleSubmitMetabaseTitle = async () => {
    if (!pendingMetabaseSql) return;
    if (!metabaseTitle.trim()) {
      window.alert("제목을 입력해주세요.");
      return;
    }
    const ok = await handleSendToMetabase(
      pendingMetabaseSql,
      metabaseTitle || undefined,
    );
    if (ok) {
      closeMetabaseTitle();
      setPendingMetabaseSql(null);
      setMetabaseTitle("");
    }
  };

  const formatDate = (val: string) => {
    const iso = /[zZ]|[+\-]\d{2}:?\d{2}$/.test(val) ? val : `${val}Z`;

    const date = new Date(iso);

    return date.toLocaleString(undefined, {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: false,
    });
  };

  const isReadOnlyQuery = (query: string) => {
    const statements = query
      .split(";")
      .map((s) => s.trim())
      .filter(Boolean);
    const forbidden =
      /^(insert|update|delete|create|alter|drop|truncate|merge|replace)\b/i;
    return statements.every(
      (stmt) => !forbidden.test(stmt) && /^(select|with)\b/i.test(stmt),
    );
  };

  const resolveMetabaseUrl = () => {
    if (metabaseResult?.url) return metabaseResult.url;
    if (metabaseCardUrl) return metabaseCardUrl;
    const current = sessionId
      ? sessions.find((s) => s.id === sessionId)
      : undefined;
    return current?.metabaseCardUrl ?? null;
  };

  const handleDbTest = async () => {
    setDbTesting(true);
    setDbTestResult(null);
    try {
      const res = await DbApi.testConnection(dbForm);
      setDbTestResult(res);
      toast({
        title: res.success ? "DB 연결 성공" : "DB 연결 실패",
        description: res.message,
        status: res.success ? "success" : "error",
      });
    } catch (err: unknown) {
      toast({
        title: "테스트 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    } finally {
      setDbTesting(false);
    }
  };

  const handleDbSave = async () => {
    setDbSaving(true);
    try {
      const res = await DbApi.register(dbForm);
      setDatabases((prev) => [...prev, res]);
      setSelectedDb(res.id);
      setDbForm(emptyDbForm);
      setDbTestResult(null);
      onClose();
      toast({ title: "DB 등록 완료", status: "success" });
    } catch (err: unknown) {
      toast({
        title: "DB 저장 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    } finally {
      setDbSaving(false);
    }
  };

  const updateDbForm = (patch: Partial<DbConnectionRequest>) =>
    setDbForm({ ...dbForm, ...patch });

  const scrollMessagesToBottom = (behavior: ScrollBehavior) => {
    const container = messagesContainerRef.current;
    if (!container) return;
    container.scrollTo({ top: container.scrollHeight, behavior });
  };

  useEffect(() => {
    if (messages.length === 0) return;
    if (initialLoadRef.current) {
      scrollMessagesToBottom("auto");
      initialLoadRef.current = false;
      return;
    }
    const lastMessage = messages[messages.length - 1];
    if (lastMessage?.role !== "ASSISTANT") return;
    scrollMessagesToBottom("smooth");
  }, [messages]);

  const closeMetabaseDialog = () => {
    closeMetabaseConfirm();
    setMetabaseResult(null);
  };

  const cancelMetabaseTitle = () => {
    closeMetabaseTitle();
    setPendingMetabaseSql(null);
    setMetabaseTitle("");
  };

  const confirmOverwriteMetabase = async () => {
    if (!pendingMetabaseSql) return;
    closeMetabaseManage();
    const ok = await handleSendToMetabase(pendingMetabaseSql);
    if (ok) {
      closeOverwriteConfirm();
      setPendingMetabaseSql(null);
    }
  };

  const openMetabaseFromManage = () => {
    if (!metabaseAvailable) {
      toast({
        title: "Metabase가 비활성화되었습니다.",
        description: "환경변수 METABASE_ENABLED를 확인하세요.",
        status: "warning",
      });
      return;
    }
    const url = resolveMetabaseUrl();
    if (url) {
      window.open(url, "_blank", "noopener,noreferrer");
      closeMetabaseManage();
      return;
    }
    toast({
      title: "Metabase 링크를 찾을 수 없습니다.",
      description: "쿼리를 다시 전송하거나 서버 환경변수를 확인하세요.",
      status: "warning",
    });
  };

  const openMetabaseInNewTab = () => {
    if (!metabaseAvailable) {
      toast({
        title: "Metabase가 비활성화되었습니다.",
        description: "환경변수 METABASE_ENABLED를 확인하세요.",
        status: "warning",
      });
      return;
    }
    const url = resolveMetabaseUrl();
    if (!url) return;
    window.open(url, "_blank", "noopener,noreferrer");
    closeMetabaseDialog();
  };

  const handleDbDelete = async () => {
    if (!selectedDb) {
      toast({ title: "삭제할 DB를 선택하세요.", status: "warning" });
      return;
    }
    const target = databases.find((d) => d.id === selectedDb);
    const ok = window.confirm(
      `'${target?.name ?? "선택된 DB"}'을 삭제하시겠습니까? 관련 대화 기록도 함께 삭제됩니다.`,
    );
    if (!ok) return;
    try {
      await DbApi.delete(selectedDb);
      const remaining = databases.filter((d) => d.id !== selectedDb);
      setDatabases(remaining);
      setSelectedDb(remaining[0]?.id);
      setSessionId(undefined);
      setMessages([]);
      toast({ title: "DB 삭제 완료", status: "success" });
    } catch (err: unknown) {
      toast({
        title: "DB 삭제 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    }
  };

  const handleDbRefresh = async () => {
    if (!selectedDb) {
      toast({ title: "갱신할 DB를 선택하세요.", status: "warning" });
      return;
    }
    try {
      const res = await DbApi.refresh(selectedDb);
      setDatabases((prev) => prev.map((db) => (db.id === res.id ? res : db)));
      toast({ title: "DB 스키마 갱신 완료", status: "success" });
      await loadHistory(res.id);
    } catch (err: unknown) {
      toast({
        title: "갱신 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    }
  };

  const createNewSession = () => {
    if (!selectedDb) {
      onToggle();
      return;
    }
    setSessionId(undefined);
    setMetabaseCardId(undefined);
    setMetabaseCardUrl(undefined);
    setMetabaseResult(null);
    setMessages([]);
  };

  const handleSessionDelete = async () => {
    if (!sessionId) {
      toast({ title: "삭제할 세션을 선택하세요.", status: "warning" });
      return;
    }
    const target = sessions.find((s) => s.id === sessionId);
    const ok = window.confirm(
      `'${target?.title ?? "선택된 세션"}'을 삭제하시겠습니까? 대화 내용이 모두 삭제됩니다.`,
    );
    if (!ok) return;
    try {
      await ChatApi.deleteSession(sessionId);
      const remaining = sessions.filter((s) => s.id !== sessionId);
      setSessions(remaining);
      if (remaining[0]) {
        const nextId = remaining[0].id;
        setSessionId(nextId);
        setMetabaseCardId(remaining[0].metabaseCardId ?? undefined);
        setMetabaseCardUrl(remaining[0].metabaseCardUrl ?? undefined);
        await loadHistory(nextId);
      } else {
        setSessionId(undefined);
        setMetabaseCardId(undefined);
        setMetabaseCardUrl(undefined);
        setMetabaseResult(null);
        setMessages([]);
      }
      toast({ title: "세션 삭제 완료", status: "success" });
    } catch (err: unknown) {
      toast({
        title: "세션 삭제 실패",
        description: extractErrorMessage(err),
        status: "error",
      });
    }
  };

  return (
    <Stack spacing={6}>
      <Flex justify="space-between" align="center">
        <Heading size="lg">Jm's SQL Query Bot</Heading>
        <HStack spacing={3}>
          <Tag size="lg" variant="subtle" colorScheme="purple">
            <TagLabel>{user ?? "admin"}</TagLabel>
          </Tag>
          <Button variant="outline" onClick={logout}>
            로그아웃
          </Button>
        </HStack>
      </Flex>

      <Card bg="whiteAlpha.50" borderColor="whiteAlpha.200" borderWidth="1px">
        <CardBody>
          <Grid
            templateColumns={{ base: "1fr", md: "2fr 1fr auto" }}
            gap={4}
            alignItems="center"
          >
            <GridItem>
              <FormControl>
                <FormLabel color="gray.200">데이터베이스 선택</FormLabel>
                <Select
                  value={selectedDb ?? ""}
                  onChange={async (e) => {
                    const id = Number(e.target.value);
                    setSelectedDb(id);
                    if (id) {
                      await loadSessions(id);
                    } else {
                      setSessions([]);
                      setMessages([]);
                    }
                  }}
                  placeholder="선택하세요"
                >
                  {databases.map((db) => (
                    <option key={db.id} value={db.id}>
                      {db.name} ({db.dbType})
                    </option>
                  ))}
                </Select>
              </FormControl>
            </GridItem>
            <GridItem>
              <FormControl>
                <FormLabel color="gray.200">세션 선택</FormLabel>
                <Select
                  value={sessionId ?? ""}
                  onChange={async (e) => {
                    const value = e.target.value;
                    if (!value) {
                      createNewSession();
                      return;
                    }
                    const sid = Number(value);
                    setSessionId(sid);
                    if (sid) {
                      await loadHistory(sid);
                    }
                  }}
                  placeholder="세션을 선택하세요"
                >
                  {sessions.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.title}
                    </option>
                  ))}
                </Select>
              </FormControl>
            </GridItem>
            <GridItem alignSelf={{ base: "stretch", md: "end" }}>
              <HStack justify="flex-end">
                <Menu>
                  <ButtonGroup isAttached size="sm" variant="outline">
                    <Button
                      onClick={createNewSession}
                      leftIcon={<FiRefreshCw />}
                    >
                      {selectedDb ? "새 대화" : "새 DB 추가"}
                    </Button>
                    <MenuButton
                      as={IconButton}
                      icon={<LuChevronDown />}
                      aria-label="more actions"
                    />
                  </ButtonGroup>
                  <MenuList>
                    {selectedDb ? (
                      <>
                        <MenuItem onClick={handleSessionDelete} color="red.400">
                          선텍 세션 삭제
                        </MenuItem>
                        <MenuItem
                          icon={<FiRefreshCw />}
                          onClick={async () => {
                            const ok = window.confirm(
                              "선택한 DB의 스키마를 다시 수집할까요?",
                            );
                            if (ok) {
                              await handleDbRefresh();
                            }
                          }}
                        >
                          DB 정보 갱신
                        </MenuItem>
                        <MenuItem icon={<FiPlus />} onClick={onToggle}>
                          새 DB 추가
                        </MenuItem>
                        <MenuItem onClick={handleDbDelete} color="red.400">
                          선택 DB 삭제
                        </MenuItem>
                      </>
                    ) : (
                      <MenuItem icon={<FiPlus />} onClick={onToggle}>
                        새 DB 추가
                      </MenuItem>
                    )}
                  </MenuList>
                </Menu>
              </HStack>
            </GridItem>
          </Grid>
          {isOpen && (
            <Box
              mt={6}
              p={4}
              borderRadius="md"
              borderWidth="1px"
              borderColor="whiteAlpha.200"
              bg="blackAlpha.300"
            >
              <HStack justify="space-between" mb={3}>
                <Heading size="sm">데이터베이스 등록</Heading>
                {dbTestResult && (
                  <Badge colorScheme={dbTestResult.success ? "green" : "red"}>
                    {dbTestResult.success ? "테스트 통과" : "테스트 실패"}
                  </Badge>
                )}
              </HStack>
              <Grid
                templateColumns={{ base: "1fr", md: "repeat(3, 1fr)" }}
                gap={3}
              >
                <FormControl>
                  <FormLabel>이름</FormLabel>
                  <Input
                    value={dbForm.name}
                    onChange={(e) => updateDbForm({ name: e.target.value })}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>타입</FormLabel>
                  <Select
                    value={dbForm.dbType}
                    onChange={(e) =>
                      updateDbForm({
                        dbType: e.target.value as DbConnectionRequest["dbType"],
                      })
                    }
                  >
                    <option value="POSTGRESQL">PostgreSQL</option>
                    <option value="MYSQL">MySQL</option>
                    <option value="MARIADB">MariaDB</option>
                  </Select>
                </FormControl>
                <FormControl>
                  <FormLabel>DB 이름/스키마 (콤마 구분)</FormLabel>
                  <Input
                    value={dbForm.databaseName}
                    onChange={(e) =>
                      updateDbForm({ databaseName: e.target.value })
                    }
                    placeholder="app,public,analytics"
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>호스트</FormLabel>
                  <Input
                    value={dbForm.host}
                    onChange={(e) => updateDbForm({ host: e.target.value })}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>포트</FormLabel>
                  <Input
                    type="number"
                    value={dbForm.port ?? ""}
                    onChange={(e) =>
                      updateDbForm({
                        port: e.target.value
                          ? Number(e.target.value)
                          : undefined,
                      })
                    }
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>아이디</FormLabel>
                  <Input
                    value={dbForm.username}
                    onChange={(e) => updateDbForm({ username: e.target.value })}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>비밀번호</FormLabel>
                  <Input
                    type="password"
                    value={dbForm.password}
                    onChange={(e) => updateDbForm({ password: e.target.value })}
                  />
                </FormControl>
              </Grid>
              <HStack mt={4} spacing={3}>
                <Button
                  onClick={handleDbTest}
                  isLoading={dbTesting}
                  variant="outline"
                >
                  연결 테스트
                </Button>
                <Button
                  colorScheme="teal"
                  onClick={handleDbSave}
                  isLoading={dbSaving}
                >
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

      <Card
        bg="whiteAlpha.50"
        borderColor="whiteAlpha.200"
        borderWidth="1px"
        minH="60vh"
      >
        <CardBody>
          <VStack align="stretch" spacing={4}>
            <Box bg="blackAlpha.500" borderRadius="md" p={3}>
              <Text color="gray.200" fontSize="sm">
                새로운 질문은 상단의 ‘새 대화’ 버튼을 클릭해 시작하며,
                데이터베이스 정보가 변경된 경우 우측 ‘더보기’에서
                동기화하십시오.
              </Text>
              <Text color="gray.400" fontSize="xs" mt={2}>
                대화 내용은 마지막 질문 후 30일이 지나면 자동으로 삭제됩니다.
              </Text>
            </Box>
            <Stack
              spacing={4}
              maxH="50vh"
              overflowY="auto"
              pr={2}
              ref={messagesContainerRef}
            >
              {messages.map((msg, idx) => {
                const isAssistant = msg.role === "ASSISTANT";
                const isRunnable = isReadOnlyQuery(msg.content);
                return (
                  <ChatMessageItem
                    key={`${msg.createdAt}-${idx}`}
                    message={msg}
                    timestamp={formatDate(msg.createdAt)}
                    isAssistant={isAssistant}
                    isRunnable={isRunnable}
                    onCopy={() => handleCopyQuery(msg.content)}
                    onExecute={() => handleExecute(msg.content)}
                    execLoading={execLoading}
                    showMetabase={metabaseAvailable}
                    metabaseLabel={
                      metabaseCardId ? "쿼리 관리하기" : "쿼리 추가"
                    }
                    metabaseLoading={metabaseSending}
                    onMetabase={
                      isAssistant && isRunnable
                        ? () => startSendToMetabase(msg.content)
                        : undefined
                    }
                  />
                );
              })}
              {aiTyping && (
                <Box
                  bg="gray.800"
                  borderRadius="md"
                  p={3}
                  borderWidth="1px"
                  borderColor="whiteAlpha.200"
                >
                  <HStack justify="space-between" mb={2}>
                    <Badge colorScheme="purple">AI</Badge>
                    <Text fontSize="xs" color="gray.400">
                      답변 준비 중...
                    </Text>
                  </HStack>
                  <Text color="gray.300">AI가 답변을 생성하고 있습니다...</Text>
                </Box>
              )}
            </Stack>
            <Divider />
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder="예: 지난주 신규 가입자 수를 반환하는 쿼리를 알려줘"
              bg="blackAlpha.500"
              borderColor="whiteAlpha.200"
              minH="120px"
              readOnly={sending}
            />
            <Button
              colorScheme="teal"
              rightIcon={<FiArrowRight />}
              alignSelf="flex-end"
              onClick={handleSend}
              isLoading={sending}
            >
              전송
            </Button>
          </VStack>
        </CardBody>
      </Card>

      <ResultModal
        isOpen={isResultOpen}
        onClose={closeResult}
        columns={execResult?.columns ?? []}
        rows={execResult?.rows ?? []}
      />

      <MetabaseTitleModal
        isOpen={isMetabaseTitleOpen}
        onClose={cancelMetabaseTitle}
        title={metabaseTitle}
        onTitleChange={setMetabaseTitle}
        onSubmit={handleSubmitMetabaseTitle}
        isSubmitting={metabaseSending}
      />

      <MetabaseConfirmModal
        isOpen={isMetabaseConfirmOpen}
        onClose={closeMetabaseDialog}
        onConfirm={openMetabaseInNewTab}
      />
      <Modal
        isOpen={isMetabaseManageOpen}
        onClose={closeMetabaseManage}
        isCentered
      >
        <ModalOverlay />
        <ModalContent>
          <ModalHeader>Metabase 쿼리 관리</ModalHeader>
          <ModalBody>어떤 작업을 진행할까요?</ModalBody>
          <ModalFooter gap={3}>
            <Button
              variant="outline"
              onClick={() => {
                closeMetabaseManage();
                openOverwriteConfirm();
              }}
            >
              쿼리 업데이트
            </Button>
            <Button variant="outline" onClick={openMetabaseFromManage}>
              Metabase에서 보기
            </Button>
            <Button colorScheme="teal" onClick={closeMetabaseManage}>
              닫기
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
      <AlertDialog
        isOpen={isOverwriteConfirmOpen}
        leastDestructiveRef={overwriteCancelRef}
        onClose={closeOverwriteConfirm}
        isCentered
      >
        <AlertDialogOverlay />
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            Metabase 쿼리 덮어쓰기
          </AlertDialogHeader>
          <AlertDialogBody>
            기존 추가한 쿼리가 덮어쓰기됩니다. 계속하시겠습니까?
          </AlertDialogBody>
          <AlertDialogFooter>
            <Button ref={overwriteCancelRef} onClick={closeOverwriteConfirm}>
              취소
            </Button>
            <Button
              colorScheme="teal"
              onClick={confirmOverwriteMetabase}
              ml={3}
              isLoading={metabaseSending}
            >
              덮어쓰기
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Stack>
  );
}
