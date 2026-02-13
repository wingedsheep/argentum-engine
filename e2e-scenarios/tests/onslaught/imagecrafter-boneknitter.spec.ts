import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test: Imagecrafter denies Boneknitter's regeneration by changing
 * the creature type in response.
 *
 * Setup:
 * - P1 controls Imagecrafter (1/1) and Aphetto Alchemist (1/2)
 * - P2 controls Boneknitter (1/1 Zombie) and has mana for its {1}{B} ability
 *
 * Flow:
 * 1. P2 attacks with Boneknitter
 * 2. P1 blocks with Aphetto Alchemist (1/2 — survives the 1 damage)
 * 3. P2 activates Boneknitter's "{1}{B}: Regenerate target Zombie" targeting itself
 * 4. P1 responds with Imagecrafter's "{T}: Target creature becomes type of your choice"
 *    targeting Boneknitter, choosing "Goblin"
 * 5. Imagecrafter's ability resolves first (LIFO) — Boneknitter becomes a Goblin
 * 6. Boneknitter's regenerate tries to resolve — target is no longer a Zombie → fizzles
 * 7. Combat damage kills Boneknitter (no regeneration shield)
 *
 * This tests Rule 608.2b: targets must still be legal when a spell or ability resolves.
 */
test.describe('Imagecrafter + Boneknitter interaction', () => {
  test('changing creature type causes regenerate to fizzle', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Defender',
      player2Name: 'Attacker',
      player1: {
        battlefield: [
          { name: 'Imagecrafter', tapped: false, summoningSickness: false },
          { name: 'Aphetto Alchemist', tapped: false, summoningSickness: false },
        ],
        library: ['Island'],
      },
      player2: {
        battlefield: [
          { name: 'Boneknitter', tapped: false, summoningSickness: false },
          { name: 'Swamp', tapped: false },
          { name: 'Swamp', tapped: false },
        ],
        library: ['Swamp'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
      // Stop P2 at declare blockers so they can activate Boneknitter's regen
      player2StopAtSteps: ['DECLARE_BLOCKERS'],
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // P2 passes main phase → advance to combat
    await p2.pass()

    // P2 attacks with Boneknitter
    await p2.attackWith('Boneknitter')

    // P1 has activated abilities (Imagecrafter, Alchemist) so won't auto-pass
    // P1 passes to advance to declare blockers step
    await p1.pass()

    // P1 blocks Boneknitter with Aphetto Alchemist
    await p1.declareBlocker('Aphetto Alchemist', 'Boneknitter')
    await p1.confirmBlockers()

    // P1 passes priority (clicks "Resolve combat damage") → P2 gets priority
    await p1.pass()

    // P2 activates Boneknitter's regenerate ability targeting itself
    // Force click — the combat blocking arrow overlay intercepts pointer events on Boneknitter
    await p2.page.locator('img[alt="Boneknitter"]').first().click({ force: true })
    await p2.selectAction('Regenerate')
    await p2.page.locator('img[alt="Boneknitter"]').first().click({ force: true })
    await p2.confirmTargets()

    // P1 gets priority (sees Boneknitter's ability on stack)
    // P1 responds with Imagecrafter's ability targeting Boneknitter
    await p1.clickCard('Imagecrafter')
    await p1.selectAction('becomes the creature type')
    // Force click — Playwright hit-test is blocked by overlay div during combat
    await p1.page.locator('img[alt="Boneknitter"]').first().click({ force: true })
    await p1.confirmTargets()

    // P2 must resolve Imagecrafter's ability (top of stack)
    await p2.pass()

    // Imagecrafter's ability resolves — P1 chooses "Goblin" (Boneknitter is no longer a Zombie)
    // The creature type picker has a searchable list pre-filled with "Zombie"
    // Clear the search, type "Goblin", wait for option to appear, select it, confirm
    const searchBox = p1.page.locator('input[type="text"]')
    await searchBox.fill('Goblin')
    const goblinBtn = p1.page.locator('button').filter({ hasText: 'Goblin' })
    await goblinBtn.first().waitFor({ state: 'visible', timeout: 5000 })
    await goblinBtn.first().click()
    await p1.page.getByRole('button', { name: 'Confirm' }).click()

    // After Imagecrafter's ability resolves, regen is still on stack
    // P2 auto-passes (own ability on top of stack), P1 gets priority
    // P1 passes → both passed → regen resolves (fizzles: target is no longer a Zombie)
    await p1.pass()

    // Stack is now empty, still at DECLARE_BLOCKERS step
    // Both players must pass to advance to COMBAT_DAMAGE step
    await p2.pass() // P2 passes (stopped at DECLARE_BLOCKERS via stopAtSteps)
    await p1.pass() // P1 passes (has Alchemist ability, chooses not to use it)

    // Combat damage: Boneknitter (1/1) deals 1 to Alchemist (1/2 → survives)
    //                Alchemist (1/2) deals 1 to Boneknitter (1/1 → dies, no regen shield)
    await p1.expectNotOnBattlefield('Boneknitter')
    await p1.expectOnBattlefield('Aphetto Alchemist')
    await p1.expectOnBattlefield('Imagecrafter')

    // No damage to either player (attacker was blocked)
    await p1.expectLifeTotal(player1.playerId, 20)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('Boneknitter dead — regeneration denied by type change')
  })
})
