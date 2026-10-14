import { test, expect } from '../../fixtures/scenarioFixture'

test('Nuka-Nuke Launcher attack installs a rad watcher on the defending player, badged until their next turn ends', async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Raider', player2Name: 'Settler',
    player1: {
      battlefield: [
        { name: 'Grizzly Bears' },
        { name: 'Nuka-Nuke Launcher', attachedTo: 'Grizzly Bears' },
      ],
      library: ['Forest', 'Forest', 'Forest'],
    },
    player2: { library: ['Forest', 'Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['DECLARE_ATTACKERS'],
  })
  const p1 = player1.gamePage
  const page = player1.page

  await p1.pass()
  await p1.attackWith('Grizzly Bears')
  await player2.gamePage.resolveStack('Nuka-Nuke Launcher trigger')

  const badge = page.locator('span', { hasText: /^Nuka-Nuke Launcher$/ }).first()
  await expect(badge).toBeVisible({ timeout: 10_000 })
  await badge.hover()
  await expect(page.getByText(/Whenever Settler casts a spell/)).toBeVisible()
  await expect(page.getByText(/until the end of Settler's next turn/i).first()).toBeVisible()
  await p1.screenshot('Nuka-Nuke Launcher watcher badge names the defending player and its end')
})
