import { test, expect, type Page } from '@playwright/test'
import { enterName } from '../../helpers/homeScreen'

test.use({ channel: 'chrome' })

async function selectJumpstart(page: Page) {
  await enterName(page, 'Jumpstart Host')
  await page.getByTestId('wizard-roster-group').click()
  await page.getByTestId('wizard-cards-sealed').click()
  await page.getByTestId('wizard-shape-bracket').click()
  await page.getByTestId('wizard-create').click()
  await expect(page.getByTestId('invite-code')).toBeVisible()
  await page.getByRole('button', { name: 'Remove Lorwyn Eclipsed', exact: true }).click()
  await page.getByRole('button', { name: 'Choose sets', exact: true }).click()
  await page.getByPlaceholder('Search sets by name or code…').fill('JMP')
  await page.getByText('Jumpstart', { exact: true }).click()
  await page.getByRole('button', { name: '×', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Choose themed packs', exact: true })).toHaveAttribute('aria-pressed', 'true')
}

test('Jumpstart defaults on only for the single set and preserves a traditional-format preference', async ({ page }) => {
  await selectJumpstart(page)
  await page.getByRole('button', { name: 'Traditional sealed', exact: true }).click()
  await page.reload()
  await expect(page.getByRole('button', { name: 'Traditional sealed', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await page.getByTestId('axis-choice-draft').click()
  await expect(page.getByRole('button', { name: 'Traditional draft', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await page.getByRole('button', { name: 'Choose themed packs', exact: true }).click()
  await page.getByRole('button', { name: '+ Add set', exact: true }).click()
  await page.getByPlaceholder('Search sets by name or code…').fill('M21')
  await page.getByText('Core Set 2021', { exact: true }).click()
  await page.getByRole('button', { name: '×', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Choose themed packs', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Remove Core Set 2021', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Choose themed packs', exact: true })).toHaveAttribute('aria-pressed', 'true')
})

test('choose two exact packs, reconnect between picks, and play against the AI', async ({ page }) => {
  await selectJumpstart(page)
  await page.getByRole('button', { name: '+ Add AI', exact: true }).click()
  await page.getByRole('button', { name: 'Start Game', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Choose your first theme', exact: true })).toBeVisible()
  await expect(page.getByText('Explore this pack', { exact: false })).toHaveCount(3)
  await page.getByText('Explore this pack', { exact: false }).first().click()
  await expect(page.locator('article details[open]').first()).toBeVisible()
  await page.locator('article').getByRole('button', { name: /^Choose / }).first().click()
  await expect(page.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
  const selected = await page.getByLabel('Chosen themes').textContent() ?? ''
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Choose your second theme', exact: true })).toBeVisible()
  await expect(page.getByLabel('Chosen themes')).toHaveText(selected)
  await page.locator('article').getByRole('button', { name: /^Choose / }).last().click()
  await expect(page.getByRole('button', { name: 'Keep Hand', exact: true })).toBeVisible({ timeout: 30000 })
  await expect(page.getByRole('button', { name: 'Submit Deck', exact: true })).toHaveCount(0)
})
