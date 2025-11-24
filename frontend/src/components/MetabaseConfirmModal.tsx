import {
  Button,
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
  onClose: () => void;
  onConfirm: () => void;
};

export function MetabaseConfirmModal({ isOpen, onClose, onConfirm }: Props) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Metabase에 SQL을 추가했습니다. 확인하시겠습니까?</ModalHeader>
        <ModalCloseButton />
        <ModalBody>Metabase에서 쿼리를 바로 확인할 수 있습니다.</ModalBody>
        <ModalFooter>
          <Button mr={3} onClick={onClose}>
            닫기
          </Button>
          <Button colorScheme="teal" onClick={onConfirm}>
            확인하기
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
