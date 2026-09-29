import { test, expect } from '../../fixtures/scenarioFixture'

test.describe('Triton Wavebreaker bestow', () => {
  test('offers a disabled bestow price when only the creature cast is affordable', async ({ createGame }) => {
    const { player1 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Triton Wavebreaker'],
        battlefield: [{ name: 'Island' }, { name: 'Grizzly Bears' }],
        library: ['Island'],
      },
      player2: { library: ['Forest'] },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    await p1.clickCard('Triton Wavebreaker')
    await expect(p1.page.getByRole('button', { name: /Cast Triton Wavebreaker/ })).toBeEnabled()
    await expect(p1.page.getByRole('button', { name: /Bestow Triton Wavebreaker/ })).toBeDisabled()
    await p1.expectInHand('Triton Wavebreaker')
    await p1.screenshot('Creature and unavailable bestow prices')
  })

  test('bestow chooses a creature on the battlefield and attaches the resolved Aura', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Triton Wavebreaker'],
        battlefield: [
          { name: 'Island' },
          { name: 'Island' },
          { name: 'Grizzly Bears', summoningSickness: false },
        ],
        library: ['Island'],
      },
      player2: { library: ['Forest'] },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    await p1.clickCard('Triton Wavebreaker')
    await expect(p1.page.getByRole('button', { name: /Cast Triton Wavebreaker/ })).toBeEnabled()
    await expect(p1.page.getByRole('button', { name: /Bestow Triton Wavebreaker/ })).toBeEnabled()
    await p1.selectAction('Bestow Triton Wavebreaker')
    await p1.selectTarget('Grizzly Bears')
    await p1.confirmTargets()
    await player2.gamePage.resolveStack('Triton Wavebreaker')
    await p1.expectStats('Grizzly Bears', '3/3')
    await p1.expectOnBattlefield('Triton Wavebreaker')
    await p1.expectNotInHand('Triton Wavebreaker')
    await p1.screenshot('Bestowed Wavebreaker attached to the 3/3 Bears')
  })
})
