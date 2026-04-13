import { test, expect } from '../../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Treasure Cruise with Delve.
 *
 * Card: Treasure Cruise ({7}{U}) — Sorcery
 * Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)
 * Draw three cards.
 *
 * Covers: Delve selection UI (card images, exile from graveyard, mana cost reduction)
 */
test.describe('Treasure Cruise (Delve)', () => {
  test('cast with delve exiling graveyard cards to reduce cost', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Delver',
      player2Name: 'Opponent',
      player1: {
        hand: ['Treasure Cruise'],
        battlefield: [{ name: 'Island' }],
        graveyard: [
          'Mountain',
          'Forest',
          'Swamp',
          'Plains',
          'Mountain',
          'Forest',
          'Swamp',
        ],
        library: ['Island', 'Island', 'Island', 'Island'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Click Treasure Cruise to open action menu
    await p1.clickCard('Treasure Cruise')
    await p1.selectAction('Cast Treasure Cruise')

    // Delve selector should appear — select 7 graveyard cards to exile
    // The heading "Delve" should be visible
    await p1.page.getByRole('heading', { name: 'Delve' }).waitFor({ state: 'visible', timeout: 10_000 })
    await p1.screenshot('Delve selector open')

    // Card images should be rendered (img elements with card names as alt text)
    const cardImages = p1.page.locator('img[alt="Mountain"], img[alt="Forest"], img[alt="Swamp"], img[alt="Plains"]')
    await expect(cardImages.first()).toBeVisible({ timeout: 10_000 })

    // Select all 7 cards by clicking their buttons (aria-label matches card name)
    // Click each card — they are buttons with aria-label set to the card name
    const delveButtons = p1.page.locator('button[aria-label]').filter({
      has: p1.page.locator('img'),
    })
    const count = await delveButtons.count()
    for (let i = 0; i < count && i < 7; i++) {
      await delveButtons.nth(i).click()
    }
    await p1.screenshot('All graveyard cards selected for delve')

    // Click "Cast" to confirm delve selection
    await p1.page.getByRole('button', { name: 'Cast' }).click()

    // Mana selection appears — confirm mana payment
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).waitFor({ state: 'visible', timeout: 10_000 })
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).click()
    await p1.screenshot('Cast with delve')

    // Opponent resolves the spell
    // Wait for spell to appear on P2's stack, then resolve
    await p2.page.getByText('Draw 3 cards').waitFor({ state: 'visible', timeout: 10_000 })
    await p2.pass()

    // Player 1 should have drawn 3 cards (started with Treasure Cruise which was cast)
    // Hand should now contain the 3 drawn cards
    await p1.expectHandSize(3)

    // Graveyard should be empty (7 cards exiled + Treasure Cruise went to graveyard after resolving)
    // Actually Treasure Cruise is a sorcery so it goes to graveyard after resolving
    await p1.expectGraveyardSize(player1.playerId, 1) // just Treasure Cruise itself

    await p1.screenshot('End state — 3 cards drawn')
  })

  test('cast with partial delve (exile fewer cards than max)', async ({ createGame }) => {
    // Treasure Cruise costs {7}{U} (8 total). With 3 delve + 5 Islands = 8, exactly enough.
    const { player1, player2 } = await createGame({
      player1Name: 'Delver',
      player2Name: 'Opponent',
      player1: {
        hand: ['Treasure Cruise'],
        battlefield: [
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
        ],
        graveyard: ['Mountain', 'Forest', 'Swamp'],
        library: ['Island', 'Island', 'Island', 'Island'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Treasure Cruise
    await p1.clickCard('Treasure Cruise')
    await p1.selectAction('Cast Treasure Cruise')

    // Delve selector appears — select 3 cards (paying 3 of 7 generic, rest from lands)
    await p1.page.getByRole('heading', { name: 'Delve' }).waitFor({ state: 'visible', timeout: 10_000 })

    const delveButtons = p1.page.locator('button[aria-label]').filter({
      has: p1.page.locator('img'),
    })
    const count = await delveButtons.count()
    for (let i = 0; i < count && i < 3; i++) {
      await delveButtons.nth(i).click()
    }
    await p1.screenshot('3 cards selected for delve')

    // Cast with partial delve (3 exiled + 5 islands for remaining {4}{U})
    await p1.page.getByRole('button', { name: 'Cast' }).click()

    // Mana selection appears — confirm mana payment
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).waitFor({ state: 'visible', timeout: 10_000 })
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).click()

    // Opponent resolves
    // Wait for spell to appear on P2's stack, then resolve
    await p2.page.getByText('Draw 3 cards').waitFor({ state: 'visible', timeout: 10_000 })
    await p2.pass()

    // Should have drawn 3 cards
    await p1.expectHandSize(3)

    // All 5 Islands should be tapped (paid 4 generic + 1 blue)
    await p1.expectTapped('Island')

    await p1.screenshot('End state — partial delve')
  })

  test('cast with zero delve (skip delve selection)', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Delver',
      player2Name: 'Opponent',
      player1: {
        hand: ['Treasure Cruise'],
        battlefield: [
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Island' },
        ],
        graveyard: ['Mountain'],
        library: ['Island', 'Island', 'Island', 'Island'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Treasure Cruise
    await p1.clickCard('Treasure Cruise')
    await p1.selectAction('Cast Treasure Cruise')

    // Delve selector appears — don't select any cards, just cast
    await p1.page.getByRole('heading', { name: 'Delve' }).waitFor({ state: 'visible', timeout: 10_000 })
    await p1.screenshot('Delve selector — no cards selected')

    // Cast without exiling anything (pay full cost from lands)
    await p1.page.getByRole('button', { name: 'Cast', exact: true }).click()

    // Mana selection appears — confirm mana payment (all 8 Islands pre-selected)
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).waitFor({ state: 'visible', timeout: 10_000 })
    await p1.page.locator('button').filter({ hasText: /^Confirm/ }).click()

    // Opponent resolves — stack items always stop opponent
    await p2.resolveStack('Treasure Cruise')

    // Should have drawn 3 cards
    await p1.expectHandSize(3)

    // Graveyard should have Mountain + Treasure Cruise
    await p1.expectGraveyardSize(player1.playerId, 2)

    await p1.screenshot('End state — no delve used')
  })
})
