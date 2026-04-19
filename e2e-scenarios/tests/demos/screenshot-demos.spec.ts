import { test, chromium, type Browser } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'
import { createScenario, type ScenarioRequest } from '../../helpers/scenarioApi'
import { HAND } from '../../helpers/selectors'

const SCENARIOS_DIR = path.resolve(__dirname, '../../../test-scenarios')
const OUTPUT_DIR = path.resolve(__dirname, '../../screenshots')

const SCENARIOS = [
  { key: 'creature-heavy', file: 'creature-heavy-ui-demo.json' },
  { key: 'enchantment-heavy', file: 'enchantment-heavy-ui-demo.json' },
]

interface Viewport {
  label: string
  width: number
  height: number
  scale: number
}

const VIEWPORTS: Viewport[] = [
  { label: '1280x800@1x',  width: 1280, height: 800,  scale: 1   },
  { label: '1440x900@1x',  width: 1440, height: 900,  scale: 1   },
  { label: '1920x1080@1x', width: 1920, height: 1080, scale: 1   },
  { label: '2560x1440@1x', width: 2560, height: 1440, scale: 1   },
  { label: 'mbp13-1440x900@2x',   width: 1440, height: 900,  scale: 2    },
  { label: 'mbp14-1512x982@2x',   width: 1512, height: 982,  scale: 2    },
  { label: 'mbp-m1-1512x982@1x',  width: 1512, height: 982,  scale: 1    },
  { label: 'mbp16-1728x1117@2x',  width: 1728, height: 1117, scale: 2    },
  { label: 'win-1536x864@1.25x',  width: 1536, height: 864,  scale: 1.25 },
  { label: 'win-1707x960@1.5x',   width: 1707, height: 960,  scale: 1.5  },
  { label: '4k-1920x1080@2x',     width: 1920, height: 1080, scale: 2    },
]

function loadScenario(file: string): ScenarioRequest {
  const full = path.join(SCENARIOS_DIR, file)
  return JSON.parse(fs.readFileSync(full, 'utf8')) as ScenarioRequest
}

test.describe.configure({ mode: 'serial' })

test.describe('Demo screenshots across viewports', () => {
  let browser: Browser

  test.beforeAll(async () => {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true })
    browser = await chromium.launch()
  })

  test.afterAll(async () => {
    await browser?.close()
  })

  for (const scenario of SCENARIOS) {
    for (const vp of VIEWPORTS) {
      test(`${scenario.key} @ ${vp.label}`, async () => {
        test.setTimeout(120_000)
        const config = loadScenario(scenario.file)
        const response = await createScenario(config)

        const outDir = path.join(OUTPUT_DIR, scenario.key)
        fs.mkdirSync(outDir, { recursive: true })

        for (const player of ['player1', 'player2'] as const) {
          const info = response[player]
          const ctx = await browser.newContext({
            viewport: { width: vp.width, height: vp.height },
            deviceScaleFactor: vp.scale,
          })
          await ctx.addInitScript(
            ({ token, name }: { token: string; name: string }) => {
              localStorage.setItem('argentum-token', token)
              localStorage.setItem('argentum-player-name', name)
            },
            { token: info.token, name: info.name },
          )
          const page = await ctx.newPage()
          await page.goto('http://localhost:5173/')
          await page.locator(HAND).waitFor({ state: 'visible', timeout: 30_000 })
          // Let card images settle.
          await page.waitForLoadState('networkidle').catch(() => {})
          await page.waitForTimeout(500)

          const filename = `${vp.label}-${player}.png`
          await page.screenshot({ path: path.join(outDir, filename), fullPage: false })
          await ctx.close()
        }
      })
    }
  }
})
