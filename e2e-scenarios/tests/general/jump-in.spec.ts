import { test, expect, type Page } from '@playwright/test'
import { enterName, createLobby, joinLobby } from '../../helpers/homeScreen'

test.use({ channel: 'chrome' })

async function choosePack(page: Page, index = 0) {
  await page.locator('article').getByRole('button', { name: /^Choose / }).nth(index).click()
}

test('Jump In creates a J22 lobby, previews packs, reconnects, and plays against AI on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await enterName(page, 'Jump In Host')
  await createLobby(page, { roster: 'solo', cards: 'jump-in', shape: 'bracket' })
  await expect(page.getByTestId('lobby-axis-summary')).toContainText('Jump In')
  await expect(page.getByRole('button', { name: 'Remove Jumpstart 2022', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Start Game', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Choose your first theme', exact: true })).toBeVisible()
  await expect(page.locator('article')).toHaveCount(3)
  await page.locator('article').first().getByText('Explore this pack').click()
  await page.locator('article').first().locator('li summary').first().click()
  await expect(page.locator('article').first().locator('li details[open]')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/jump-in-mobile.png', fullPage: true })
  await choosePack(page)
  await expect(page.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
  const selected = await page.getByLabel('Chosen themes').textContent() ?? ''
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
  await expect(page.getByLabel('Chosen themes')).toHaveText(selected)
  await choosePack(page, 2)
  await expect(page.getByRole('button', { name: 'Keep Hand', exact: true })).toBeVisible({ timeout: 30000 })
  await expect(page.getByRole('button', { name: 'Submit Deck', exact: true })).toHaveCount(0)
})

test('friends choose privately and the completed deck waits for the other player', async ({ page, browser }) => {
  const context = await browser.newContext()
  const guest = await context.newPage()
  try {
    await enterName(page, 'Host')
    const code = await createLobby(page, { roster: 'friend', cards: 'jump-in', shape: 'bracket' })
    await page.getByTestId('axis-choice-sealed').click()
    await expect(page.getByRole('button', { name: 'Traditional sealed', exact: true })).toHaveAttribute('aria-pressed', 'true')
    await page.getByTestId('axis-choice-jump-in').click()
    await expect(page.getByTestId('lobby-axis-summary')).toContainText('Jump In')
    await enterName(guest, 'Friend')
    await joinLobby(guest, code)
    await page.getByRole('button', { name: 'Start Game', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Choose your first theme', exact: true })).toBeVisible()
    await page.screenshot({ path: 'test-results/jump-in-desktop.png', fullPage: true })
    await choosePack(page)
    await expect(page.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
    await choosePack(page)
    await expect(page.getByRole('heading', { name: 'Two themes. One deck. Let’s play.', exact: true })).toBeVisible()
    await expect(page.getByLabel('Player readiness')).toContainText('Choosing themes…')
    await expect(guest.getByRole('heading', { name: 'Choose your first theme', exact: true })).toBeVisible()
    await expect(guest.getByLabel('Chosen themes')).toHaveCount(0)
    await page.reload()
    await expect(page.getByLabel('Player readiness')).toBeVisible()
    await choosePack(guest)
    await expect(guest.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
    await choosePack(guest)
    await expect(page.getByRole('button', { name: 'Keep Hand', exact: true })).toBeVisible({ timeout: 30000 })
    await expect(guest.getByRole('button', { name: 'Keep Hand', exact: true })).toBeVisible({ timeout: 30000 })
  } finally { await context.close() }
})
