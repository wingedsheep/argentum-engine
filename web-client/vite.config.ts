import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { execSync } from 'child_process'
import path from 'path'

let commitHash = process.env.VITE_COMMIT_HASH?.slice(0, 7) || 'unknown'
try {
  if (commitHash === 'unknown') {
    commitHash = execSync('git rev-parse --short HEAD').toString().trim()
  }
} catch {
  // git may not be available (e.g. Docker build)
}

// Backend the dev server proxies to — override when the game server runs on a non-default port.
const gameServerUrl = process.env.GAME_SERVER_URL || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  define: {
    __COMMIT_HASH__: JSON.stringify(commitHash),
  },
  server: {
    port: 5173,
    proxy: {
      '/game': {
        target: gameServerUrl,
        ws: true,
        changeOrigin: true,
      },
      '/api': {
        target: gameServerUrl,
        changeOrigin: true,
      },
    },
  },
  // `vite preview` serves the real production bundle. Without the same proxy it can't reach the
  // game server, so the only way to exercise a build the way a player meets it — minified, no
  // StrictMode double-render — was to deploy it. Perf work needs those numbers, not dev's.
  preview: {
    port: 5175,
    proxy: {
      '/game': {
        target: gameServerUrl,
        ws: true,
        changeOrigin: true,
      },
      '/api': {
        target: gameServerUrl,
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: true,
  },
})
