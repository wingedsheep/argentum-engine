import { defineConfig } from '@playwright/test'
import path from 'node:path'
import base from './playwright.config'

function port(name: string): number {
  const value = process.env[name] ?? ''
  if (!/^\d+$/.test(value) || Number(value) < 1024 || Number(value) > 65535) {
    throw new Error(`${name} must be a port between 1024 and 65535`)
  }
  return Number(value)
}

const clientPort = port('E2E_CLIENT_PORT')
const serverPort = port('E2E_SERVER_PORT')
if (clientPort === serverPort) throw new Error('E2E client and server ports must differ')

const clientUrl = `http://localhost:${clientPort}`
const serverUrl = `http://localhost:${serverPort}`
// Scenario helpers run in test workers, which inherit this environment.
process.env.E2E_BASE_URL = clientUrl
process.env.E2E_SERVER_URL = serverUrl

export default defineConfig({
  ...base,
  outputDir: '.loop-artifacts/test-results',
  reporter: [
    ['line'],
    ['html', { outputFolder: '.loop-artifacts/report', open: 'never' }],
  ],
  use: {
    ...base.use,
    browserName: 'chromium',
    headless: true,
    baseURL: clientUrl,
    screenshot: 'on',
    trace: 'retain-on-failure',
  },
  // Always start this worktree's servers, even if SKIP_WEB_SERVER is set. Occupied ports fail
  // rather than silently verifying another checkout. Playwright tears down both processes.
  webServer: [
    {
      command: `just e2e-server ${serverPort}`,
      cwd: path.resolve(__dirname, '..'),
      url: `${serverUrl}/api/dev/scenarios/cards`,
      reuseExistingServer: false,
      timeout: 2_100_000, // Build semaphore wait (30 minutes) plus server startup.
      gracefulShutdown: { signal: 'SIGTERM', timeout: 10_000 },
    },
    {
      command: `npm run dev -- --port ${clientPort} --strictPort`,
      cwd: path.resolve(__dirname, '../web-client'),
      env: { GAME_SERVER_URL: serverUrl },
      url: clientUrl,
      reuseExistingServer: false,
      timeout: 30_000,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 5_000 },
    },
  ],
})
