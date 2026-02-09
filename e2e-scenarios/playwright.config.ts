import { defineConfig } from '@playwright/test'

const skipWebServer = !!process.env.SKIP_WEB_SERVER

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: 'html',
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  webServer: skipWebServer
    ? undefined
    : [
        {
          command: 'cd .. && GAME_DEV_ENDPOINTS_ENABLED=true ./gradlew :game-server:bootRun',
          url: 'http://localhost:8080/api/dev/scenarios/cards',
          reuseExistingServer: !process.env.CI,
          timeout: 120_000,
        },
        {
          command: 'cd ../web-client && npm run dev',
          url: 'http://localhost:5173',
          reuseExistingServer: !process.env.CI,
          timeout: 30_000,
        },
      ],
})
