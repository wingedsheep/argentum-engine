import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Mind Rot.
 *
 * Card: Mind Rot ({2}{B}) — Sorcery
 * "Target opponent discards two cards."
 *
 * Covers: SelectCardsDecision / CardSelectionDecision UI (opponent discard)
 */
test.describe('Mind Rot', () => {
  test('target opponent discards two cards', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Mind Rot'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
      },
      player2: {
        hand: ['Grizzly Bears', 'Hill Giant', 'Mountain'],
        library: ['Forest'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Mind Rot (auto-targets the only opponent)
    await p1.clickCard('Mind Rot')
    await p1.selectAction('Cast Mind Rot')

    // Opponent resolves the spell
    await p2.resolveStack('Mind Rot')

    // Opponent sees card selection overlay — must choose 2 cards to discard
    await p2.selectCardInDecision('Grizzly Bears')
    await p2.selectCardInDecision('Hill Giant')
    await p2.confirmSelection()

    // Opponent should have 1 card remaining in hand
    await p2.expectHandSize(1)
    await p2.expectInHand('Mountain')

    await p2.screenshot('End state')
  })
})
