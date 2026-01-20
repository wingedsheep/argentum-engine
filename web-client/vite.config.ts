import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/game': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    sourcemap: true,
  },
})
