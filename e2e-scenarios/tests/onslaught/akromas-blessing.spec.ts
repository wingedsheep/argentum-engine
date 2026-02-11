import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Akroma's Blessing.
 *
 * Card: Akroma's Blessing ({2}{W}) — Instant
 * "Choose a color. Creatures you control gain protection from the
 *  chosen color until end of turn."
 * Cycling {W}
 *
 * Covers: ChooseColorDecisionUI (color selection buttons)
 */
test.describe("Akroma's Blessing", () => {
  test('cast and choose a color for protection', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ["Akroma's Blessing"],
        battlefield: [
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Glory Seeker' },
        ],
      },
      player2: {},
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Akroma's Blessing
    await p1.clickCard("Akroma's Blessing")
    await p1.selectAction("Cast Akroma's Blessing")

    // Spell auto-resolves (opponent has no responses)
    // Color selection overlay appears — choose Red
    await p1.selectManaColor('Red')

    // Spell resolved — Glory Seeker should still be on battlefield with protection
    await p1.expectOnBattlefield('Glory Seeker')

    await p1.screenshot('End state')
  })
})
