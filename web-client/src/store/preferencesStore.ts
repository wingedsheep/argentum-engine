/**
 * Player preferences — how the table behaves and looks for this player: where the engine stops
 * instead of auto-passing, how identical permanents stack on the battlefield, motion, card preview.
 *
 * One document, kept in localStorage for everyone (guests included) under `argentum-preferences`,
 * and on the account too once signed in (`/api/auth/me/preferences`, stored verbatim by the server).
 * Unlike Learn progress there is nothing to union: {@link syncPreferences} keeps whichever copy was
 * changed last (`updatedAt`) and writes it to the other side.
 *
 * In-game toggles that used to persist on their own (the step-strip stops, auto-tap, follow the
 * action) now write here, so the preferences page and the table never disagree. The legacy keys are
 * read once to seed a browser that has no document yet.
 */
import { create } from 'zustand'
import { fetchPreferences, savePreferences } from '@/api/account'
import { useAuthStore } from '@/store/authStore'
import { Step } from '@/types/enums'
import type { PriorityModeValue } from '@/types/messages'

const STORAGE_KEY = 'argentum-preferences'
const LEGACY_AUTO_TAP_KEY = 'argentum-auto-tap'
const LEGACY_STOPS_KEY = 'argentum-stop-overrides'
const LEGACY_FOLLOW_KEY = 'argentum-follow-action'

/**
 * How identical permanents of one battlefield row collapse into stacks.
 *
 * [groupFrom] — the fewest identical permanents that form a stack; fewer stay side by side.
 * `0` turns stacking off for the row. [stackSize] — the most cards in one stack; a larger group
 * splits into several stacks of at most this many. `0` means no limit.
 */
export interface StackingRule {
  groupFrom: number
  stackSize: number
}

/** `system` follows the OS "reduce motion" setting; `reduce` always cuts animations short. */
export type MotionPreference = 'system' | 'reduce'

export interface Preferences {
  version: 1
  /** Epoch ms of the last change; the newer of the browser's and the account's copy wins a sync. */
  updatedAt: number
  gameplay: {
    /** Priority mode every game starts in (the in-game button still cycles it for that game). */
    priorityMode: PriorityModeValue
    /** Steps the engine always stops at on your own turn, on top of its automatic stops. */
    myTurnStops: Step[]
    /** Steps the engine always stops at on an opponent's turn. */
    opponentTurnStops: Step[]
    /** Pay mana costs automatically when you cast a spell or activate an ability. */
    autoTap: boolean
    /** Multiplayer: move the camera to the board where the action is. */
    followAction: boolean
  }
  battlefield: {
    lands: StackingRule
    creatures: StackingRule
    /** Planeswalkers, artifacts, enchantments, battles — everything that isn't a land or creature. */
    other: StackingRule
    /** How many overlapping card layers a stack draws before the rest hide behind its count badge. */
    maxVisibleLayers: number
  }
  display: {
    motion: MotionPreference
    /** Show the large card preview when hovering a card with a mouse. */
    hoverPreview: boolean
  }
}

/** Steps that hold a priority window — the ones a stop can be set on. */
export const STOPPABLE_STEPS: readonly Step[] = [
  Step.UPKEEP, Step.DRAW,
  Step.PRECOMBAT_MAIN,
  Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS,
  Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT,
  Step.POSTCOMBAT_MAIN,
  Step.END,
]

export const STACK_LIMITS = { groupFromMax: 10, stackSizeMax: 20, layersMin: 1, layersMax: 8 } as const

export const DEFAULT_PREFERENCES: Preferences = {
  version: 1,
  updatedAt: 0,
  gameplay: {
    priorityMode: 'auto',
    myTurnStops: [],
    opponentTurnStops: [],
    autoTap: true,
    followAction: true,
  },
  battlefield: {
    lands: { groupFrom: 2, stackSize: 0 },
    creatures: { groupFrom: 2, stackSize: 0 },
    other: { groupFrom: 2, stackSize: 0 },
    maxVisibleLayers: 4,
  },
  display: {
    motion: 'system',
    hoverPreview: true,
  },
}

// ----- Parsing (defensive: the document may come from an older or newer build) -----

const STEP_SET = new Set<string>(STOPPABLE_STEPS)
const MODES = new Set<string>(['auto', 'stops', 'fullControl'])
const MOTIONS = new Set<string>(['system', 'reduce'])

function obj(v: unknown): Record<string, unknown> {
  return v && typeof v === 'object' && !Array.isArray(v) ? (v as Record<string, unknown>) : {}
}
function bool(v: unknown, fallback: boolean): boolean {
  return typeof v === 'boolean' ? v : fallback
}
function int(v: unknown, fallback: number, min: number, max: number): number {
  return typeof v === 'number' && Number.isFinite(v) ? Math.min(max, Math.max(min, Math.round(v))) : fallback
}
function steps(v: unknown): Step[] {
  if (!Array.isArray(v)) return []
  return [...new Set(v.filter((s): s is Step => typeof s === 'string' && STEP_SET.has(s)))]
}
function stacking(v: unknown, fallback: StackingRule): StackingRule {
  const o = obj(v)
  const groupFrom = int(o.groupFrom, fallback.groupFrom, 0, STACK_LIMITS.groupFromMax)
  const stackSize = int(o.stackSize, fallback.stackSize, 0, STACK_LIMITS.stackSizeMax)
  return {
    // A "stack" of one card is no stack: 1 means the same as 2 here, and as unlimited there.
    groupFrom: groupFrom === 1 ? 2 : groupFrom,
    stackSize: stackSize === 1 ? 0 : stackSize,
  }
}

/** Read any JSON as preferences, falling back to the default for every missing or invalid field. */
export function parsePreferences(raw: unknown): Preferences {
  const root = obj(raw)
  const g = obj(root.gameplay)
  const b = obj(root.battlefield)
  const d = obj(root.display)
  const D = DEFAULT_PREFERENCES
  return {
    version: 1,
    updatedAt: int(root.updatedAt, 0, 0, Number.MAX_SAFE_INTEGER),
    gameplay: {
      priorityMode: typeof g.priorityMode === 'string' && MODES.has(g.priorityMode)
        ? (g.priorityMode as PriorityModeValue)
        : D.gameplay.priorityMode,
      myTurnStops: steps(g.myTurnStops),
      opponentTurnStops: steps(g.opponentTurnStops),
      autoTap: bool(g.autoTap, D.gameplay.autoTap),
      followAction: bool(g.followAction, D.gameplay.followAction),
    },
    battlefield: {
      lands: stacking(b.lands, D.battlefield.lands),
      creatures: stacking(b.creatures, D.battlefield.creatures),
      other: stacking(b.other, D.battlefield.other),
      maxVisibleLayers: int(b.maxVisibleLayers, D.battlefield.maxVisibleLayers, STACK_LIMITS.layersMin, STACK_LIMITS.layersMax),
    },
    display: {
      motion: typeof d.motion === 'string' && MOTIONS.has(d.motion) ? (d.motion as MotionPreference) : D.display.motion,
      hoverPreview: bool(d.hoverPreview, D.display.hoverPreview),
    },
  }
}

/** Same settings, ignoring when they were changed. */
export function samePreferences(a: Preferences, b: Preferences): boolean {
  return JSON.stringify({ ...a, updatedAt: 0 }) === JSON.stringify({ ...b, updatedAt: 0 })
}

// ----- Persistence -----

function readJson(key: string): unknown {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** A browser without a document yet: seed it from the keys the in-game toggles used to write. */
function fromLegacyKeys(): Preferences {
  const stops = obj(readJson(LEGACY_STOPS_KEY))
  let autoTap = true
  let followAction = true
  try {
    autoTap = localStorage.getItem(LEGACY_AUTO_TAP_KEY) !== 'false'
    followAction = localStorage.getItem(LEGACY_FOLLOW_KEY) !== 'false'
  } catch { /* storage blocked */ }
  return parsePreferences({
    gameplay: { myTurnStops: stops.myTurnStops, opponentTurnStops: stops.opponentTurnStops, autoTap, followAction },
  })
}

function load(): Preferences {
  const stored = readJson(STORAGE_KEY)
  return stored ? parsePreferences(stored) : fromLegacyKeys()
}

function save(prefs: Preferences) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs))
  } catch {
    // Private mode / quota — preferences just do not persist this session.
  }
}

let pushTimer: ReturnType<typeof setTimeout> | null = null

/** Push to the account when signed in, debounced so dragging a slider is one request. */
function pushToAccount(prefs: Preferences) {
  if (useAuthStore.getState().status !== 'authenticated') return
  if (pushTimer) clearTimeout(pushTimer)
  pushTimer = setTimeout(() => {
    pushTimer = null
    void savePreferences(prefs).catch(() => undefined)
  }, 600)
}

// ----- Store -----

type Section = 'gameplay' | 'battlefield' | 'display'

interface PreferencesState {
  prefs: Preferences
  /** Merge [patch] into one section; persists locally and to the account. */
  update: <S extends Section>(section: S, patch: Partial<Preferences[S]>) => void
  /** Back to defaults (kept as a change, so it syncs). */
  reset: () => void
  /** Replace the whole document without pushing — what a sync does when the account's copy is newer. */
  replace: (prefs: Preferences) => void
}

export const usePreferences = create<PreferencesState>((set, get) => ({
  prefs: load(),
  update: (section, patch) => {
    const current = get().prefs
    const next = parsePreferences({
      ...current,
      [section]: { ...current[section], ...patch },
      updatedAt: Date.now(),
    })
    save(next)
    pushToAccount(next)
    set({ prefs: next })
  },
  reset: () => {
    const next: Preferences = { ...DEFAULT_PREFERENCES, updatedAt: Date.now() }
    save(next)
    pushToAccount(next)
    set({ prefs: next })
  },
  replace: (prefs) => {
    save(prefs)
    set({ prefs })
  },
}))

/** Non-hook read for store slices and helpers outside React. */
export function getPreferences(): Preferences {
  return usePreferences.getState().prefs
}

let syncing: Promise<void> | null = null

/**
 * Reconcile this browser's preferences with the signed-in account's: the copy changed last wins and
 * is written to the other side. No-op for guests; a network failure leaves the local copy standing.
 */
export function syncPreferences(): Promise<void> {
  if (useAuthStore.getState().status !== 'authenticated') return Promise.resolve()
  if (syncing) return syncing
  syncing = (async () => {
    try {
      const remoteRaw = await fetchPreferences()
      const remote = parsePreferences(remoteRaw)
      const local = getPreferences()
      const remoteEmpty = Object.keys(obj(remoteRaw)).length === 0
      if (!remoteEmpty && remote.updatedAt > local.updatedAt) {
        if (!samePreferences(remote, local)) usePreferences.getState().replace(remote)
      } else if (remoteEmpty || !samePreferences(remote, local)) {
        await savePreferences(local)
      }
    } catch {
      // Offline, or the server has no accounts: the local copy stands.
    } finally {
      syncing = null
    }
  })()
  return syncing
}

/** Mirror the motion preference onto <html> for the global CSS rule and `prefersReducedMotion()`. */
function applyMotion(motion: MotionPreference) {
  if (typeof document === 'undefined') return
  if (motion === 'reduce') document.documentElement.dataset.motion = 'reduce'
  else delete document.documentElement.dataset.motion
}
applyMotion(getPreferences().display.motion)
usePreferences.subscribe((state, prev) => {
  if (state.prefs.display.motion !== prev.prefs.display.motion) applyMotion(state.prefs.display.motion)
})

// Sync whenever a session becomes authenticated (app start with a stored token, or a fresh login).
useAuthStore.subscribe((state, prev) => {
  if (state.status === 'authenticated' && (prev.status !== 'authenticated' || prev.user?.id !== state.user?.id)) {
    void syncPreferences()
  }
})
