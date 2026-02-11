import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Cruel Revival.
 *
 * Card: Cruel Revival (4B) — Instant
 * "Destroy target non-Zombie creature. It can't be regenerated.
 *  Return up to one target Zombie card from your graveyard to your hand."
 *
 * Mirrors: CruelRevivalScenarioTest.kt
 */
test.describe('Cruel Revival', () => {
  test('destroys creature and returns zombie from graveyard to hand', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Caster',
      player1: {
        battlefield: [{ name: 'Hill Giant' }],
      },
      player2: {
        hand: ['Cruel Revival'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
        graveyard: ['Gluttonous Zombie'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Cruel Revival
    await p2.clickCard('Cruel Revival')
    await p2.selectAction('Cast Cruel Revival')

    // Step 1/2: select non-Zombie creature target on battlefield, then confirm
    await p2.selectTarget('Hill Giant')
    await p2.confirmTargets()

    // Step 2/2: targeting step modal shows graveyard zombie — click card then confirm
    await p2.selectTargetInStep('Gluttonous Zombie')
    await p2.confirmTargets()

    // Spell auto-resolves (opponent has no responses)

    // Verify: Hill Giant is destroyed (not on battlefield)
    await p2.expectNotOnBattlefield('Hill Giant')

    // Verify: Gluttonous Zombie returned to caster's hand
    await p2.expectInHand('Gluttonous Zombie')

    await p2.screenshot('End state')
  })

  test('destroys creature when no zombie target is available', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Caster',
      player1: {
        battlefield: [{ name: 'Hill Giant' }],
      },
      player2: {
        hand: ['Cruel Revival'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Cruel Revival
    await p2.clickCard('Cruel Revival')
    await p2.selectAction('Cast Cruel Revival')

    // Step 1/2: select non-Zombie creature target on battlefield, then confirm
    await p2.selectTarget('Hill Giant')
    await p2.confirmTargets()

    // Step 2/2: no zombies in graveyard — confirm with 0 targets selected
    await p2.confirmTargets()

    // Spell auto-resolves (opponent has no responses)

    // Verify: Hill Giant is destroyed
    await p2.expectNotOnBattlefield('Hill Giant')

    await p2.screenshot('End state')
  })
})
