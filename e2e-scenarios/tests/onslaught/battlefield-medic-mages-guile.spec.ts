import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test: responding to an activated ability on the stack with an instant.
 *
 * Scenario:
 * - P2 attacks with Grizzly Bears (2/2), P1 blocks with Glory Seeker (2/2)
 * - P1 activates Battlefield Medic targeting Glory Seeker (prevent 1 damage — X=1 Cleric)
 * - P2 responds with Mage's Guile on Glory Seeker, giving it shroud
 * - Medic's ability fizzles (target now has shroud) → prevention doesn't apply
 * - Combat damage: Glory Seeker takes full 2 damage and dies
 *
 * Without Mage's Guile: Medic prevents 1 of 2 damage → Glory Seeker survives.
 * With Mage's Guile: Medic's ability fizzles → Glory Seeker takes 2 damage → dies.
 */
test.describe('Battlefield Medic — responding to activated ability with shroud', () => {
  test('instant giving shroud causes activated ability to fizzle', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Medic Player',
      player2Name: 'Attacker',
      player1: {
        battlefield: [
          { name: 'Battlefield Medic', tapped: false, summoningSickness: false },
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
        ],
        library: ['Plains'],
      },
      player2: {
        battlefield: [
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
        ],
        hand: ["Mage's Guile"],
        library: ['Island'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // P2 advances from main phase to combat
    await p2.pass()

    // P2 attacks with Grizzly Bears (2/2)
    await p2.attackAll()

    // P1 has priority after attackers declared (has instant-speed ability) — pass to blockers
    await p1.pass()

    // P1 blocks Grizzly Bears with Glory Seeker (2/2)
    await p1.declareBlocker('Glory Seeker', 'Grizzly Bears')
    await p1.confirmBlockers()

    // After blockers, P2 auto-passes (nothing on stack to respond to), P1 gets priority
    // P1 activates Battlefield Medic's {T} ability targeting Glory Seeker
    // X = 1 (Battlefield Medic is the only Cleric) — would prevent 1 of 2 damage
    await p1.clickCard('Battlefield Medic')
    await p1.selectAction('Prevent')
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // P1 auto-passes (ability goes on stack), P2 gets priority
    // P2 responds by casting Mage's Guile ({1}{U}) targeting Glory Seeker
    await p2.clickCard("Mage's Guile")
    await p2.selectAction("Cast Mage's Guile")
    await p2.selectTarget('Glory Seeker')
    await p2.confirmTargets()

    // Stack auto-resolves (neither player has responses):
    // 1. Mage's Guile resolves → Glory Seeker gains shroud
    // 2. Medic's ability resolves → target has shroud → ability fizzles
    // 3. Combat damage: Grizzly Bears (2/2) deals 2 to Glory Seeker (2/2) — lethal
    //    Glory Seeker (2/2) deals 2 to Grizzly Bears (2/2) — also lethal

    // Both creatures die — Glory Seeker was NOT saved because prevention fizzled
    await p1.expectNotOnBattlefield('Glory Seeker')
    await p1.expectNotOnBattlefield('Grizzly Bears')

    // Battlefield Medic survives (tapped, but not in combat)
    await p1.expectOnBattlefield('Battlefield Medic')
    await p1.expectTapped('Battlefield Medic')

    // No player damage (attacker was fully blocked)
    await p1.expectLifeTotal(player1.playerId, 20)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })
})
