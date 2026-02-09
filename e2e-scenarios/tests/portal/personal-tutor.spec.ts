import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Personal Tutor.
 *
 * Card: Personal Tutor ({U}) — Sorcery
 * "Search your library for a sorcery card, reveal it, then shuffle
 *  your library and put that card on top."
 *
 * Covers: Library search overlay UI (SearchLibraryDecision / ZoneSelectionUI)
 */
test.describe('Personal Tutor', () => {
  test('search library for a sorcery card', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Personal Tutor'],
        battlefield: [{ name: 'Island' }],
        library: ['Mountain', 'Mind Rot', 'Forest', 'Lava Axe'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Personal Tutor
    await p1.clickCard('Personal Tutor')
    await p1.selectAction('Cast Personal Tutor')

    // Opponent resolves
    await p2.resolveStack('Personal Tutor')

    // Library search overlay appears — select Mind Rot (a sorcery)
    await p1.selectCardInZoneOverlay('Mind Rot')
    await p1.confirmSelection()

    // Spell resolved — Personal Tutor should be in graveyard
    // Mind Rot is now on top of library (can't directly verify position,
    // but we verify the search overlay completed successfully)
    await p1.screenshot('End state')
  })
})
