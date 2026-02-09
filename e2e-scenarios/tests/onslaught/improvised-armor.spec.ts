import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Improvised Armor.
 *
 * Card: Improvised Armor ({3}{W}) — Enchantment — Aura
 * "Enchant creature. Enchanted creature gets +2/+5."
 * Cycling {3}
 *
 * Covers: Aura targeting UI (cast enchantment → select creature target → confirm)
 */
test.describe('Improvised Armor', () => {
  test('cast aura targeting a creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Improvised Armor'],
        battlefield: [
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Cast Improvised Armor targeting Glory Seeker
    await p1.clickCard('Improvised Armor')
    await p1.selectAction('Cast Improvised Armor')

    // Target Glory Seeker and confirm (aura targeting uses ChooseTargetsDecision)
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // Both players auto-pass (p2 has no actions), spell resolves

    // Glory Seeker should now be 4/7 (base 2/2 + 2/+5 from aura)
    await p1.expectStats('Glory Seeker', '4/7')

    // Improvised Armor should be on the battlefield as an enchantment
    await p1.expectOnBattlefield('Improvised Armor')

    await p1.screenshot('End state')
  })
})
