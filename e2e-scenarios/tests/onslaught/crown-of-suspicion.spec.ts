import { test, expect } from '../../fixtures/scenarioFixture'
import { cardByName } from '../../helpers/selectors'

/**
 * E2E browser tests for Crown of Suspicion.
 *
 * Card: Crown of Suspicion ({1}{B}) — Enchantment — Aura
 * "Enchant creature. Enchanted creature gets +2/-1.
 * Sacrifice Crown of Suspicion: Enchanted creature and other creatures that share
 * a creature type with it get +2/-1 until end of turn."
 *
 * Covers: Aura on opponent's creature — caster retains control and can sacrifice it.
 */
test.describe('Crown of Suspicion', () => {
  test('cast aura on opponent creature and sacrifice it', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Crown of Suspicion'],
        battlefield: [
          { name: 'Swamp' },
          { name: 'Swamp' },
        ],
        library: ['Swamp'],
      },
      player2: {
        battlefield: [
          { name: 'Grizzly Bears', summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Crown of Suspicion targeting opponent's Grizzly Bears
    await p1.clickCard('Crown of Suspicion')
    await p1.selectAction('Cast Crown of Suspicion')

    // Target Grizzly Bears on opponent's battlefield and confirm
    await p1.selectTarget('Grizzly Bears')
    await p1.confirmTargets()

    // Both players auto-pass (no responses), spell resolves

    // Grizzly Bears should have +2/-1 from static ability (2/2 → 4/1)
    await p1.expectStats('Grizzly Bears', '4/1')

    // Crown should be on the battlefield (controlled by caster)
    await p1.expectOnBattlefield('Crown of Suspicion')

    // Caster activates the sacrifice ability on Crown
    // Crown is rendered as a peek behind the enchanted creature — click the card container via JS
    await p1.page.locator(cardByName('Crown of Suspicion')).first().dispatchEvent('click')
    await p1.selectAction('Sacrifice this permanent')

    // P1 auto-passes (own ability on stack), P2 must resolve the ability
    await p2.pass()

    // Crown should be gone from the battlefield
    await p1.expectNotOnBattlefield('Crown of Suspicion')

    // Grizzly Bears should still have +2/-1 from sacrifice effect (2/2 → 4/1)
    await p1.expectStats('Grizzly Bears', '4/1')

    await p1.screenshot('End state')
  })
})
