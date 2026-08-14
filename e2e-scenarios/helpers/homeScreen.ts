import { expect, type Page } from '@playwright/test'

/**
 * Driving the landing screen's play wizard.
 *
 * These live in one place because the landing screen has now been restructured twice — the specs
 * first typed into a placeholder that had been renamed, then clicked `mode-preset-draft-sealed`
 * cards that no longer exist — and each time every tournament spec had to be edited. A third
 * restructure should be one edit here.
 *
 * The wizard asks three questions (`components/ui/PlayWizard.tsx`): who is playing, what with, and
 * how it is played. A step whose options collapse to one answer is skipped, so `createLobby` only
 * clicks the shape tile when it is actually rendered. There is no sub-shape step — which sealed or
 * draft shape it is is a lobby sub-option, set on the lobby's own `Sealed shape` / `Draft shape` row.
 *
 * Each answer is also a URL segment (`components/ui/wizardUrl.ts`), so a spec that wants a specific
 * selection without clicking can `page.goto('/play/group/draft/bracket')` instead.
 */

export type Roster = 'solo' | 'friend' | 'group'
export type Cards = 'bring-a-deck' | 'random' | 'momir' | 'sealed' | 'draft'
export type Shape = 'one-game' | 'bracket' | 'free-for-all' | 'two-headed-giant' | 'team-vs-team'

/** The placeholder on the join field, so specs don't each hard-code the copy. */
export const JOIN_PLACEHOLDER = 'Been invited? Paste the code here'

export interface WizardChoice {
  roster: Roster
  cards: Cards
  shape?: Shape
}

/** Wait for the landing screen to be interactive (step 1 rendered). */
export async function waitForHome(page: Page): Promise<void> {
  await expect(page.getByTestId('wizard-roster-solo')).toBeVisible({ timeout: 10000 })
}

/** Enter a name and land on the wizard. */
export async function enterName(page: Page, name: string): Promise<void> {
  await page.goto('/')
  await page.getByPlaceholder('Your name').fill(name)
  await page.getByRole('button', { name: 'Continue' }).click()
  await waitForHome(page)
}

/** Walk the wizard and create the lobby. Returns once the invite code is on screen. */
export async function createLobby(page: Page, choice: WizardChoice): Promise<string> {
  await page.getByTestId(`wizard-roster-${choice.roster}`).click()
  await page.getByTestId(`wizard-cards-${choice.cards}`).click()

  // Step 3 is only rendered when there is more than one reachable shape.
  if (choice.shape) {
    const tile = page.getByTestId(`wizard-shape-${choice.shape}`)
    if (await tile.isVisible({ timeout: 2000 }).catch(() => false)) await tile.click()
  }

  // No seat step: the lobby opens at the cap its shape allows and people join until it is full.
  await page.getByTestId('wizard-create').click()
  await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
  const lobbyId = await page.getByTestId('invite-code').textContent() ?? ''
  expect(lobbyId).toBeTruthy()
  return lobbyId
}

/**
 * Launch a saved setup from the rail above the wizard.
 *
 * The rail only exists once something has been played — a fresh browser profile sees the three
 * questions and nothing else — so a spec that wants this has to create and start a lobby first.
 * `name` is the setup's name lower-cased and hyphenated; the auto-captured slot is `'last'`.
 */
export async function launchSetup(page: Page, name: string): Promise<string> {
  await page.getByTestId(`setup-chip-${name}`).click()
  await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
  return await page.getByTestId('invite-code').textContent() ?? ''
}

/** Save the current lobby as a named setup, from the host's ★ button. */
export async function saveSetup(page: Page, name: string): Promise<void> {
  await page.getByTestId('save-setup').click()
  await page.getByLabel('Setup name').fill(name)
  await page.getByTestId('confirm-save-setup').click()
}

/** Open one of the lobby's settings groups by name (`cards`, `rules`, `table`, `event`, `lobby`). */
export async function openSettingsGroup(page: Page, group: string): Promise<void> {
  const panel = page.getByTestId(`settings-group-${group}`)
  if ((await panel.getAttribute('data-open')) !== 'true') {
    await page.getByTestId(`settings-group-toggle-${group}`).click()
  }
  await expect(panel).toHaveAttribute('data-open', 'true')
}

/** Join an existing lobby by code from the landing screen. */
export async function joinLobby(page: Page, lobbyId: string): Promise<void> {
  await page.getByPlaceholder(JOIN_PLACEHOLDER).fill(lobbyId)
  await page.getByRole('button', { name: 'Join' }).click()
  await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
}

/** A group sealed bracket — what the tournament specs all want. */
export const GROUP_SEALED: WizardChoice = {
  roster: 'group',
  cards: 'sealed',
  shape: 'bracket',
}

/** A group booster draft bracket. */
export const GROUP_DRAFT: WizardChoice = {
  roster: 'group',
  cards: 'draft',
  shape: 'bracket',
}
