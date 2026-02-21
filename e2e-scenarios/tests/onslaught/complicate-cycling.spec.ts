import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E stress test for complex LIFO stack interactions involving cycling triggers,
 * counterspells, and activated abilities.
 *
 * Tests that the engine correctly handles Last-In-First-Out resolution with
 * multiple interleaved triggers and responses across both players.
 *
 * Cards involved:
 * - Siege-Gang Commander: {1}{R}, Sacrifice a Goblin: Deal 2 damage to any target
 * - Skirk Prospector: Sacrifice a Goblin: Add {R} (mana ability)
 * - Renewed Faith: Cycling {1}{W}. When you cycle ~, you may gain 2 life.
 * - Astral Slide: Whenever a player cycles, you may exile target creature (returns at end step)
 * - Chain of Vapor: {U} — Return target nonland permanent to owner's hand
 * - Complicate: Cycling {2}{U}. When you cycle ~, you may counter target spell unless
 *   its controller pays {1}.
 * - Goblin Piledriver: Protection from blue
 */
test.describe('Complicate cycling trigger', () => {
  test('full LIFO stress test with cycling triggers, counter, and activated abilities', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Alice',
      player2Name: 'Bob',
      player1: {
        lifeTotal: 15,
        hand: ['Renewed Faith', 'Complicate'],
        battlefield: [
          { name: 'Astral Slide' },
          { name: 'Sparksmith', tapped: false, summoningSickness: false },
          { name: 'Plains', tapped: false },
          { name: 'Plains', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Mountain', tapped: false },
        ],
        library: ['Plains', 'Plains'],
      },
      player2: {
        lifeTotal: 15,
        hand: ['Chain of Vapor', 'Smother'],
        battlefield: [
          { name: 'Siege-Gang Commander', tapped: false, summoningSickness: false },
          { name: 'Goblin Piledriver', tapped: false, summoningSickness: false },
          { name: 'Skirk Prospector', tapped: false, summoningSickness: false },
          { name: 'Mountain', tapped: false },
          { name: 'Mountain', tapped: false },
          { name: 'Mountain', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
          { name: 'Island', tapped: false },
        ],
        library: ['Mountain', 'Island'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // =========================================================================
    // Step 1: Bob activates Siege-Gang Commander's ability
    // Cost: {1}{R}, Sacrifice a Goblin. Target: Sparksmith.
    // He sacrifices Skirk Prospector as the Goblin.
    // =========================================================================
    await p2.clickCard('Siege-Gang Commander')
    await p2.selectAction('2 damage')
    await p2.selectTarget('Skirk Prospector')
    await p2.confirmTargets()
    await p2.selectTarget('Sparksmith')
    await p2.confirmTargets()

    // =========================================================================
    // Step 2: Alice cycles Renewed Faith in response
    // Triggers: Astral Slide (may exile target creature) + Renewed Faith (may gain 2 life)
    // =========================================================================
    await p1.clickCard('Renewed Faith')
    await p1.selectAction('Cycle')

    // Astral Slide trigger: may exile — accept, target SGC
    await p1.answerYes()
    await p1.selectTarget('Siege-Gang Commander')
    await p1.confirmTargets()

    // Renewed Faith trigger goes on stack (may decided at resolution)

    // =========================================================================
    // Step 3: Bob casts Chain of Vapor targeting Astral Slide
    // =========================================================================
    await p2.clickCard('Chain of Vapor')
    await p2.selectAction('Cast')
    await p2.selectTarget('Astral Slide')
    await p2.confirmTargets()

    // =========================================================================
    // Step 4: Alice cycles Complicate in response
    // Triggers: Astral Slide (target Piledriver) + Complicate (target Chain of Vapor)
    // =========================================================================
    await p1.clickCard('Complicate')
    await p1.selectAction('Cycle')

    // Astral Slide trigger #2: may exile — accept, target Goblin Piledriver
    await p1.answerYes()
    await p1.selectTarget('Goblin Piledriver')
    await p1.confirmTargets()

    // Complicate trigger: may counter — accept, target Chain of Vapor on the stack
    await p1.answerYes()
    await p1.page.locator('img[alt="Chain of Vapor"]').first().click({ force: true })
    await p1.confirmTargets()

    // =========================================================================
    // Step 5: Resolve the stack (LIFO)
    //
    // Stack (top to bottom):
    // 6. Complicate trigger (targeting Chain of Vapor)
    // 5. Astral Slide trigger #2 (targeting Goblin Piledriver)
    // 4. Chain of Vapor (targeting Astral Slide)
    // 3. Renewed Faith trigger (may gain 2 life)
    // 2. Astral Slide trigger #1 (targeting SGC)
    // 1. SGC ability (targeting Sparksmith)
    // =========================================================================

    // 6. Complicate trigger resolves — Bob auto-pays {1}, Chain of Vapor stays
    await p2.resolveStack('Complicate trigger')

    // 5. Astral Slide trigger #2 → exile Goblin Piledriver
    // Bob has priority (active player) — passes
    await p2.resolveStack('Astral Slide trigger')
    // Alice has priority (Sparksmith available) — passes
    await p1.pass()
    await p1.expectNotOnBattlefield('Goblin Piledriver')

    // 4. Chain of Vapor resolves → bounce Astral Slide to Alice's hand
    // Chain of Vapor's copy ability: Alice may sacrifice a land to copy — decline
    await p1.selectAction('Decline')
    await p1.expectNotOnBattlefield('Astral Slide')
    await p1.expectInHand('Astral Slide')

    // 3. Renewed Faith trigger — P2 passes, P1 auto-passes, trigger resolves
    await p2.resolveStack('Renewed Faith trigger')
    // May gain 2 life — accept (15 → 17)
    await p1.answerYes()

    // 2. Astral Slide trigger #1 resolves → exile SGC
    // (trigger exists independently of source — Astral Slide was bounced but trigger persists)
    await p2.resolveStack('Astral Slide trigger')
    // Alice has Sparksmith (activated ability) — she must pass explicitly
    // After she passes, SGC ability also auto-resolves (no more responses)
    await p1.pass()
    await p2.expectNotOnBattlefield('Siege-Gang Commander')

    // 1. SGC ability resolved → dealt 2 damage to Sparksmith (1/1, dies)
    await p1.expectNotOnBattlefield('Sparksmith')
  })
})
