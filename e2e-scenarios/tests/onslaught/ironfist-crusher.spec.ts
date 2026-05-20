import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Ironfist Crusher (Onslaught).
 *
 * Ironfist Crusher {4}{W}
 * Creature — Human Soldier 2/4
 * Ironfist Crusher can block any number of creatures.
 * Morph {3}{W}
 *
 * Covers: CanBlockAnyNumber (one blocker assigned to multiple attackers),
 *         combat damage from multiple blocked attackers.
 */
test.describe('Ironfist Crusher', () => {
  test('blocks two attackers at once and survives', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
          { name: 'Devoted Hero', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Ironfist Crusher' }],
        library: ['Plains'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to combat
    await p1.pass()
    await p1.attackAll()

    // Ironfist Crusher blocks both attackers
    await p2.declareBlocker('Ironfist Crusher', 'Glory Seeker')
    await p2.declareBlocker('Ironfist Crusher', 'Devoted Hero')
    await p2.confirmBlockers()

    // Attacker must order the attackers for damage assignment
    await p1.confirmBlockerOrder()

    // Combat damage: Glory Seeker (2/2) + Devoted Hero (1/2) = 3 damage to Ironfist Crusher (2/4)
    // Ironfist Crusher survives with 1 toughness remaining
    await p2.expectOnBattlefield('Ironfist Crusher')

    // No damage to defending player — all attackers were blocked
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })

  test('blocks three attackers and dies to combined damage', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
          { name: 'Devoted Hero', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Ironfist Crusher' }],
        library: ['Plains'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to combat
    await p1.pass()
    await p1.attackAll()

    // Ironfist Crusher blocks all three attackers
    await p2.declareBlocker('Ironfist Crusher', 'Glory Seeker')
    await p2.declareBlocker('Ironfist Crusher', 'Grizzly Bears')
    await p2.declareBlocker('Ironfist Crusher', 'Devoted Hero')
    await p2.confirmBlockers()

    // Attacker must order the attackers for damage assignment
    await p1.confirmBlockerOrder()

    // Combat damage: Glory Seeker (2) + Grizzly Bears (2) + Devoted Hero (1) = 5 damage
    // Ironfist Crusher (2/4) takes 5 damage and dies
    await p2.expectNotOnBattlefield('Ironfist Crusher')

    // No damage to defending player — all attackers were blocked
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })

  /**
   * Regression: two Ironfist Crushers each block both attackers; one attacker
   * (Spurred Wolverine 3/2) has *less power than a Crusher's toughness*, so
   * per CR 510.1d the attacker may assign all 3 to the first blocker and 0 to
   * the second (no later blocker gets damage, so falling below lethal on the
   * first is legal).
   *
   * Pre-fix the engine emitted these edges with `minimum = lethal (4)` and
   * `maximum = power (3)` — unsatisfiable — and the validator rejected the
   * default with "Edge ...: amount 3 below minimum 4". The fix: blocker edges
   * have `minimum = 0`; CR 510.1d is enforced relationally in the validator
   * via `lethalThreshold`.
   */
  test('two Crushers each blocking both Silvos and Spurred Wolverine: defaults submit cleanly', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Silvos, Rogue Elemental', tapped: false, summoningSickness: false }, // 8/5 trample
          { name: 'Spurred Wolverine', tapped: false, summoningSickness: false },       // 3/2
        ],
        library: ['Mountain', 'Mountain'],
      },
      player2: {
        battlefield: [
          { name: 'Ironfist Crusher' }, // 2/4 can-block-any-number
          { name: 'Ironfist Crusher' }, // 2/4 can-block-any-number
        ],
        library: ['Plains', 'Plains'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    await p1.pass()
    await p1.attackAll()

    // Two Crushers; both block both attackers (4 block-edges total).
    // Drag each Crusher to each attacker. Resolve locators at click-time via
    // .nth() because the GamePage.declareBlocker helper uses .first() which
    // would hit the same Crusher twice.
    const crushers = p2.page.locator('img[alt="Ironfist Crusher"]')
    const silvos = p2.page.locator('img[alt="Silvos, Rogue Elemental"]').first()
    const wolverine = p2.page.locator('img[alt="Spurred Wolverine"]').first()

    await crushers.nth(0).dragTo(silvos, { force: true })
    await crushers.nth(0).dragTo(wolverine, { force: true })
    await crushers.nth(1).dragTo(silvos, { force: true })
    await crushers.nth(1).dragTo(wolverine, { force: true })
    await p2.screenshot('Both Crushers blocking both attackers')

    await p2.confirmBlockers()
    await p2.screenshot('Blocks confirmed')

    // The combat resolution board opens on the attacker side. Submit the
    // engine-supplied defaults via "Confirm Damage". Pre-fix the engine
    // emitted unsatisfiable bounds for Spurred Wolverine's edges (min=4 max=3)
    // and the validator rejected the default with "amount 3 below minimum 4".
    const confirmDamage = p1.page.getByRole('button', { name: /^Confirm Damage/ })
    await confirmDamage.waitFor({ state: 'visible', timeout: 15_000 })
    await confirmDamage.click()
    await p1.screenshot('Confirm damage')

    // Outcome (when the bug is fixed): Silvos's 8 power must be split as 4+4
    // across the two Crushers (lethal to each, no trample) — this alone kills
    // both Crushers. Wolverine's 3 power is assigned on top in damage order
    // but is irrelevant to lethality. Defender stays at 20 because Silvos
    // couldn't trample past the two 4-toughness blockers.
    await p2.expectNotOnBattlefield('Ironfist Crusher')
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })
})
