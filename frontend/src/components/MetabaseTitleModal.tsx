import {
  Button,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
} from "@chakra-ui/react";

type Props = {
  isOpen: boolean;
  title: string;
  onTitleChange: (value: string) => void;
  onSubmit: () => void;
  onClose: () => void;
  isSubmitting?: boolean;
};

export function MetabaseTitleModal({
  isOpen,
  title,
  onTitleChange,
  onSubmit,
  onClose,
  isSubmitting,
}: Props) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Metabase로 전송할 제목을 입력하세요</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <Input
            placeholder="새로운 쿼리"
            value={title}
            onChange={(e) => onTitleChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                onSubmit();
              }
            }}
          />
        </ModalBody>
        <ModalFooter>
          <Button mr={3} onClick={onClose}>
            취소
          </Button>
          <Button colorScheme="teal" onClick={onSubmit} isLoading={isSubmitting}>
            전송
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
