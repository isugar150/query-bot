import {
  Badge,
  Box,
  Button,
  HStack,
  Image,
  Text,
  Show,
  Tooltip,
} from "@chakra-ui/react";
import { FiCopy } from "react-icons/fi";
import { FiPlay } from "react-icons/fi";
import type { ChatMessage } from "../types";

type Props = {
  message: ChatMessage;
  timestamp: string;
  isAssistant: boolean;
  isRunnable: boolean;
  onCopy: () => void;
  onExecute: () => void;
  execLoading: boolean;
  showMetabase: boolean;
  metabaseLabel: string;
  metabaseLoading: boolean;
  onMetabase?: () => void;
};

export function ChatMessageItem({
  message,
  timestamp,
  isAssistant,
  isRunnable,
  onCopy,
  onExecute,
  execLoading,
  showMetabase,
  metabaseLabel,
  metabaseLoading,
  onMetabase,
}: Props) {
  return (
    <Box
      bg={message.role === "USER" ? "teal.900" : "gray.800"}
      borderRadius="md"
      p={3}
      borderWidth="1px"
      borderColor="whiteAlpha.200"
    >
      <HStack justify="space-between" mb={2}>
        <Badge colorScheme={message.role === "USER" ? "teal" : "purple"}>
          {message.role === "USER" ? "나" : "AI"}
        </Badge>
        <HStack spacing={2}>
          {isAssistant && isRunnable && (
            <HStack spacing={1}>
              {showMetabase && (
                <>
                  <Show above="sm">
                    <Button
                      size="xs"
                      variant="outline"
                      leftIcon={
                        <Image
                          src="/icon/metabase.png"
                          alt="Metabase"
                          boxSize="16px"
                        />
                      }
                      isLoading={metabaseLoading}
                      onClick={onMetabase}
                    >
                      {metabaseLabel}
                    </Button>
                  </Show>
                  <Show below="sm">
                    <Tooltip label={metabaseLabel}>
                      <Button
                        size="xs"
                        variant="outline"
                        aria-label={metabaseLabel}
                        isLoading={metabaseLoading}
                        onClick={onMetabase}
                      >
                        <Image
                          src="/icon/metabase.png"
                          alt="Metabase"
                          boxSize="16px"
                        />
                      </Button>
                    </Tooltip>
                  </Show>
                </>
              )}
              <Show above="sm">
                <Button size="xs" variant="outline" leftIcon={<FiCopy />} onClick={onCopy}>
                  쿼리 복사
                </Button>
                <Button
                  size="xs"
                  variant="outline"
                  isLoading={execLoading}
                  leftIcon={<FiPlay />}
                  onClick={onExecute}
                >
                  실행 (최대 100건)
                </Button>
              </Show>
              <Show below="sm">
                <Tooltip label="쿼리 복사">
                  <Button
                    size="xs"
                    variant="outline"
                    aria-label="쿼리 복사"
                    onClick={onCopy}
                  >
                    <FiCopy />
                  </Button>
                </Tooltip>
                <Tooltip label="실행 (최대 100건)">
                  <Button
                    size="xs"
                    variant="outline"
                    aria-label="실행"
                    isLoading={execLoading}
                    onClick={onExecute}
                  >
                    <FiPlay />
                  </Button>
                </Tooltip>
              </Show>
            </HStack>
          )}
          <Text fontSize="xs" color="gray.400">
            {timestamp}
          </Text>
        </HStack>
      </HStack>
      <Text
        whiteSpace="pre-wrap"
        fontFamily={isAssistant ? "mono" : "body"}
        wordBreak="break-word"
      >
        {message.content}
      </Text>
    </Box>
  );
}
