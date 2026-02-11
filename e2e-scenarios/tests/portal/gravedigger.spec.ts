import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Gravedigger's ETB triggered ability.
 *
 * Card: Gravedigger (3B) — 2/2 Creature - Zombie
 * "When Gravedigger enters the battlefield, you may return target creature card
 *  from your graveyard to your hand."
 *
 * Mirrors: GravediggerScenarioTest.kt
 */
test.describe('Gravedigger ETB trigger', () => {
  test('returns creature card from graveyard to hand when target is selected', async ({
    createGame,
  }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        hand: ['Gravedigger'],
        graveyard: ['Grizzly Bears'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
      },
      player2: {},
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Gravedigger from hand
    await p1.clickCard('Gravedigger')
    await p1.selectAction('Cast Gravedigger')

    // Auto-pass resolves spell → ETB trigger → graveyard targeting overlay appears
    // Select Grizzly Bears from the graveyard overlay
    await p1.selectCardInZoneOverlay('Grizzly Bears')
    await p1.confirmTargets()

    // Trigger is on the stack — opponent resolves
    await p2.resolveStack('Gravedigger trigger')

    // Verify: Gravedigger on battlefield, Grizzly Bears returned to hand
    await p1.expectOnBattlefield('Gravedigger')
    await p1.expectInHand('Grizzly Bears')

    await p1.screenshot('End state')
  })

  test('ability is optional - player can skip target selection', async ({ createGame }) => {
    const { player1 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        hand: ['Gravedigger'],
        graveyard: ['Grizzly Bears'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
      },
      player2: {},
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Cast Gravedigger
    await p1.clickCard('Gravedigger')
    await p1.selectAction('Cast Gravedigger')

    // Auto-pass resolves spell → graveyard targeting overlay appears
    // Decline the optional targeting
    await p1.skipTargets()

    // Verify: Gravedigger on battlefield, Grizzly Bears NOT returned
    await p1.expectOnBattlefield('Gravedigger')
    await p1.expectNotInHand('Grizzly Bears')

    await p1.screenshot('End state')
  })
})
