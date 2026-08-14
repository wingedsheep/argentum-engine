/**
 * What an AI seat plays — asked once, for every lobby that has one.
 *
 * This used to be two files. `AiOpponentPanel` asked the quick lobby's single AI, `LobbyAiDeckModal`
 * asked each seat of a pod, and they were the same three sources over the same `AiDeckSpec` with the
 * same debounce — differing only in which store action carried the answer. Being copies, they drifted,
 * and each divergence was a bug rather than a choice:
 *
 *   - Clearing the last set sent `[]` in a quick lobby (a visible fall back to Auto) but was skipped
 *     in a pod, which went on quietly using the pool you had just cleared.
 *   - A pod listed its sets as bare codes with no way to browse for a random one.
 *   - A pod's set picker rendered *inside* the seat modal's backdrop, so dismissing the picker
 *     dismissed the seat dialog with it.
 *
 * The three sources are `AiDeckSpec` on the server, and they mean the same thing in both lobbies:
 *   - **Auto** — the server rolls it one, honouring the lobby's deck-legality axis.
 *   - **From sets** — same builders, card pool pinned to sets you choose. Empty means Auto.
 *   - **Pick a deck** — an exact list, through the same {@link DeckPicker} you use for your own. Its
 *     `saved` / `examples` / `paste` tabs are three ways to reach a decklist and collapse into one
 *     `deck` spec on the wire; `random` is omitted because "From sets" already is that.
 */
import { useCallback, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { AiDeckSpec, AiDeckSpecView } from '@/types'
import { DeckPicker } from '../ui/DeckPicker'
import { DeckPickerModal } from '../ui/DeckPickerModal'
import { SetSelector, rollRandomSet } from '../ui/SetSelector'
import styles from '../ui/GameUI.module.css'

export type AiDeckSource = 'auto' | 'sets' | 'deck'

/** Commander-shape formats — the AI builds a singleton deck and picks its own commander for these. */
const COMMANDER_SHAPES = ['COMMANDER', 'BRAWL', 'STANDARD_BRAWL']

/**
 * Deck size for a commander shape, matching `DeckValidator`'s profiles. A lobby running Commander
 * rules with no legality restriction builds to paper Commander, which is the same 100.
 */
function commanderDeckSize(format: string | null): number {
  return format === 'STANDARD_BRAWL' ? 60 : 100
}

/** Which source a lobby should start on, given the server's summary of the current choice. */
export function initialAiSource(aiDeck: AiDeckSpecView | null | undefined): AiDeckSource {
  return (aiDeck?.kind as AiDeckSource | undefined) ?? 'auto'
}

/** The roster-row summary: what this seat is bringing, short enough to sit beside a name. */
export function aiDeckSummary(aiDeck: AiDeckSpecView | null | undefined): string {
  if (!aiDeck) return 'Auto'
  switch (aiDeck.kind) {
    case 'sets':
      return aiDeck.setCodes?.length ? aiDeck.setCodes.join(', ') : 'Auto'
    case 'deck':
      return aiDeck.cardCount
        ? `${aiDeck.label ?? 'Chosen deck'} (${aiDeck.cardCount})`
        : (aiDeck.label ?? 'Chosen deck')
    default:
      return 'Auto'
  }
}

export interface AiDeckChooserProps {
  /** The server's summary of the current choice; re-hydrates the controls after a reconnect. */
  aiDeck: AiDeckSpecView | null
  /** The lobby's deck-legality restriction, upper-case, or null. */
  format: string | null
  /**
   * Whether the lobby's Rules axis says Commander. Asked separately from {@link format} because a
   * lobby can run Commander with no deck-legality restriction at all, and the AI still needs a
   * commander there — the same split `RandomDeckResolver` makes server-side.
   */
  commanderRules?: boolean
  /** True once the choice is locked in (the host has readied up). */
  disabled?: boolean
  /** Where the answer goes — the only thing that differs between a quick lobby and a pod seat. */
  onSpecChange: (spec: AiDeckSpec) => void
  /**
   * Optionally hoist the selected source out of the chooser. The quick lobby does, so that picking
   * "Pick a deck" and closing the dialog before choosing one is still remembered on reopen — nothing
   * has been sent at that point, so the server's summary can't say so.
   */
  source?: AiDeckSource
  onSourceChange?: (source: AiDeckSource) => void
}

export function AiDeckChooser({
  aiDeck,
  format,
  commanderRules = false,
  disabled = false,
  onSpecChange,
  source: controlledSource,
  onSourceChange,
}: AiDeckChooserProps) {
  const availableSets = useGameStore((s) => s.availableSets)

  const [uncontrolledSource, setUncontrolledSource] = useState<AiDeckSource>(() => initialAiSource(aiDeck))
  const source = controlledSource ?? uncontrolledSource
  const [setCodes, setSetCodes] = useState<readonly string[]>(aiDeck?.setCodes ?? [])
  const lastDeckKeyRef = useRef<string | null>(null)

  const isCommanderShape = commanderRules || (format !== null && COMMANDER_SHAPES.includes(format))
  const commanderSize = commanderDeckSize(format)
  const formatLabel = format?.replace('_', ' ').toLowerCase() ?? ''

  const pick = (next: AiDeckSource) => {
    setUncontrolledSource(next)
    onSourceChange?.(next)
    // Auto is complete the moment it's picked. The other two need a selection first, so they only
    // send once the host has actually chosen sets / a deck — otherwise clicking the tab would submit
    // an empty spec the server has to reject.
    if (next === 'auto') onSpecChange({ type: 'auto' })
    else if (next === 'sets' && setCodes.length > 0) onSpecChange({ type: 'sets', setCodes })
  }

  const toggleSet = (code: string) => {
    const next = setCodes.includes(code) ? setCodes.filter((c) => c !== code) : [...setCodes, code]
    setSetCodes(next)
    // An empty selection is the server's "treat as Auto", so send it rather than going quiet —
    // clearing the last chip should visibly fall back, not silently keep the old pool.
    onSpecChange({ type: 'sets', setCodes: next })
  }

  // Deduped like the human picker's submission path: DeckPicker re-emits its current deck on every
  // render, and each send costs a re-roll and a lobby broadcast.
  const handleDeckChange = useCallback(
    (deckList: Record<string, number>, commander?: string | null) => {
      const total = Object.values(deckList).reduce((a, b) => a + b, 0)
      if (total === 0) return
      const key = `${Object.entries(deckList).sort().map(([n, c]) => `${n}:${c}`).join('|')}|${commander ?? ''}`
      if (key === lastDeckKeyRef.current) return
      lastDeckKeyRef.current = key
      onSpecChange({
        type: 'deck',
        deckList,
        label: 'Chosen deck',
        commander: commander ?? null,
      } satisfies AiDeckSpec)
    },
    [onSpecChange],
  )

  return (
    <div data-testid="ai-deck-chooser">
      <div className={styles.settingsRow}>
        <span className={styles.settingsLabel}>AI deck</span>
        <div className={styles.variantGroup}>
          <div className={styles.settingsButtons}>
            <SourceButton active={source === 'auto'} disabled={disabled} onClick={() => pick('auto')}>
              Auto
            </SourceButton>
            <SourceButton active={source === 'sets'} disabled={disabled} onClick={() => pick('sets')}>
              From sets
            </SourceButton>
            <SourceButton active={source === 'deck'} disabled={disabled} onClick={() => pick('deck')}>
              Pick a deck
            </SourceButton>
          </div>

          {source === 'auto' && (
            <div className={styles.variantCaption}>
              {isCommanderShape
                ? `The server picks the AI a commander and builds it a ${commanderSize}-card singleton deck in that commander's colours.`
                : format
                  ? `The server builds the AI a 60-card ${formatLabel}-legal deck.`
                  : 'The server opens eight boosters from your set and auto-builds the AI a 40-card deck.'}
            </div>
          )}

          {source === 'sets' && (
            <>
              <SetSelector
                sets={availableSets}
                selectedCodes={setCodes}
                onToggleSet={toggleSet}
                disabled={disabled}
                title="Sets for the AI's deck"
                emptyLabel="No sets chosen — the AI falls back to Auto"
                onSelectRandom={() => {
                  const chosen = rollRandomSet(availableSets, setCodes)
                  if (chosen) toggleSet(chosen.code)
                }}
              />
              <div className={styles.variantCaption}>
                {setCodes.length === 0
                  ? 'Pick one or more sets — until you do, the AI falls back to Auto.'
                  : isCommanderShape
                    ? `The AI's commander and its ${commanderSize}-card deck are drawn from these sets.`
                    : format
                      ? `Only ${formatLabel}-legal cards from these sets are used.`
                      : 'Eight boosters are opened across these sets and auto-built into a deck.'}
              </div>
            </>
          )}
        </div>
      </div>

      {source === 'deck' && (
        <div className={styles.aiDeckSection} data-testid="ai-deck-section">
          <div className={styles.aiDeckSectionHeader}>
            <span className={styles.aiDeckSectionTitle}>The AI opponent’s deck</span>
            <span className={styles.aiDeckSectionHint}>
              Checked against the lobby format when you pick it.
            </span>
          </div>
          <DeckPicker
            onDeckChange={handleDeckChange}
            availableSets={availableSets}
            disabled={disabled}
            format={format}
            tabs={['saved', 'examples', 'paste']}
          />
        </div>
      )}
    </div>
  )
}

function SourceButton({
  active,
  disabled,
  onClick,
  children,
}: {
  active: boolean
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`${styles.settingsButton} ${active ? styles.settingsButtonActive : ''}`}
    >
      {children}
    </button>
  )
}

/**
 * The quick lobby's single AI seat, opened from its player row.
 *
 * Source state is hoisted to `LobbyScreen` here (and only here) so that "Pick a deck" survives
 * closing the dialog before a deck has been chosen — see {@link AiDeckChooserProps.source}.
 */
export function QuickAiDeckModal({
  playerName,
  aiDeck,
  format,
  commanderRules,
  disabled,
  source,
  onSourceChange,
  onClose,
}: {
  playerName: string
  aiDeck: AiDeckSpecView | null
  format: string | null
  commanderRules: boolean
  disabled: boolean
  source: AiDeckSource
  onSourceChange: (source: AiDeckSource) => void
  onClose: () => void
}) {
  const setQuickGameAiDeck = useGameStore((s) => s.setQuickGameAiDeck)
  return (
    <DeckPickerModal
      title={`${playerName}’s deck`}
      subtitle="Choose what this seat brings to the game."
      onClose={onClose}
    >
      <AiDeckChooser
        aiDeck={aiDeck}
        format={format}
        commanderRules={commanderRules}
        disabled={disabled}
        onSpecChange={setQuickGameAiDeck}
        source={source}
        onSourceChange={onSourceChange}
      />
    </DeckPickerModal>
  )
}

/**
 * One AI seat of a pod, opened from its roster row.
 *
 * Only reachable in a premade-decks lobby. Everywhere else the AI builds from the pool it was dealt
 * — that is the format working, not a gap — and the server refuses the message rather than accepting
 * a choice it would silently ignore.
 */
export function LobbyAiDeckModal({
  playerId,
  playerName,
  aiDeck,
  format,
  onClose,
}: {
  playerId: string
  playerName: string
  aiDeck: AiDeckSpecView | null
  format: string | null
  onClose: () => void
}) {
  const setLobbyAiDeck = useGameStore((s) => s.setLobbyAiDeck)
  const onSpecChange = useCallback(
    (spec: AiDeckSpec) => setLobbyAiDeck(playerId, spec),
    [setLobbyAiDeck, playerId],
  )
  return (
    <DeckPickerModal
      title={`${playerName}’s deck`}
      subtitle="Each AI seat is chosen separately, so they need not all play the same thing."
      onClose={onClose}
    >
      <AiDeckChooser
        aiDeck={aiDeck}
        format={format}
        onSpecChange={onSpecChange}
      />
    </DeckPickerModal>
  )
}
