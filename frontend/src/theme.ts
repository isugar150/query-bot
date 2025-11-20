import { extendTheme, type ThemeConfig } from '@chakra-ui/react'

const config: ThemeConfig = {
  initialColorMode: 'dark',
  useSystemColorMode: false,
}

export const theme = extendTheme({
  config,
  fonts: {
    heading: "'Space Grotesk', 'Pretendard', system-ui, sans-serif",
    body: "'Inter', 'Space Grotesk', system-ui, sans-serif",
  },
  styles: {
    global: {
      body: {
        bg: 'radial-gradient(circle at 20% 20%, rgba(46, 213, 166, 0.12), transparent 25%), radial-gradient(circle at 80% 0%, rgba(80, 66, 255, 0.12), transparent 25%), #0a0c14',
        color: 'gray.50',
        minHeight: '100vh',
      },
    },
  },
  colors: {
    brand: {
      500: '#3BC9A8',
      600: '#2D9C86',
    },
  },
  components: {
    Card: {
      baseStyle: {
        bg: 'rgba(255,255,255,0.04)',
        borderColor: 'whiteAlpha.200',
      },
    },
  },
})
