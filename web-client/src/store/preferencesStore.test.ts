import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  fetchPreferences: vi.fn<() => Promise<unknown>>(),
  savePreferences: vi.fn<(p: unknown) => Promise<void>>(),
}))
vi.mock('@/api/account', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/account')>()),
  fetchPreferences: api.fetchPreferences,
  savePreferences: api.savePreferences,
}))

import { useAuthStore } from './authStore'
import {
  DEFAULT_PREFERENCES,
  parsePreferences,
  syncPreferences,
  usePreferences,
  type Preferences,
} from './preferencesStore'
import { Step } from '@/types/enums'

const signedIn = () =>
  useAuthStore.setState({
    status: 'authenticated',
    user: { id: 'u1', email: 'a@b.co', displayName: 'A', isAdmin: false, hidePresence: false },
  } as never)

describe('parsePreferences', () => {
  it('reads an empty or garbage document as the defaults', () => {
    expect(parsePreferences({})).toEqual(DEFAULT_PREFERENCES)
    expect(parsePreferences(null)).toEqual(DEFAULT_PREFERENCES)
    expect(parsePreferences('nope')).toEqual(DEFAULT_PREFERENCES)
  })

  it('reads the message alert switch, defaulting to on for documents written before it existed', () => {
    expect(parsePreferences({ display: { motion: 'reduce' } }).messages.alertsAway).toBe(true)
    expect(parsePreferences({ messages: { alertsAway: false } }).messages.alertsAway).toBe(false)
    expect(parsePreferences({ messages: { alertsAway: 'no' } }).messages.alertsAway).toBe(true)
  })

  it('keeps valid fields and drops invalid ones field by field', () => {
    const p = parsePreferences({
      gameplay: { priorityMode: 'stops', myTurnStops: ['UPKEEP', 'BOGUS', 'UPKEEP', 'UNTAP'], autoTap: 'yes' },
      battlefield: { lands: { groupFrom: 3, stackSize: 5 }, creatures: { groupFrom: 99 }, maxVisibleLayers: 0 },
      display: { motion: 'wild', hoverPreview: false },
    })
    expect(p.gameplay.priorityMode).toBe('stops')
    // Unknown steps and steps without a priority window are dropped; duplicates collapse.
    expect(p.gameplay.myTurnStops).toEqual([Step.UPKEEP])
    expect(p.gameplay.autoTap).toBe(true)
    expect(p.battlefield.lands).toEqual({ groupFrom: 3, stackSize: 5 })
    // Out-of-range numbers clamp rather than reset.
    expect(p.battlefield.creatures.groupFrom).toBe(10)
    expect(p.battlefield.maxVisibleLayers).toBe(1)
    expect(p.display).toEqual({ motion: 'system', hoverPreview: false })
  })

  it('normalizes a one-card stack: group-from 1 is 2, stack-size 1 is unlimited', () => {
    expect(parsePreferences({ battlefield: { other: { groupFrom: 1, stackSize: 1 } } }).battlefield.other)
      .toEqual({ groupFrom: 2, stackSize: 0 })
  })
})

describe('usePreferences', () => {
  beforeEach(() => {
    useAuthStore.setState({ status: 'anonymous', user: null } as never)
    usePreferences.getState().replace(DEFAULT_PREFERENCES)
    api.fetchPreferences.mockReset()
    api.savePreferences.mockReset().mockResolvedValue()
  })

  it('update merges into one section and stamps the change', () => {
    usePreferences.getState().update('battlefield', { lands: { groupFrom: 4, stackSize: 0 } })
    const { prefs } = usePreferences.getState()
    expect(prefs.battlefield.lands.groupFrom).toBe(4)
    expect(prefs.battlefield.creatures).toEqual(DEFAULT_PREFERENCES.battlefield.creatures)
    expect(prefs.updatedAt).toBeGreaterThan(0)
  })

  it('a guest never talks to the account', async () => {
    await syncPreferences()
    expect(api.fetchPreferences).not.toHaveBeenCalled()
  })

  it('adopts the account copy when it is newer', async () => {
    const remote: Preferences = {
      ...DEFAULT_PREFERENCES,
      updatedAt: 5_000,
      gameplay: { ...DEFAULT_PREFERENCES.gameplay, autoTap: false },
    }
    usePreferences.getState().replace({ ...DEFAULT_PREFERENCES, updatedAt: 1_000 })
    api.fetchPreferences.mockResolvedValue(remote)
    signedIn() // signing in starts the sync; this awaits the same one
    await syncPreferences()
    expect(usePreferences.getState().prefs.gameplay.autoTap).toBe(false)
    expect(api.savePreferences).not.toHaveBeenCalled()
  })

  it('pushes the browser copy when it is newer', async () => {
    const local: Preferences = { ...DEFAULT_PREFERENCES, updatedAt: 9_000, display: { motion: 'reduce', hoverPreview: true } }
    usePreferences.getState().replace(local)
    api.fetchPreferences.mockResolvedValue({ ...DEFAULT_PREFERENCES, updatedAt: 2_000 })
    signedIn() // signing in starts the sync; this awaits the same one
    await syncPreferences()
    expect(api.savePreferences).toHaveBeenCalledWith(local)
    expect(usePreferences.getState().prefs.display.motion).toBe('reduce')
  })

  it('seeds an account that has never saved preferences', async () => {
    api.fetchPreferences.mockResolvedValue({})
    signedIn() // signing in starts the sync; this awaits the same one
    await syncPreferences()
    expect(api.savePreferences).toHaveBeenCalledTimes(1)
  })

  it('a failed fetch leaves the local copy standing', async () => {
    usePreferences.getState().replace({ ...DEFAULT_PREFERENCES, updatedAt: 3, gameplay: { ...DEFAULT_PREFERENCES.gameplay, followAction: false } })
    api.fetchPreferences.mockRejectedValue(new Error('offline'))
    signedIn() // signing in starts the sync; this awaits the same one
    await syncPreferences()
    expect(usePreferences.getState().prefs.gameplay.followAction).toBe(false)
  })
})
