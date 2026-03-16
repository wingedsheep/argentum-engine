import { test, expect } from '../../../fixtures/scenarioFixture'

/**
 * E2E test: Priority after activated ability during combat.
 *
 * Scenario:
 * - P1 has Daru Stinger (1/1 + 3 +1/+1 counters = 4/4) and Dive Bomber (2/2 flying)
 * - P2 has Needleshot Gourna (3/6 reach) with Lavamancer's Skill attached
 * - P1 attacks with Dive Bomber, P2 blocks with Needleshot Gourna
 * - P1 taps Daru Stinger to deal 3 damage to Needleshot Gourna
 * - P2 responds with Lavamancer's Skill to deal 2 damage to Daru Stinger
 * - Stack resolves LIFO, then combat damage finishes the fight
 *
 * Validates that P2 gets priority to respond after P1 activates an ability during combat.
 */
test.describe('Daru Stinger — combat priority after activated ability', () => {
  test('P2 gets priority to respond with Lavamancer\'s Skill after Daru Stinger activation', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Stinger Player',
      player2Name: 'Gourna Player',
      player1: {
        battlefield: [
          { name: 'Daru Stinger', tapped: false, summoningSickness: false, counters: { PLUS_ONE_PLUS_ONE: 3 } },
          { name: 'Dive Bomber', tapped: false, summoningSickness: false },
          { name: 'Plains', tapped: false },
        ],
        library: ['Plains'],
      },
      player2: {
        battlefield: [
          { name: 'Needleshot Gourna', tapped: false, summoningSickness: false },
          { name: "Lavamancer's Skill", attachedTo: 'Needleshot Gourna' },
        ],
        library: ['Mountain'],
      },
      phase: 'COMBAT',
      step: 'DECLARE_ATTACKERS',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // P1 declares Dive Bomber as attacker and confirms
    await p1.attackWith('Dive Bomber')

    // P2 has Lavamancer's Skill ability so gets priority after attackers — pass to blockers
    await p2.pass()

    // P2 blocks Dive Bomber with Needleshot Gourna (has reach)
    await p2.declareBlocker('Needleshot Gourna', 'Dive Bomber')
    await p2.confirmBlockers()

    // After blockers declared, P2 retains priority (has instant-speed responses) — pass
    await p2.pass()

    // P1 activates Daru Stinger's ability targeting Needleshot Gourna
    // Combat arrow overlay intercepts normal clicks, so use JS-level dispatchEvent
    await p1.page.locator('img[alt="Daru Stinger"]').first().dispatchEvent('click')
    const actionBtn = p1.page.locator('button').filter({ hasText: 'damage to target' }).first()
    if (await actionBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await actionBtn.click()
      await p1.selectTarget('Needleshot Gourna')
      await p1.confirmTargets()
    }
    // If action menu didn't appear, the ability auto-activated via the dispatchEvent click

    // Daru Stinger's ability is on the stack. P1 auto-passes.
    // P2 gets priority to respond — activate the tap ability granted to Needleshot Gourna.
    // (Lavamancer's Skill grants the ability to the creature, so click the creature)
    await p2.page.locator('img[alt="Needleshot Gourna"]').first().dispatchEvent('click')
    await p2.selectAction('damage to target')
    // Combat arrow overlay blocks clicks on opponent's Daru Stinger, use dispatchEvent
    await p2.page.locator('img[alt="Daru Stinger"]').first().dispatchEvent('click')
    await p2.confirmTargets()

    // Resolve the stack (LIFO):
    // 1. Lavamancer's Skill ability resolves — deals 1 damage to Daru Stinger (4/4, survives)
    await p1.resolveStack('Needleshot Gourna ability')
    // 2. Daru Stinger's ability resolves — deals 3 damage to Needleshot Gourna
    await p2.resolveStack('Daru Stinger ability')

    // Combat damage: Dive Bomber (2/2) vs Needleshot Gourna (3/6 with 3 damage)
    // Gourna takes 2 more (total 5 of 6 toughness — survives)
    // Dive Bomber takes 3 damage (2 toughness — dies)
    await p1.expectNotOnBattlefield('Dive Bomber')
    await p2.expectOnBattlefield('Needleshot Gourna')

    // Daru Stinger survives (1 damage on 4/4 — Gourna is not a Wizard so deals 1)
    await p1.expectOnBattlefield('Daru Stinger')
    await p1.expectTapped('Daru Stinger')

    await p1.screenshot('End state — priority was correctly given')
  })
})
