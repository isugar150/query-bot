import {
  Box,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalHeader,
  ModalOverlay,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  Text,
  Stack,
  HStack,
  Tag,
  Badge,
  Divider,
  useBreakpointValue,
} from '@chakra-ui/react'

type Props = {
  isOpen: boolean
  onClose: () => void
  columns: string[]
  rows: (string | number | boolean | null)[][]
}

export function ResultModal({ isOpen, onClose, columns, rows }: Props) {
  const isMobile = useBreakpointValue({ base: true, md: false }) ?? false
  const modalSize = useBreakpointValue({ base: 'full', md: '6xl' }) ?? '6xl'
  const hasData = columns.length > 0

  const renderTable = () => (
    <Box
      overflow="auto"
      maxH="70vh"
      borderWidth="1px"
      borderColor="whiteAlpha.200"
      borderRadius="md"
      bg="rgba(255,255,255,0.02)"
      sx={{ '&::-webkit-scrollbar': { height: '8px' } }}
    >
      <Table
        size="sm"
        variant="simple"
        colorScheme="whiteAlpha"
        sx={{
          'tbody tr:nth-of-type(odd) td': { bg: 'whiteAlpha.50' },
          'tbody tr:nth-of-type(even) td': { bg: 'whiteAlpha.100' },
        }}
      >
        <Thead position="sticky" top={0} zIndex={1} bg="whiteAlpha.200">
          <Tr>
            {columns.map((col) => (
              <Th key={col} whiteSpace="nowrap">
                {col}
              </Th>
            ))}
          </Tr>
        </Thead>
        <Tbody fontFamily="mono">
          {rows.map((row, rIdx) => (
            <Tr key={rIdx}>
              {row.map((cell, cIdx) => (
                <Td key={cIdx} maxW="320px" wordBreak="break-word">
                  {cell === null ? '∅' : String(cell)}
                </Td>
              ))}
            </Tr>
          ))}
          {rows.length === 0 && (
            <Tr>
              <Td colSpan={columns.length} textAlign="center">
                결과가 없습니다.
              </Td>
            </Tr>
          )}
        </Tbody>
      </Table>
    </Box>
  )

  const renderCards = () => (
    <Stack spacing={3} maxH="70vh" overflowY="auto">
      {rows.length === 0 && (
        <Box borderWidth="1px" borderRadius="md" p={4} bg="gray.800" borderColor="whiteAlpha.200">
          <Text color="gray.200">결과가 없습니다.</Text>
        </Box>
      )}
      {rows.map((row, rIdx) => (
        <Box
          key={rIdx}
          borderWidth="1px"
          borderRadius="md"
          p={4}
          bg="gray.800"
          borderColor="whiteAlpha.300"
        >
          <HStack justify="space-between" mb={2}>
            <Badge colorScheme="teal">#{rIdx + 1}</Badge>
            <Text fontSize="xs" color="gray.300">
              {columns.length} columns
            </Text>
          </HStack>
          <Stack spacing={2}>
            {row.map((cell, cIdx) => (
              <Box key={`${rIdx}-${cIdx}`}>
                <Tag size="sm" mr={2} colorScheme="teal" variant="subtle">
                  {columns[cIdx] ?? `컬럼 ${cIdx + 1}`}
                </Tag>
                <Text
                  as="span"
                  fontFamily="mono"
                  fontSize="sm"
                  wordBreak="break-word"
                  color="gray.100"
                >
                  {cell === null ? '∅' : String(cell)}
                </Text>
              </Box>
            ))}
          </Stack>
        </Box>
      ))}
    </Stack>
  )

  return (
    <Modal isOpen={isOpen} onClose={onClose} size={modalSize}>
      <ModalOverlay />
      <ModalContent
        bg="rgba(10,12,20,0.95)"
        borderWidth="1px"
        borderColor="whiteAlpha.200"
        backdropFilter="blur(6px)"
      >
        <ModalHeader color="gray.100">쿼리 실행 결과 (최대 100건)</ModalHeader>
        <ModalCloseButton />
        <ModalBody pb={6}>
          {hasData ? (
            <>
              <HStack justify="space-between" mb={3}>
                <Text color="gray.600" fontSize="sm">
                  {rows.length}행 · {columns.length}열
                </Text>
                <Divider flex="1" ml={3} borderColor="gray.200" />
              </HStack>
              {isMobile ? renderCards() : renderTable()}
            </>
          ) : (
            <Text>표시할 결과가 없습니다.</Text>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  )
}
