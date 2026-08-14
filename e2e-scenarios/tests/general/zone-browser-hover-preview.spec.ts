import { test, expect } from '../../fixtures/scenarioFixture'
import type { Page } from '@playwright/test'

/**
 * E2E browser test: the card hover preview must paint above the graveyard/exile browsers.
 *
 * The zone browsers (graveyard, exile, deck, …) are full-screen overlays portalled to `<body>`
 * at z-index 2000. The hover preview used to render in place, inside the board tree — fine for a
 * player, but the spectator and replay shells wrap the whole board in their own
 * `position: fixed; z-index: 1500` stacking context, which clamped the preview *below* those
 * body-level overlays: hovering a card in the graveyard showed it dimmed behind the backdrop
 * instead of on top. The preview is now portalled to `<body>` on the tooltip layer.
 *
 * The assertions encode that invariant structurally (portal parent + z-index ordering) rather
 * than by pixel comparison, since a screenshot can't distinguish "behind a 0.85 black scrim"
 * from "slightly dark card art".
 */

interface PreviewStacking {
  previewZ: number
  /** z-indexes of any stacking contexts between the preview layer and <body>. */
  trappingAncestors: number[]
  /** z-index of the zone browser's full-screen backdrop. */
  overlayZ: number | null
}

/** Computed stacking facts for the hover preview of `cardName`, or null if it isn't showing. */
function previewStacking(page: Page, cardName: string): Promise<PreviewStacking | null> {
  return page.evaluate((name) => {
    // The preview renders the card at ~280px wide; grid tiles are 160px or less.
    const img = Array.from(document.querySelectorAll('img')).find(
      (i) => i.alt === name && i.getBoundingClientRect().width > 250,
    )
    if (!img) return null

    // The preview layer is the positioned wrapper HoverCardPreview renders.
    let layer: HTMLElement = img
    while (layer.parentElement && getComputedStyle(layer).position !== 'fixed') {
      layer = layer.parentElement
    }

    // Every z-indexed ancestor between that layer and <body> is a stacking context that would
    // trap the preview underneath a body-level overlay.
    const trappingAncestors: number[] = []
    for (let el = layer.parentElement; el && el !== document.body; el = el.parentElement) {
      const z = parseInt(getComputedStyle(el).zIndex, 10)
      if (!Number.isNaN(z)) trappingAncestors.push(z)
    }

    // The zone browser's backdrop: the fixed full-screen ancestor of its heading.
    const heading = Array.from(document.querySelectorAll('h2')).find((h) =>
      /Graveyard|Exile/.test(h.textContent ?? ''),
    )
    let overlayZ: number | null = null
    for (let el = heading?.parentElement ?? null; el && el !== document.body; el = el.parentElement) {
      if (getComputedStyle(el).position === 'fixed') {
        overlayZ = parseInt(getComputedStyle(el).zIndex, 10)
        break
      }
    }

    return {
      previewZ: parseInt(getComputedStyle(layer).zIndex, 10),
      trappingAncestors,
      overlayZ,
    }
  }, cardName)
}

/**
 * Open `playerId`'s `zone` pile, hover `cardName` in the browser it opens, and assert the preview
 * paints above the overlay.
 */
async function expectPreviewOnTop(
  page: Page,
  playerId: string,
  zone: 'graveyard' | 'exile',
  cardName: string,
) {
  // Address the pile by owner: seat order differs between the player's view and a spectator's.
  await page.locator(`[data-${zone}-id="${playerId}"]`).click()

  // Zone browsers are portalled to <body>, so scope the grid to that overlay — the same card
  // name also sits on the (unopened) pile behind it.
  const heading = new RegExp(`${zone === 'graveyard' ? 'Graveyard' : 'Exile'} \\(\\d+\\)`, 'i')
  const overlay = page
    .locator('body > div')
    .filter({ has: page.getByRole('heading', { name: heading }) })
  await overlay.waitFor({ state: 'visible', timeout: 10_000 })
  await overlay.locator(`img[alt="${cardName}"]`).first().hover()

  await expect
    .poll(async () => (await previewStacking(page, cardName)) !== null, { timeout: 10_000 })
    .toBe(true)

  const stacking = (await previewStacking(page, cardName))!
  expect(stacking.trappingAncestors, 'no stacking context may sit between preview and <body>').toEqual([])
  expect(stacking.overlayZ, 'zone browser backdrop not found').not.toBeNull()
  expect(stacking.previewZ).toBeGreaterThan(stacking.overlayZ!)

  await page.keyboard.press('Escape')
  await overlay.waitFor({ state: 'detached', timeout: 10_000 })
}

async function checkZoneBrowsers(page: Page, playerId: string) {
  await expectPreviewOnTop(page, playerId, 'graveyard', 'Hill Giant')
  await expectPreviewOnTop(page, playerId, 'exile', 'Goblin Bully')
}

test.describe('Zone browsers — hover preview stacking', () => {
  test('graveyard and exile previews paint above the overlay, for player and spectator', async ({
    createGame,
    browser,
  }) => {
    const { player1, response } = await createGame({
      player1Name: 'Alice',
      player2Name: 'Bob',
      player1: {
        battlefield: [{ name: 'Mountain' }, { name: 'Grizzly Bears' }],
        graveyard: ['Lightning Bolt', 'Hill Giant'],
        exile: ['Goblin Bully'],
        library: ['Mountain', 'Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Swamp' }],
        library: ['Swamp', 'Swamp'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const alice = response.player1.playerId

    // The player's own view — the board is the root stacking context here.
    await checkZoneBrowsers(player1.page, alice)

    // A spectator's view — the board sits inside the spectator shell's stacking context, which
    // is what used to bury the preview.
    const spectatorContext = await browser.newContext()
    const spectator = await spectatorContext.newPage()
    await spectator.goto(`/?spectate=${response.sessionId}`)
    await spectator
      .locator(`[data-graveyard-id="${alice}"]`)
      .waitFor({ state: 'visible', timeout: 30_000 })
    await checkZoneBrowsers(spectator, alice)
    await spectatorContext.close()
  })
})
