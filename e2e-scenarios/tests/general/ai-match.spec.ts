import { test, expect } from '@playwright/test'
import * as fs from 'node:fs'
import * as path from 'node:path'

/**
 * Watch an LLM vs LLM game using Bloomburrow decks.
 *
 * Starts an AI-only sealed tournament via the dev API, then opens the
 * spectate URL so you can watch the match play out in a real browser.
 *
 * Configure per-player models with env vars (optional — falls back to the
 * server's configured model):
 *
 *   AI_MODEL_P1=claude-opus-4-6 AI_MODEL_P2=gpt-4o npx playwright test tests/general/ai-match --headed
 *
 * Run headed to actually watch:
 *   cd e2e-scenarios && npm run test:headed -- tests/general/ai-match
 *
 * Build decks with the Draftsim autobuilder (fast — skips LLM deck build):
 *   AI_SKIP_LLM_DECK=true ...
 *
 * Use fixed pre-built decks (skip sealed pool + deckbuilding entirely):
 *   AI_DECK_P1=path/to/deck1.json AI_DECK_P2=path/to/deck2.json ...
 *   Each deck JSON is a `{ "Card Name": count }` object.
 */

// Run headed and maximized
test.use({
  viewport: null,
  launchOptions: { args: ['--start-maximized'] },
})

const SERVER_URL = 'http://localhost:8080'
const CLIENT_URL = 'http://localhost:5173'

// Skipped by default — this is a long-running LLM-driven spectator test.
// Opt in via `just watch-ai-match` (which sets AI_MATCH=true), or export AI_MATCH=true manually.
test.skip(
  !process.env.AI_MATCH,
  'AI vs AI match is opt-in — set AI_MATCH=true or use `just watch-ai-match`'
)

test('AI vs AI Bloomburrow match', async ({ page, request }) => {
  test.setTimeout(20 * 60 * 1000) // 20 minutes — LLM games can be slow

  const model1 = process.env.AI_MODEL_P1 ?? null
  const model2 = process.env.AI_MODEL_P2 ?? null
  const skipLlmDeckbuilding = process.env.AI_SKIP_LLM_DECK !== 'false'
  // Comma-separated list of set codes (e.g., "BLB" or "ONS,LGN,SCG"). Defaults to BLB.
  const setCodes = (process.env.AI_SET_CODES ?? 'BLB')
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)

  const deckPathP1 = process.env.AI_DECK_P1?.trim() || null
  const deckPathP2 = process.env.AI_DECK_P2?.trim() || null
  const fixedDecksMode = !!(deckPathP1 && deckPathP2)
  if ((deckPathP1 && !deckPathP2) || (!deckPathP1 && deckPathP2)) {
    throw new Error('AI_DECK_P1 and AI_DECK_P2 must both be set (or both unset).')
  }

  const loadDeck = (p: string): Record<string, number> => {
    const resolved = path.resolve(p)
    const raw = fs.readFileSync(resolved, 'utf8')
    const parsed = JSON.parse(raw)
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error(`Deck JSON at ${resolved} must be an object of { "Card Name": count }`)
    }
    return parsed as Record<string, number>
  }

  const body: Record<string, unknown> = {
    playerCount: 2,
  }
  if (fixedDecksMode) {
    const deck1 = loadDeck(deckPathP1!)
    const deck2 = loadDeck(deckPathP2!)
    body.decks = [deck1, deck2]
    console.log(`Player 1 deck: ${deckPathP1} (${Object.values(deck1).reduce((a, b) => a + b, 0)} cards)`)
    console.log(`Player 2 deck: ${deckPathP2} (${Object.values(deck2).reduce((a, b) => a + b, 0)} cards)`)
  } else {
    body.setCodes = setCodes
    body.skipLlmDeckbuilding = skipLlmDeckbuilding
  }
  if (model1 || model2) {
    body.models = [model1 ?? null, model2 ?? null]
  }

  // Create the AI tournament
  const response = await request.post(`${SERVER_URL}/api/dev/ai-tournament`, { data: body })
  expect(response.ok(), `Failed to create AI tournament: ${await response.text()}`).toBe(true)

  const { lobbyId, spectateUrl, message } = await response.json()
  console.log(`Tournament created: ${lobbyId}`)
  console.log(message)
  if (!fixedDecksMode) {
    console.log(`Sets: ${setCodes.join(', ')}`)
    console.log(`Deck building: ${skipLlmDeckbuilding ? 'Draftsim (fast)' : 'LLM'}`)
  }
  if (model1) console.log(`Player 1 model: ${model1}`)
  if (model2) console.log(`Player 2 model: ${model2}`)

  // Pre-inject the player name into localStorage so the app auto-connects
  // without showing the name-entry screen
  await page.addInitScript(() => {
    localStorage.setItem('argentum-player-name', 'Spectator')
  })

  // If PROFILE=true, activate the RenderProfiler wrappers before the app
  // mounts. Persists across in-app navigations (Watch button, etc.).
  const profilingEnabled = process.env.PROFILE === 'true'
  if (profilingEnabled) {
    await page.addInitScript(() => {
      ;(window as unknown as { __profile?: boolean }).__profile = true
    })
    console.log('Render profiling enabled — report will print at the end')
  }

  await page.goto(`${CLIENT_URL}${spectateUrl}`)

  // Wait for the tournament page to connect
  await expect(
    page.getByText('Standings').or(page.getByText('Round')).or(page.getByText('[AI]')).first()
  ).toBeVisible({ timeout: 30_000 })
  console.log('Tournament page loaded')

  // The AIs auto-build decks and auto-ready — wait for the match to start
  const watchButton = page.getByRole('button', { name: 'Watch' }).first()
  await expect(watchButton).toBeVisible({ timeout: 120_000 })
  console.log('Match started — clicking Watch')
  await watchButton.click()

  // Wait for the spectator view to mount (loading state or live board)
  await expect(
    page.getByText('Back to Overview').or(page.getByText('Spectating')).first()
  ).toBeVisible({ timeout: 30_000 })
  console.log('Spectating game board — watching match play out...')

  // When profiling, run for a bounded window and capture data periodically so we
  // get usable numbers even if the game never ends or the browser is closed early.
  // When not profiling, keep the old behavior: wait for game-over.
  const captureProfile = async (label: string) => {
    if (!profilingEnabled) return
    try {
      const rows = await page.evaluate(() => {
        const w = window as unknown as { __profileReport?: () => unknown }
        return w.__profileReport ? w.__profileReport() : []
      })
      console.log(`Render profiler report (${label}):`)
      console.table(rows)
    } catch (err) {
      console.log(`Profile capture (${label}) failed: ${(err as Error).message}`)
    }
  }

  // Abort-aware sleep so we stop polling immediately when the page/browser closes.
  let closed = false
  page.on('close', () => { closed = true })
  const sleep = (ms: number) => new Promise<void>((resolve) => {
    const t = setTimeout(resolve, ms)
    page.once('close', () => { clearTimeout(t); resolve() })
  })

  if (profilingEnabled) {
    const PROFILE_WINDOW_MS = 5 * 60 * 1000  // 5 minutes of gameplay is plenty for render stats
    const SNAPSHOT_EVERY_MS = 60 * 1000
    const deadline = Date.now() + PROFILE_WINDOW_MS
    let snapshot = 0
    while (Date.now() < deadline && !closed) {
      await sleep(Math.min(SNAPSHOT_EVERY_MS, deadline - Date.now()))
      snapshot += 1
      await captureProfile(`snapshot ${snapshot} @ ${Math.round((Date.now() - (deadline - PROFILE_WINDOW_MS)) / 1000)}s`)
    }
    await captureProfile('final')
  } else {
    // Watch until game over or timeout
    await page.waitForSelector(
      'text=Game Over, text=Victory, text=Defeat, text=wins, text=Match Complete',
      { timeout: 18 * 60 * 1000 }
    ).catch(() => {
      console.log('Game did not complete within timeout — match is still running')
    })
  }

  console.log('Done watching')
})
