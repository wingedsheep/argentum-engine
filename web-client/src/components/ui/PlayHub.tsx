/**
 * The landing screen's PLAY surface: a catalogue of named game modes and a launch panel.
 *
 * It replaces a three-question wizard (who's playing → where the cards come from → which table)
 * that made every player translate "I want to draft" into axes before anything happened. Here the
 * player names the mode, and the panel asks only what that mode still needs — who to play with,
 * how many opponents, which set or deck — with everything defaulted, so the common cases are two
 * clicks: a tile, then Play. A game against the AI skips the lobby entirely once its seats are
 * filled (`startWhenSeated`); a game with people opens the lobby with its invite code.
 *
 * The selected mode lives in the URL (`/play/<slug>`), so Back closes the panel — which on a phone,
 * where the panel is a bottom sheet, is exactly what the system back gesture should do — and a mode
 * is linkable. The options themselves are remembered per mode in `localStorage`: the second draft
 * opens on the set and table size of the first.
 *
 * Every combination resolves through `lobby/playModes.ts` to a recipe `modeMatrix` accepts; this
 * file only renders it.
 */
import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore'
import { useUnifiedDecks } from '@/store/useUnifiedDecks'
import {
  MODES,
  MODE_GROUPS,
  canRollDeck,
  defaultOptions,
  effectiveOpponents,
  hasHumanTableChoice,
  launchLabel,
  modeFromSlug,
  modeInfo,
  needsDeck,
  needsSet,
  opponentRange,
  recipeForOptions,
  stagesFor,
  type DraftStyle,
  type HumanTable,
  type ModeGroup,
  type ModeId,
  type PanelDeck,
  type PlayOptions,
  type PlayWith,
  type SealedStyle,
  type TableCards,
} from '../lobby/playModes'
import type { LobbyRecipe } from '../lobby/lobbyRecipe'
import { AI_DISABLED_ON_SERVER } from '../lobby/modeMatrix'
import { defaultSetCode } from '../lobby/useApplyRecipe'
import { SetPickerModal } from './SetPickerModal'
import { SetIcon } from './SetIcon'
import { LaunchDeckChoice } from './LaunchDeckChoice'
import { useStarterDecks, type StarterDeck } from '@/store/useStarterDecks'
import type { UnifiedDeck } from '@/store/useUnifiedDecks'
import styles from './PlayHub.module.css'

const PLAY_PREFIX = '/play'
const OPTIONS_STORAGE_KEY = 'argentum-play-options'

export function PlayHub({
  aiEnabled,
  onLaunch,
  aside,
}: {
  aiEnabled: boolean
  onLaunch: (recipe: LobbyRecipe) => void
  /** The side column when no mode is open: open lobbies, live games, callouts. */
  aside: ReactNode
}) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const selected = pathname.startsWith(`${PLAY_PREFIX}/`)
    ? modeFromSlug(pathname.slice(PLAY_PREFIX.length + 1).split('/')[0])
    : null

  const open = (mode: ModeId) => {
    const path = `${PLAY_PREFIX}/${modeInfo(mode).slug}`
    // Switching between tiles replaces rather than stacks, so one Back always closes the panel.
    navigate(path, { replace: selected !== null })
  }
  const close = () => navigate('/', { replace: false })

  // Escape closes the panel, as it would a sheet or dialog.
  useEffect(() => {
    if (selected === null) return
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected])

  return (
    <div className={styles.hub}>
      <section className={styles.catalog} aria-labelledby="play-heading">
        <h2 id="play-heading" className={styles.catalogHeading}>Pick a game</h2>
        {MODE_GROUPS.map((group) => {
          const modes = MODES.filter((m) => m.group === group.id)
          return (
            <div key={group.id} className={styles.group}>
              <div className={styles.groupHeading}>
                <span className={styles.groupSwatch} data-group={group.id} aria-hidden />
                <h3 className={styles.groupLabel}>{group.label}</h3>
                <span className={styles.groupCaption}>{group.caption}</span>
              </div>
              <div className={styles.tiles} data-count={modes.length}>
                {modes.map((mode) => (
                  <button
                    key={mode.id}
                    type="button"
                    className={styles.tile}
                    data-group={mode.group}
                    data-selected={selected === mode.id}
                    data-testid={`mode-${mode.slug}`}
                    aria-pressed={selected === mode.id}
                    onClick={() => (selected === mode.id ? close() : open(mode.id))}
                  >
                    <ModeIcon mode={mode.id} group={mode.group} />
                    <span className={styles.tileText}>
                      <span className={styles.tileTop}>
                        <span className={styles.tileLabel}>{mode.label}</span>
                        <span className={styles.tilePlayers}>{mode.players}</span>
                      </span>
                      <span className={styles.tileCaption}>{mode.caption}</span>
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )
        })}
      </section>

      <div className={styles.side} data-panel-open={selected !== null}>
        {selected !== null && (
          <>
            <button type="button" className={styles.sheetBackdrop} aria-label="Close" onClick={close} />
            <LaunchPanel
              key={selected}
              mode={selected}
              aiEnabled={aiEnabled}
              onClose={close}
              onLaunch={onLaunch}
            />
          </>
        )}
        <div className={styles.aside}>{aside}</div>
      </div>
    </div>
  )
}

/* ── Launch panel ───────────────────────────────────────────────────────── */

function LaunchPanel({
  mode,
  aiEnabled,
  onClose,
  onLaunch,
}: {
  mode: ModeId
  aiEnabled: boolean
  onClose: () => void
  onLaunch: (recipe: LobbyRecipe) => void
}) {
  const info = modeInfo(mode)
  const availableSets = useGameStore((s) => s.availableSets)
  const { decks } = useUnifiedDecks()
  const allStarters = useStarterDecks()
  const [options, setOptionsState] = useState<PlayOptions>(() => loadOptions(mode, aiEnabled))
  const [setPickerOpen, setSetPickerOpen] = useState(false)

  const setOptions = (patch: Partial<PlayOptions>) => {
    setOptionsState((prev) => {
      const next = { ...prev, ...patch }
      saveOptions(next)
      return next
    })
  }

  // The AI can be switched off between page loads; a remembered "AI" must not survive that.
  useEffect(() => {
    if (!aiEnabled && options.playWith === 'AI') setOptions({ playWith: 'FRIENDS' })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [aiEnabled])

  // Commander wants commander decks; everything else takes whatever you have, newest first.
  const deckChoices = useMemo(() => {
    const pool = mode === 'COMMANDER' ? decks.filter((d) => d.commander) : decks
    return [...pool].sort((a, b) => (b.updatedAt ?? 0) - (a.updatedAt ?? 0))
  }, [decks, mode])

  // Starter decks the mode can play: Commander wants the commander precons, everything else the
  // 60-card lists.
  const starterChoices = useMemo(() => {
    if (!allStarters) return null
    return allStarters.filter((d) => (d.format?.toUpperCase() === 'COMMANDER') === (mode === 'COMMANDER'))
  }, [allStarters, mode])

  const setCode = options.setCode && availableSets.some((s) => s.code === options.setCode)
    ? options.setCode
    : availableSets.length > 0 ? defaultSetCode(availableSets) : null
  const resolved: PlayOptions = { ...options, setCode }
  const deck = resolveDeck(options.deck, deckChoices, starterChoices, canRollDeck(resolved))
  const effective: PlayOptions = { ...resolved, deck }

  const range = opponentRange(effective)
  const opponents = effectiveOpponents(effective)
  const stages = stagesFor(effective)
  const set = availableSets.find((s) => s.code === setCode)
  const isAi = effective.playWith === 'AI'

  return (
    <section className={styles.panel} aria-labelledby="launch-heading" data-testid="launch-panel">
      <header className={styles.panelHeader}>
        <ModeIcon mode={mode} group={info.group} />
        <div className={styles.panelTitleBlock}>
          <h2 id="launch-heading" className={styles.panelTitle}>{info.label}</h2>
          <p className={styles.panelDescription}>{info.description}</p>
        </div>
        <button type="button" className={styles.closeButton} aria-label="Close" onClick={onClose}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden>
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </header>

      <div className={styles.panelBody}>
        <Field label="Play with">
          <Segmented<PlayWith>
            name="play-with"
            value={effective.playWith}
            onChange={(playWith) => setOptions({ playWith })}
            options={[
              { value: 'AI', label: 'AI', sub: 'Starts right away', disabledReason: aiEnabled ? undefined : AI_DISABLED_ON_SERVER },
              { value: 'FRIENDS', label: 'Friends', sub: 'Invite link' },
              { value: 'PUBLIC', label: 'Anyone', sub: 'Public lobby' },
            ]}
          />
        </Field>

        {isAi && range && (
          <Field label="AI opponents" inline>
            <Stepper
              value={opponents}
              min={range.min}
              max={range.max}
              step={range.step}
              onChange={(n) => setOptions({ opponents: n })}
            />
          </Field>
        )}

        {!isAi && hasHumanTableChoice(mode) && (
          <Field label="Table">
            <Segmented<HumanTable>
              name="human-table"
              value={effective.humanTable}
              onChange={(humanTable) => setOptions({ humanTable })}
              options={[
                { value: 'ONE_V_ONE', label: 'One opponent' },
                { value: 'BRACKET', label: 'Group bracket', sub: 'Up to 8' },
              ]}
            />
          </Field>
        )}

        {mode === 'DRAFT' && (
          <Field label="Draft style">
            <Chips<DraftStyle>
              value={effective.draftStyle}
              onChange={(draftStyle) => setOptions({ draftStyle })}
              options={[
                { value: 'BOOSTER', label: 'Booster' },
                { value: 'WINSTON', label: 'Winston', title: 'Two players pass three piles' },
                { value: 'GRID', label: 'Grid', title: 'Up to four players pick rows and columns' },
                { value: 'COMMANDER', label: 'Commander' },
              ]}
            />
          </Field>
        )}

        {mode === 'SEALED' && (
          <Field label="Boosters">
            <Chips<SealedStyle>
              value={effective.sealedStyle}
              onChange={(sealedStyle) => setOptions({ sealedStyle })}
              options={[
                { value: 'STANDARD', label: 'Standard' },
                { value: 'COMMANDER', label: 'Commander' },
              ]}
            />
          </Field>
        )}

        {info.group === 'TABLE' && mode !== 'COMMANDER' && (
          <Field label="Decks from">
            <Chips<TableCards>
              value={effective.tableCards}
              onChange={(tableCards) => setOptions({ tableCards })}
              options={[
                { value: 'DECKS', label: 'Your decks' },
                { value: 'JUMP_IN', label: 'Jump In' },
                { value: 'SEALED', label: 'Sealed' },
                { value: 'DRAFT', label: 'Draft' },
              ]}
            />
          </Field>
        )}

        {needsSet(effective) && (
          <Field label="Set">
            <button type="button" className={styles.setButton} onClick={() => setSetPickerOpen(true)}>
              {setCode && <SetIcon code={setCode} className={styles.setIcon} />}
              <span className={styles.setName}>{set?.name ?? 'Choose a set'}</span>
              <span className={styles.setChange}>Change</span>
            </button>
          </Field>
        )}

        {needsDeck(effective) && (
          <Field label={isAi ? 'Your deck' : 'Your deck · others bring their own'}>
            <LaunchDeckChoice
              value={deck}
              saved={deckChoices}
              starters={starterChoices}
              canRoll={canRollDeck(resolved)}
              onChange={(next) => setOptions({ deck: next })}
            />
          </Field>
        )}
      </div>

      <footer className={styles.panelFooter}>
        <ol className={styles.stages} aria-label="What happens next">
          {stages.map((stage) => (
            <li key={stage} className={styles.stage}>{stage}</li>
          ))}
        </ol>
        <button
          type="button"
          className={styles.playButton}
          data-testid="launch-play"
          onClick={() => {
            onLaunch(recipeForOptions(effective))
            // Leave `/play/<mode>` behind, so coming back from the game lands on the catalogue.
            onClose()
          }}
        >
          {launchLabel(effective)}
          {isAi && range && <span className={styles.playButtonMeta}>vs {opponents} AI</span>}
        </button>
        <p className={styles.footerHint}>
          {isAi
            ? 'Timer, pack count and other settings keep their defaults.'
            : 'Settings stay editable in the lobby until you start.'}
        </p>
      </footer>

      {setPickerOpen && (
        <SetPickerModal
          sets={availableSets}
          selectedCodes={setCode ? [setCode] : []}
          mode="single"
          title="Choose a set"
          onToggleSet={(code) => setOptions({ setCode: code })}
          onClose={() => setSetPickerOpen(false)}
        />
      )}
    </section>
  )
}

/**
 * The deck the panel will launch with.
 *
 * A remembered choice wins while it still holds — the deck still exists, a rolled deck is still
 * possible at this table. Otherwise the newest of your decks, so "play the deck I just built" is no
 * clicks at all; with none, the first starter deck, so a first game against the AI goes straight to
 * the table. Only with nothing at all (starters still loading) does it fall back to the lobby.
 */
function resolveDeck(
  stored: PanelDeck | null,
  saved: readonly UnifiedDeck[],
  starters: readonly StarterDeck[] | null,
  canRoll: boolean,
): PanelDeck {
  switch (stored?.kind) {
    case 'SAVED':
      if (saved.some((d) => d.name === stored.name)) return stored
      break
    case 'EXAMPLE':
      if (starters === null || starters.some((d) => d.name === stored.name)) return stored
      break
    case 'RANDOM':
      if (canRoll) return stored
      break
    case 'LOBBY':
      return stored
  }
  if (saved[0]) return { kind: 'SAVED', name: saved[0].name }
  if (starters?.[0]) return { kind: 'EXAMPLE', name: starters[0].name }
  return { kind: 'LOBBY' }
}

/* ── Small controls ─────────────────────────────────────────────────────── */

function Field({ label, inline, children }: { label: string; inline?: boolean; children: ReactNode }) {
  return (
    <div className={styles.field} data-inline={inline ?? false}>
      <span className={styles.fieldLabel}>{label}</span>
      {children}
    </div>
  )
}

function Segmented<V extends string>({
  name,
  value,
  options,
  onChange,
}: {
  name: string
  value: V
  options: ReadonlyArray<{ value: V; label: string; sub?: string; disabledReason?: string | undefined }>
  onChange: (value: V) => void
}) {
  return (
    <div className={styles.segmented} role="radiogroup" aria-label={name}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={value === o.value}
          data-testid={`${name}-${o.value.toLowerCase()}`}
          className={styles.segment}
          disabled={o.disabledReason !== undefined}
          title={o.disabledReason}
          onClick={() => onChange(o.value)}
        >
          <span className={styles.segmentLabel}>{o.label}</span>
          {o.sub && <span className={styles.segmentSub}>{o.sub}</span>}
        </button>
      ))}
    </div>
  )
}

function Chips<V extends string>({
  value,
  options,
  onChange,
}: {
  value: V
  options: ReadonlyArray<{ value: V; label: string; title?: string }>
  onChange: (value: V) => void
}) {
  return (
    <div className={styles.chips}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          className={styles.chip}
          aria-pressed={value === o.value}
          title={o.title}
          onClick={() => onChange(o.value)}
        >
          {o.label}
        </button>
      ))}
    </div>
  )
}

function Stepper({
  value,
  min,
  max,
  step,
  onChange,
}: {
  value: number
  min: number
  max: number
  step: number
  onChange: (value: number) => void
}) {
  return (
    <div className={styles.stepper}>
      <button
        type="button"
        className={styles.stepButton}
        aria-label="Fewer opponents"
        disabled={value <= min}
        onClick={() => onChange(Math.max(min, value - step))}
      >
        −
      </button>
      <span className={styles.stepValue} aria-live="polite">{value}</span>
      <button
        type="button"
        className={styles.stepButton}
        aria-label="More opponents"
        disabled={value >= max}
        onClick={() => onChange(Math.min(max, value + step))}
      >
        +
      </button>
    </div>
  )
}

/* ── Icons ──────────────────────────────────────────────────────────────── */

const ICON_PATHS: Record<ModeId, string> = {
  CONSTRUCTED: 'M8 4h9a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M4 7v11a3 3 0 0 0 3 3 M10 9h5 M10 13h5',
  JUMP_IN: 'M4 5h7v14H4z M13 5h7v14h-7z M7.5 9v6 M16.5 9v6',
  RANDOM: 'M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z M9 9h.01 M15 9h.01 M12 12h.01 M9 15h.01 M15 15h.01',
  MOMIR: 'M12 3l2.2 5.8L20 11l-5.8 2.2L12 19l-2.2-5.8L4 11l5.8-2.2z',
  DRAFT: 'M3 9l6-3 2 13-6 2z M10 5.5h6l1 14h-6 M17 6.5l4 1.8-3 12',
  SEALED: 'M3 7l9-4 9 4v10l-9 4-9-4z M3 7l9 4 9-4 M12 11v10',
  FREE_FOR_ALL: 'M10 5a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M3 18a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M17 18a2 2 0 1 0 4 0a2 2 0 1 0-4 0 M11 7l-5 9 M13 7l5 9 M7 18h10',
  TWO_HEADED_GIANT: 'M5 8a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M14 8a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0 M3 19c0-3 2-5 4.5-5 1.6 0 2.8.6 4.5 2 1.7-1.4 2.9-2 4.5-2 2.5 0 4.5 2 4.5 5',
  TEAM_VS_TEAM: 'M4 4l9 9 M20 4l-9 9 M6 15l-2 2 3 3 2-2 M18 15l2 2-3 3-2-2',
  COMMANDER: 'M3 18h18 M4 18L3 8l5 4 4-7 4 7 5-4-1 10',
}

function ModeIcon({ mode, group }: { mode: ModeId; group: ModeGroup }) {
  return (
    <span className={styles.icon} data-group={group} aria-hidden>
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
        <path d={ICON_PATHS[mode]} />
      </svg>
    </span>
  )
}

/* ── Remembered options ─────────────────────────────────────────────────── */

type StoredOptions = Partial<Record<ModeId, Partial<PlayOptions>>>

function readStore(): StoredOptions {
  try {
    const raw = localStorage.getItem(OPTIONS_STORAGE_KEY)
    const parsed: unknown = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? parsed as StoredOptions : {}
  } catch {
    return {}
  }
}

/**
 * The options this mode was last launched with, over its defaults.
 *
 * Each field is checked against its own domain rather than trusted: storage outlives the build that
 * wrote it, and a value this build doesn't know would otherwise reach the recipe. `playModes`
 * clamps the opponent count itself, and a deck or set that has gone is re-resolved by the panel.
 */
function loadOptions(mode: ModeId, aiEnabled: boolean): PlayOptions {
  const defaults = defaultOptions(mode, aiEnabled)
  const stored = readStore()[mode]
  if (!stored) return defaults
  const pick = <K extends keyof PlayOptions>(key: K, allowed: readonly PlayOptions[K][]): PlayOptions[K] =>
    allowed.includes(stored[key] as PlayOptions[K]) ? stored[key] as PlayOptions[K] : defaults[key]
  const playWith = pick('playWith', ['AI', 'FRIENDS', 'PUBLIC'])
  return {
    ...defaults,
    playWith: playWith === 'AI' && !aiEnabled ? 'FRIENDS' : playWith,
    opponents: typeof stored.opponents === 'number' ? stored.opponents : defaults.opponents,
    humanTable: pick('humanTable', ['ONE_V_ONE', 'BRACKET']),
    draftStyle: pick('draftStyle', ['BOOSTER', 'WINSTON', 'GRID', 'COMMANDER']),
    sealedStyle: pick('sealedStyle', ['STANDARD', 'COMMANDER']),
    tableCards: pick('tableCards', ['DECKS', 'JUMP_IN', 'SEALED', 'DRAFT']),
    setCode: typeof stored.setCode === 'string' ? stored.setCode : null,
    deck: storedDeck(stored),
  }
}

/** A stored deck choice, including the `deckName` string the panel stored before `deck` existed. */
function storedDeck(stored: Partial<PlayOptions> & { deckName?: unknown }): PanelDeck | null {
  const deck = stored.deck as { kind?: unknown; name?: unknown } | null | undefined
  if (deck && typeof deck === 'object') {
    if ((deck.kind === 'SAVED' || deck.kind === 'EXAMPLE') && typeof deck.name === 'string' && deck.name !== '') {
      return { kind: deck.kind, name: deck.name }
    }
    if (deck.kind === 'RANDOM' || deck.kind === 'LOBBY') return { kind: deck.kind }
  }
  if (stored.deckName === '') return { kind: 'LOBBY' }
  if (typeof stored.deckName === 'string') return { kind: 'SAVED', name: stored.deckName }
  return null
}

function saveOptions(options: PlayOptions): void {
  try {
    const all = readStore()
    all[options.mode] = options
    localStorage.setItem(OPTIONS_STORAGE_KEY, JSON.stringify(all))
  } catch {
    // Private mode or a full quota: the panel still works, it just won't remember.
  }
}
