import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Gustcloak Savior.
 *
 * Card: Gustcloak Savior ({4}{W}) — Creature — Bird Soldier (3/4, Flying)
 * "Whenever a creature you control becomes blocked, you may untap that creature
 *  and remove it from combat."
 *
 * Mirrors: GustcloakSaviorTest.kt
 */
test.describe('Gustcloak Savior', () => {
  test('accepting the trigger untaps and removes blocked creature from combat', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Gustcloak Savior', tapped: false, summoningSickness: false },
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
        ],
        library: ['Plains'],
      },
      player2: {
        battlefield: [{ name: 'Hill Giant' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Grizzly Bears (Gustcloak Savior stays back)
    await p1.attackWith('Grizzly Bears')

    // Defender blocks Grizzly Bears with Hill Giant
    await p2.declareBlocker('Hill Giant', 'Grizzly Bears')
    await p2.confirmBlockers()

    // Gustcloak Savior's trigger fires — P2 must resolve the stack item
    await p2.resolveStack('Gustcloak Savior trigger')

    // P1 chooses yes — untap Grizzly Bears and remove from combat
    await p1.answerYes()

    // Both creatures should survive (no combat damage dealt)
    await p1.expectOnBattlefield('Grizzly Bears')
    await p1.expectOnBattlefield('Hill Giant')
    await p1.expectOnBattlefield('Gustcloak Savior')

    // No damage to either player
    await p1.expectLifeTotal(player1.playerId, 20)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state — both creatures alive')
  })

  test('declining the trigger leaves creature in combat to die', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Gustcloak Savior', tapped: false, summoningSickness: false },
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
        ],
        library: ['Plains'],
      },
      player2: {
        battlefield: [{ name: 'Hill Giant' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Grizzly Bears
    await p1.attackWith('Grizzly Bears')

    // Defender blocks
    await p2.declareBlocker('Hill Giant', 'Grizzly Bears')
    await p2.confirmBlockers()

    // Gustcloak Savior's trigger fires — P2 must resolve the stack item
    await p2.resolveStack('Gustcloak Savior trigger')

    // P1 declines — Grizzly Bears stays in combat
    await p1.answerNo()

    // Grizzly Bears (2/2) vs Hill Giant (3/3) — Bears die
    await p1.expectNotOnBattlefield('Grizzly Bears')
    await p1.expectOnBattlefield('Hill Giant')
    await p1.expectOnBattlefield('Gustcloak Savior')

    // No damage to either player (attacker was blocked)
    await p1.expectLifeTotal(player1.playerId, 20)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state — Grizzly Bears dead')
  })
})
