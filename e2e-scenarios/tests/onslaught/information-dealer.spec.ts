import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Information Dealer.
 *
 * Card: Information Dealer ({1}{U}) — Creature — Human Wizard (1/1)
 * "{T}: Look at the top X cards of your library, where X is the number of
 * Wizards you control, then put them back in any order."
 *
 * Covers:
 * - Activated ability with dynamic count based on Wizard count
 * - Single card view (X=1, only Information Dealer itself)
 * - Multi-card reorder UI (X=2, with a second Wizard)
 */
test.describe('Information Dealer', () => {
  test('view top 1 card with only Information Dealer (1 Wizard)', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Wizard Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Information Dealer', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain', 'Forest', 'Island'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Information Dealer's tap ability
    await p1.clickCard('Information Dealer')
    await p1.selectAction('Look at the top')

    // P2 must resolve the ability on the stack
    await p2.pass()

    // P1 sees the "View Card" overlay with 1 card — click OK to dismiss
    const okButton = p1.page.getByRole('button', { name: 'OK' })
    await okButton.waitFor({ state: 'visible', timeout: 10_000 })

    await p1.screenshot('View single card')

    // Verify the overlay shows "View Card" heading
    await expect(p1.page.getByRole('heading', { name: 'View Card' })).toBeVisible()

    await okButton.click()
    await p1.screenshot('After confirming single card view')

    // Information Dealer should now be tapped
    await p1.expectTapped('Information Dealer')
  })

  test('view and reorder top 2 cards with 2 Wizards', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Wizard Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Information Dealer', tapped: false, summoningSickness: false },
          { name: 'Sage Aven', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain', 'Forest', 'Island'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Information Dealer's tap ability
    await p1.clickCard('Information Dealer')
    await p1.selectAction('Look at the top')

    // P2 must resolve the ability on the stack
    await p2.pass()

    // P1 sees the "Reorder Cards" overlay with 2 cards
    const confirmButton = p1.page.getByRole('button', { name: 'Confirm Order' })
    await confirmButton.waitFor({ state: 'visible', timeout: 10_000 })

    await p1.screenshot('Reorder 2 cards')

    // Verify the overlay shows "Reorder Cards" heading
    await expect(p1.page.getByRole('heading', { name: 'Reorder Cards' })).toBeVisible()

    // Confirm the order (keep default)
    await confirmButton.click()
    await p1.screenshot('After confirming reorder')

    // Information Dealer should now be tapped
    await p1.expectTapped('Information Dealer')
  })
})
