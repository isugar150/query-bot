import { isAxiosError } from 'axios'

export function extractErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    return (error.response?.data as { message?: string } | undefined)?.message ?? error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return '알 수 없는 오류가 발생했습니다.'
}
