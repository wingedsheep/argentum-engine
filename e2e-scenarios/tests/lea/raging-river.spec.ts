import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(120_000)
test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 } })

test('Raging River splits creatures on the battlefield, previews a side, and highlights only legal blockers', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: {
      battlefield: [{ name: 'Raging River' }, { name: 'Hill Giant', summoningSickness: false }],
      library: ['Mountain', 'Mountain'],
    },
    player2: {
      battlefield: [{ name: 'Grizzly Bears' }, { name: 'Wall of Wood' }, { name: 'Giant Spider' }, { name: 'Flying Men' }],
      library: ['Forest', 'Forest'],
    },
    phase: 'COMBAT', step: 'DECLARE_ATTACKERS', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
    player2StopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
    player1OpponentStopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
    player2OpponentStopAtSteps: ['DECLARE_ATTACKERS', 'DECLARE_BLOCKERS'],
  })
  player1.page.setDefaultTimeout(15_000)
  player2.page.setDefaultTimeout(15_000)
  const p1 = player1.gamePage
  const p2 = player2.gamePage
  const defenderCard = (name: string) => player2.page.locator('[data-card-id]')
    .filter({ has: player2.page.getByRole('img', { name, exact: true }) })

  await p1.attackWith('Hill Giant')
  await p2.resolveStack('Raging River trigger')
  await expect(player2.page.getByText('Select creatures for the left pile; the rest form the right pile', { exact: true })).toBeVisible()
  // This is a battlefield decision, so cards stay on the board and are selected directly.
  await expect(defenderCard('Grizzly Bears')).toBeVisible()
  await p2.clickCard('Grizzly Bears')
  await expect(player2.page.getByRole('button', { name: 'Confirm (1)', exact: true })).toBeEnabled()
  await player2.page.getByRole('button', { name: 'Confirm (1)', exact: true }).click()

  await expect(player1.page.getByRole('heading', { name: 'Choose left or right for this attacking creature' })).toBeVisible()
  await expect(player1.page.getByRole('img', { name: 'Source: Hill Giant', exact: true })).toBeVisible()
  // The counts exclude Flying Men, which may block either side.
  await expect(player1.page.getByRole('button', { name: 'left (1)', exact: true })).toBeVisible()
  await expect(player1.page.getByRole('button', { name: 'right (2)', exact: true })).toBeVisible()
  await player1.page.getByRole('button', { name: 'left (1)', exact: true }).click()
  // A selected pile previews its members before the attacker commits to that side.
  await expect(player1.page.locator('img[alt="Grizzly Bears"]')).toHaveCount(2)
  await p1.screenshot('Raging River left pile preview for Hill Giant')
  await player1.page.getByRole('button', { name: 'Confirm', exact: true }).click()

  await player1.page.getByRole('button', { name: 'To Blockers', exact: true }).click()
  await p2.pass()
  await expect(player2.page.getByRole('button', { name: 'No Blocks', exact: true })).toBeVisible()
  // Move the pointer away so the normal valid-blocker color is stable, rather than its hover color.
  await player2.page.mouse.move(0, 0)
  await expect(defenderCard('Grizzly Bears')).toHaveCSS('border-top-color', 'rgb(0, 187, 255)')
  await expect(defenderCard('Flying Men')).toHaveCSS('border-top-color', 'rgb(0, 187, 255)')
  await expect(defenderCard('Wall of Wood')).not.toHaveCSS('border-top-color', 'rgb(0, 187, 255)')
  await expect(defenderCard('Giant Spider')).not.toHaveCSS('border-top-color', 'rgb(0, 187, 255)')
  const attacker = player2.page.locator('[data-card-id]')
    .filter({ has: player2.page.getByRole('img', { name: 'Hill Giant', exact: true }) })
  await expect(attacker.getByText('Evasion', { exact: true })).toBeVisible()
  await p2.declareBlocker('Grizzly Bears', 'Hill Giant')
  await expect(player2.page.getByRole('button', { name: /Confirm Blocks/ })).toBeEnabled()
  await p2.screenshot('Raging River selected-pile blocker assigned')
  await p2.confirmBlockers()
})
