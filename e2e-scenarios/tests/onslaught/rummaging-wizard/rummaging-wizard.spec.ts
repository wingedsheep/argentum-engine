import { test, expect } from '../../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Rummaging Wizard.
 *
 * Card: Rummaging Wizard ({3}{U}) — Creature — Human Wizard (2/2)
 * "{2}{U}: Surveil 1. (Look at the top card of your library. You may put
 * that card into your graveyard.)"
 *
 * Covers:
 * - Surveil 1 UI (SelectCardsDecision) — put card into graveyard
 * - Surveil 1 UI — keep card on top of library (SelectCards + ReorderLibrary)
 */
test.describe('Rummaging Wizard', () => {
  test('surveil 1 — put card into graveyard', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Wizard Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Rummaging Wizard', tapped: false, summoningSickness: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
        ],
        library: ['Grizzly Bears', 'Mountain', 'Forest'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Rummaging Wizard's ability
    await p1.clickCard('Rummaging Wizard')
    await p1.selectAction('Surveil')

    // P2 must resolve the ability on the stack
    await p2.pass()

    // P1 sees the SelectCards decision — select the card for graveyard
    await p1.selectCardInDecision('Grizzly Bears')
    await p1.confirmSelection()

    // Graveyard should now have 1 card (Grizzly Bears)
    await p1.expectGraveyardSize(player1.playerId, 1)
  })

  test('surveil 1 — keep card on top of library', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Wizard Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Rummaging Wizard', tapped: false, summoningSickness: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
        ],
        library: ['Grizzly Bears', 'Mountain', 'Forest'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Rummaging Wizard's ability
    await p1.clickCard('Rummaging Wizard')
    await p1.selectAction('Surveil')

    // P2 must resolve the ability on the stack
    await p2.pass()

    // P1 sees the SelectCards decision — select nothing to keep card on top
    await p1.skipTargets()
    // P1 sees the ReorderLibrary decision for the single card — dismiss it
    await p1.dismissRevealedCards()

    // Graveyard should remain empty
    await p1.expectGraveyardSize(player1.playerId, 0)

    // Rummaging Wizard should now be tapped (it uses mana, not {T}, so check the lands)
    // Islands should be tapped after paying {2}{U}
    await p1.expectTapped('Island')
  })
})
