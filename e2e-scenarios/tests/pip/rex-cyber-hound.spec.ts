import { test, expect } from '../../fixtures/scenarioFixture'
import { exile } from '../../helpers/selectors'

test('Rex exiles a creature card with a brain counter and borrows its activated ability', async ({ createGame }, testInfo) => {
  test.setTimeout(150_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Rex You', player2Name: 'Rex Opponent',
    player1: {
      battlefield: [{ name: 'Rex, Cyber-Hound' }, { name: 'Mountain' }],
      library: ['Island', 'Island', 'Island'],
    },
    player2: { graveyard: ['Shivan Dragon'], library: ['Island', 'Island', 'Island', 'Island'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, player1StopAtSteps: ['POSTCOMBAT_MAIN'],
  })
  const you = player1.gamePage
  // Pass priority on whichever side holds it until the stack is empty.
  const resolveAll = async () => {
    const p1 = player1.page.locator('button[data-learn="pass"]')
    const p2 = player2.page.locator('button[data-learn="pass"]')
    const stack = player1.page.getByText(/STACK \(\d+\)/)
    await expect(async () => {
      if (await p1.isEnabled().catch(() => false)) await p1.click({ timeout: 1_000 }).catch(() => {})
      if (await p2.isEnabled().catch(() => false)) await p2.click({ timeout: 1_000 }).catch(() => {})
      await expect(stack).toBeHidden({ timeout: 1_000 })
    }).toPass({ timeout: 30_000 })
  }

  // Combat damage gives {E}{E} (and mills the opponent two Islands).
  await you.pass()
  await you.attackWith('Rex, Cyber-Hound')
  await player1.page.getByText('Rex, Cyber-Hound trigger').waitFor({ state: 'visible', timeout: 15_000 })
  await resolveAll()
  await you.expectLifeTotal(player2.playerId, 18)
  await you.expectGraveyardSize(player2.playerId, 3)

  // Postcombat main: pay {E}{E}, choose the Sorcerer in the opponent's graveyard.
  await expect(async () => {
    await you.clickCard('Rex, Cyber-Hound')
    await expect(player1.page.getByRole('button', { name: /with a brain counter/i }).first()).toBeVisible({ timeout: 2_000 })
  }).toPass({ timeout: 30_000 })
  await player1.page.getByRole('button', { name: /with a brain counter/i }).first().click()
  await you.selectCardInZoneOverlay('Shivan Dragon')
  const confirm = player1.page.getByRole('button', { name: /Confirm/ }).first()
  if (await confirm.isVisible()) await confirm.click()
  await resolveAll()
  await you.expectGraveyardSize(player2.playerId, 2)

  // The exile browser badges the brain counter.
  await player1.page.locator(exile(player2.playerId)).first().click()
  const badge = player1.page.getByTitle('Brain counter')
  await expect(badge).toBeVisible()
  await expect(badge).toContainText('1')
  await player1.page.mouse.move(0, 0)
  await testInfo.attach('exile-browser-brain-counter', { body: await player1.page.screenshot({ path: testInfo.outputPath('exile-browser-brain-counter.png') }), contentType: 'image/png' })
  await player1.page.keyboard.press('Escape')
  const close = player1.page.getByRole('button', { name: /Close/ }).first()
  if (await close.isVisible()) await close.click()

  // Rex now offers the Dragon's {R}: +1/+0, even though Rex is tapped from attacking.
  await you.clickCard('Rex, Cyber-Hound')
  const firebreathing = player1.page.getByRole('button', { name: /\+1\/\+0/ }).first()
  await expect(firebreathing).toBeVisible()
  await testInfo.attach('rex-borrowed-ability', { body: await player1.page.screenshot({ path: testInfo.outputPath('rex-borrowed-ability.png') }), contentType: 'image/png' })
  await firebreathing.click()
  await resolveAll()
  await you.expectStats('Rex, Cyber-Hound', '3/2')
})
