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
} from '@chakra-ui/react'

type Props = {
  isOpen: boolean
  onClose: () => void
  columns: string[]
  rows: (string | number | boolean | null)[][]
}

export function ResultModal({ isOpen, onClose, columns, rows }: Props) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} size="6xl">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>쿼리 실행 결과 (최대 100건)</ModalHeader>
        <ModalCloseButton />
        <ModalBody pb={6}>
          {columns.length > 0 ? (
            <Box overflow="auto">
              <Table size="sm" variant="striped" colorScheme="gray">
                <Thead>
                  <Tr>
                    {columns.map((col) => (
                      <Th key={col}>{col}</Th>
                    ))}
                  </Tr>
                </Thead>
                <Tbody>
                  {rows.map((row, rIdx) => (
                    <Tr key={rIdx}>
                        {row.map((cell, cIdx) => (
                          <Td key={cIdx}>{cell === null ? '' : String(cell)}</Td>
                        ))}
                    </Tr>
                  ))}
                  {rows.length === 0 && (
                    <Tr>
                      <Td colSpan={columns.length}>결과가 없습니다.</Td>
                    </Tr>
                  )}
                </Tbody>
              </Table>
            </Box>
          ) : (
            <Text>표시할 결과가 없습니다.</Text>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  )
}
