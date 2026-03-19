import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Walking Desecration.
 *
 * Walking Desecration — {2}{B} — Creature — Zombie — 1/1
 * {B}, {T}: Creatures of the creature type of your choice attack this turn if able.
 *
 * The ability is activated at instant speed on the opponent's turn to force their
 * creatures of a chosen type to attack. The "Must Attack" badge should be visible
 * to both players on affected creatures.
 */
test.describe('Walking Desecration — creature type must attack', () => {
  test('chosen creature type gets Must Attack badge and is forced to attack', async ({ createGame }) => {
    // P1 controls Walking Desecration. It's P2's turn.
    // P1 stops at P2's precombat main to activate the ability.
    const { player1, player2 } = await createGame({
      player1Name: 'Controller',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Walking Desecration', tapped: false, summoningSickness: false },
          { name: 'Swamp', tapped: false },
        ],
        library: ['Swamp'],
      },
      player2: {
        battlefield: [
          // Zombie — should be forced to attack
          { name: 'Vengeful Dead', tapped: false, summoningSickness: false },
          // Non-zombie — should NOT be forced
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
      // P1 stops on opponent's main phase to activate Walking Desecration
      player1OpponentStopAtSteps: ['PRECOMBAT_MAIN'],
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // P2 passes main phase priority → P1 gets priority (stopped via opponentStopAtSteps)
    await p2.pass()

    // P1 activates Walking Desecration's ability: {B}, {T}
    await p1.clickCard('Walking Desecration')
    await p1.selectAction('Creatures of the creature type')

    // P2 resolves the ability from the stack
    await p2.pass()

    // P1 chooses "Zombie" from the creature type picker
    const searchBox = p1.page.locator('input[type="text"]')
    await searchBox.fill('Zombie')
    const zombieBtn = p1.page.locator('button').filter({ hasText: 'Zombie' })
    await zombieBtn.first().waitFor({ state: 'visible', timeout: 5000 })
    await zombieBtn.first().click()
    await p1.page.getByRole('button', { name: 'Confirm' }).click()

    // Vengeful Dead (Zombie) should have the "Must Attack" badge — visible to P1
    await p1.expectBadge('Vengeful Dead', 'Must Attack')
    // Glory Seeker (Human Soldier) should NOT have the badge
    await p1.expectNoBadge('Glory Seeker', 'Must Attack')

    // Badge should also be visible to the opponent (P2)
    await p2.expectBadge('Vengeful Dead', 'Must Attack')
    await p2.expectNoBadge('Glory Seeker', 'Must Attack')

    // P1 auto-passes (no legal actions — Walking Desecration and Swamp both tapped)
    // P2 advances to combat — must attack with Vengeful Dead (Zombie)
    await p2.pass()
    await p2.attackWith('Vengeful Dead')

    // P1 has no untapped blockers (Walking Desecration is tapped) — auto-passes
    // Combat resolves — Vengeful Dead (3/2) deals 3 damage
    await p2.expectLifeTotal(player1.playerId, 17)

    await p1.screenshot('End state — Vengeful Dead forced to attack')
  })
})
