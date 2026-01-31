import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright configuration for e2e tests.
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e',

  // Run tests in parallel - important for multi-player tests
  fullyParallel: false, // We need sequential for multi-player coordination

  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,

  // Retry on CI only
  retries: process.env.CI ? 2 : 0,

  // Use 1 worker for multi-player tests to ensure coordination
  workers: 1,

  // Reporter to use
  reporter: 'html',

  // Shared settings for all the projects below
  use: {
    // Base URL for all pages
    baseURL: 'http://localhost:5173',

    // Collect trace when retrying the failed test
    trace: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Video on failure
    video: 'retain-on-failure',
  },

  // Configure projects for major browsers
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome', // Use system Chrome instead of bundled Chromium
      },
    },
  ],

  // Run your local dev server before starting the tests
  // Note: For faster local dev, run `just server` and `just client` manually
  // and set SKIP_WEB_SERVER=true environment variable
  webServer: process.env.SKIP_WEB_SERVER ? undefined : [
    {
      // Start the game server
      command: 'cd .. && ./gradlew :game-server:bootRun',
      url: 'http://localhost:8080/game',
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
    },
    {
      // Start the web client
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 30000,
    },
  ],
})
