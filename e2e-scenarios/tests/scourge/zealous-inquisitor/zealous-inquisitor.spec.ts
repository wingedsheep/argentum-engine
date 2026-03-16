import { test } from '../../../fixtures/scenarioFixture'

/**
 * E2E test: Zealous Inquisitor's activated ability redirects combat damage.
 *
 * Setup:
 * - P1 controls Zealous Inquisitor (2/2) + 2 Plains
 * - P2 controls Glory Seeker (2/2) + Elvish Pioneer (1/1)
 *
 * Flow:
 * 1. P2 attacks with Glory Seeker (2/2)
 * 2. P1 blocks with Zealous Inquisitor
 * 3. After blockers, P1 activates {1}{W} ability targeting Elvish Pioneer
 * 4. Ability resolves — next 1 damage to Inquisitor is redirected to Elvish Pioneer
 * 5. Combat damage: Glory Seeker deals 2 to Inquisitor, but 1 is redirected to Elvish Pioneer
 *    → Elvish Pioneer (1/1) takes 1 damage → dies
 *    → Zealous Inquisitor (2/2) takes 1 damage → survives
 *    → Glory Seeker (2/2) takes 2 damage from Inquisitor → dies
 */
test('redirect 1 combat damage to target creature', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Inquisitor',
    player2Name: 'Attacker',
    player1: {
      battlefield: [
        { name: 'Zealous Inquisitor', tapped: false, summoningSickness: false },
        { name: 'Plains', tapped: false },
        { name: 'Plains', tapped: false },
      ],
      library: ['Plains'],
    },
    player2: {
      battlefield: [
        { name: 'Glory Seeker', tapped: false, summoningSickness: false },
        { name: 'Elvish Pioneer', tapped: false, summoningSickness: false },
      ],
      library: ['Mountain'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 2,
  })

  const p1 = player1.gamePage
  const p2 = player2.gamePage

  // Both players pass main phase → combat
  await p2.pass()
  await p1.pass()

  // P2 attacks with Glory Seeker
  await p2.attackWith('Glory Seeker')

  // P1 has activated abilities so won't auto-pass — pass to advance to declare blockers
  await p1.pass()

  // P1 blocks Glory Seeker with Zealous Inquisitor
  await p1.declareBlocker('Zealous Inquisitor', 'Glory Seeker')
  await p1.confirmBlockers()

  // After blockers confirmed, P1 gets priority before combat damage
  // P1 activates Zealous Inquisitor's ability targeting Elvish Pioneer
  // Force click due to combat overlay on the Inquisitor
  await p1.page.locator('img[alt="Zealous Inquisitor"]').first().click({ force: true })
  await p1.selectAction('damage that would be dealt to')
  await p1.selectTarget('Elvish Pioneer')
  await p1.confirmTargets()

  // P2 resolves the ability — after this, combat damage proceeds automatically
  await p2.pass()

  // Results: Elvish Pioneer dies (1 redirected damage), Glory Seeker dies (2 from Inquisitor),
  // Zealous Inquisitor survives (only took 1 of the 2 damage)
  await p1.expectOnBattlefield('Zealous Inquisitor')
  await p1.expectNotOnBattlefield('Glory Seeker')
  await p1.expectNotOnBattlefield('Elvish Pioneer')
})
