/**
 * Quick Game Lobby overlay — staging-area screen between picking "Quick Game" on the home
 * screen and the actual match start.
 *
 * Reuses the tournament lobby's CSS classes (`lobbyOverlay`, `lobbyContent`, `lobbyHeader`,
 * `inviteBox`, `playerListPanel`, `actionsRow`, etc.) so the visual language is identical to
 * `LobbyOverlay` — just simpler, since quick-game lobbies have no settings panel.
 *
 * The opponent's actual deck list is intentionally never shown — only a "deck-selected"
 * indicator and a label like "Custom (60)" / "Random Pool" — matching the project decision
 * that decks stay hidden until the game starts.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { DeckFormat, QuickGameLobbyPlayerView } from '@/types'
import { randomBackground } from '@/utils/background.ts'
import momirVigUrl from '@/assets/momir-vig.svg'
import { DeckPicker } from './DeckPicker'
import { JoinQrModal } from './JoinQrModal'
import { buildJoinUrl } from '@/utils/joinLink'
import styles from './GameUI.module.css'

const FORMAT_OPTIONS: Array<{ value: DeckFormat; label: string }> = [
  { value: 'STANDARD', label: 'Standard' },
  { value: 'PIONEER', label: 'Pioneer' },
  { value: 'MODERN', label: 'Modern' },
  { value: 'PAUPER', label: 'Pauper' },
  { value: 'LEGACY', label: 'Legacy' },
  { value: 'VINTAGE', label: 'Vintage' },
  { value: 'COMMANDER', label: 'Commander' },
  { value: 'BRAWL', label: 'Brawl' },
  { value: 'STANDARD_BRAWL', label: 'Standard Brawl' },
  { value: 'PREMODERN', label: 'Premodern' },
]

export function QuickGameLobbyOverlay() {
  const lobby = useGameStore((s) => s.quickGameLobbyState)
  const availableSets = useGameStore((s) => s.availableSets)
  const submitDeck = useGameStore((s) => s.submitQuickGameLobbyDeck)
  const setReady = useGameStore((s) => s.setQuickGameLobbyReady)
  const setSetCode = useGameStore((s) => s.setQuickGameLobbySetCode)
  const setPublic = useGameStore((s) => s.setQuickGameLobbyPublic)
  const setFormat = useGameStore((s) => s.setQuickGameLobbyFormat)
  const leave = useGameStore((s) => s.leaveQuickGameLobby)

  // Throttle deck submissions: the picker fires several times per keystroke, but we only
  // want one network round-trip per stable choice. Also dedupe — DeckPicker re-emits its
  // current deck whenever it re-renders (e.g. when a server broadcast lands), and resending
  // an unchanged deck would server-side reset our `ready` flag and trigger another broadcast,
  // which is the spam loop we used to have.
  const pendingDeckRef = useRef<Record<string, number> | null>(null)
  const pendingCommanderRef = useRef<string | null>(null)
  const lastSubmittedKeyRef = useRef<string | null>(null)
  const debounceRef = useRef<number | null>(null)
  const [deckValid, setDeckValid] = useState<boolean>(true)
  const [copied, setCopied] = useState(false)

  const handleDeckChange = useCallback(
    (deckList: Record<string, number>, commander?: string | null) => {
      // Include the commander in the dedupe key so swapping commanders on otherwise-identical
      // deck contents still triggers a resubmission.
      const key = `${serializeDeck(deckList)}|${commander ?? ''}`
      if (key === lastSubmittedKeyRef.current) return
      pendingDeckRef.current = deckList
      pendingCommanderRef.current = commander ?? null
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      debounceRef.current = window.setTimeout(() => {
        const pending = pendingDeckRef.current
        if (!pending) return
        const pendingCmdr = pendingCommanderRef.current
        const pendingKey = `${serializeDeck(pending)}|${pendingCmdr ?? ''}`
        if (pendingKey === lastSubmittedKeyRef.current) return
        lastSubmittedKeyRef.current = pendingKey
        submitDeck(pending, pendingCmdr)
      }, 250)
    },
    [submitDeck]
  )

  // Flush any pending deck on unmount so we don't drop the user's last edit.
  useEffect(() => {
    return () => {
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      const pending = pendingDeckRef.current
      const pendingCmdr = pendingCommanderRef.current
      const pendingKey = pending ? `${serializeDeck(pending)}|${pendingCmdr ?? ''}` : null
      if (pending && pendingKey !== lastSubmittedKeyRef.current) {
        submitDeck(pending, pendingCmdr)
      }
    }
  }, [submitDeck])

  if (!lobby) return null

  const you = lobby.players.find((p) => p.playerId === lobby.youPlayerId)
  const others = lobby.players.filter((p) => p.playerId !== lobby.youPlayerId)
  const youReady = you?.ready ?? false
  // Host (first non-AI player) controls visibility — matches the leave/close convention.
  const host = lobby.players.find((p) => !p.isAi)
  const isHost = host?.playerId === lobby.youPlayerId
  const isMomir = lobby.momirBasic ?? false

  const copyLobbyId = () => {
    navigator.clipboard.writeText(lobby.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className={styles.lobbyOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <div className={styles.lobbyContent}>
        <div className={styles.lobbyHeader}>
          {isMomir && (
            <div
              aria-hidden
              style={{
                width: 84,
                height: 84,
                margin: '0 auto 8px',
                backgroundColor: 'var(--accent-teal, #6fd3c0)',
                WebkitMaskImage: `url(${momirVigUrl})`,
                maskImage: `url(${momirVigUrl})`,
                WebkitMaskSize: 'contain',
                maskSize: 'contain',
                WebkitMaskRepeat: 'no-repeat',
                maskRepeat: 'no-repeat',
                WebkitMaskPosition: 'center',
                maskPosition: 'center',
                opacity: 0.9,
              }}
            />
          )}
          <div className={`${styles.lobbyFormat} ${styles.lobbyFormatSealed}`}>
            {isMomir ? 'Momir Basic' : 'Quick Game'}
          </div>
          <h1 className={styles.lobbyTitle}>
            {lobby.vsAi ? 'vs AI' : 'Lobby'}
          </h1>
          <p className={styles.lobbySubtitle}>
            {isMomir
              ? lobby.vsAi
                ? 'No deckbuilding — ready up and the AI starts. Flip creatures with the Momir Vig avatar.'
                : 'No deckbuilding — share the invite code, then both players ready up.'
              : lobby.vsAi
                ? 'Pick a deck and ready up — the AI starts as soon as you do.'
                : 'Share the invite code with a friend, then both players ready up.'}
          </p>
        </div>

        {!lobby.vsAi && (
          <div style={{ alignSelf: 'stretch', display: 'flex', alignItems: 'stretch', gap: 8 }}>
            <div
              onClick={copyLobbyId}
              className={`${styles.inviteBox} ${copied ? styles.inviteBoxCopied : ''}`}
              style={{ flex: 1, marginBottom: 0, justifyContent: 'space-between' }}
            >
              <div>
                <div style={{ color: 'var(--text-disabled)', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 3 }}>
                  Invite Code
                </div>
                <div className={styles.inviteCode} data-testid="quick-game-invite-code">
                  {lobby.lobbyId}
                </div>
              </div>
              <span className={`${styles.inviteCopyLabel} ${copied ? styles.inviteCopyLabelCopied : ''}`} style={{ flexShrink: 0, marginLeft: 12 }}>
                {copied ? 'Copied!' : 'Copy'}
              </span>
            </div>
            <JoinQrModal url={buildJoinUrl(lobby.lobbyId)} />
          </div>
        )}

        {!lobby.vsAi && (
          <div className={styles.settingsPanel}>
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Visibility</span>
              <div className={styles.settingsButtons}>
                <button
                  type="button"
                  onClick={() => isHost && setPublic(false)}
                  disabled={!isHost}
                  className={`${styles.settingsButton} ${!lobby.isPublic ? styles.settingsButtonActive : ''}`}
                  title={isHost ? '' : 'Only the host can change visibility'}
                >
                  Private
                </button>
                <button
                  type="button"
                  onClick={() => isHost && setPublic(true)}
                  disabled={!isHost}
                  className={`${styles.settingsButton} ${lobby.isPublic ? styles.settingsButtonActive : ''}`}
                  title={isHost ? '' : 'Only the host can change visibility'}
                >
                  Public
                </button>
              </div>
            </div>
            <FormatSelector isMomir={isMomir} format={lobby.format ?? null} isHost={isHost} onChange={setFormat} />
          </div>
        )}

        {lobby.vsAi && (
          <div className={styles.settingsPanel}>
            <FormatSelector isMomir={isMomir} format={lobby.format ?? null} isHost={isHost} onChange={setFormat} />
          </div>
        )}

        <div className={styles.playerListPanel}>
          <div className={styles.playerListHeader}>
            <span className={styles.playerListTitle}>Players</span>
            <span className={styles.playerCount}>{lobby.players.length} / 2</span>
          </div>
          {you && <PlayerRow player={you} isYou={true} isLast={others.length === 0} />}
          {others.map((p, i) => (
            <PlayerRow key={p.playerId} player={p} isYou={false} isLast={i === others.length - 1} />
          ))}
          {others.length === 0 && !lobby.vsAi && (
            <div className={styles.playerRow} style={{ borderBottom: 'none', justifyContent: 'center', color: 'var(--text-faint)', fontStyle: 'italic' }}>
              Waiting for opponent…
            </div>
          )}
        </div>

        {!isMomir && (
          <DeckPicker
            onDeckChange={handleDeckChange}
            onValidityChange={setDeckValid}
            onSetCodeChange={setSetCode}
            initialSetCode={you?.setCode ?? null}
            availableSets={availableSets}
            disabled={youReady}
            format={lobby.format ?? null}
          />
        )}

        <div className={styles.actionsRow}>
          {youReady ? (
            <button onClick={() => setReady(false)} className={styles.startButton} type="button">
              Cancel ready
            </button>
          ) : (
            <button
              onClick={() => setReady(true)}
              className={styles.startButton}
              type="button"
              disabled={!isMomir && (!deckValid || !you?.deckSelected)}
              title={!isMomir && !you?.deckSelected ? 'Pick a deck first' : ''}
            >
              I'm ready
            </button>
          )}
          <button onClick={leave} className={styles.leaveButton} type="button">
            Leave
          </button>
        </div>

        {!lobby.vsAi && others.length === 0 && (
          <p className={styles.waitingHint}>
            Waiting for opponent to join…
          </p>
        )}
      </div>
    </div>
  )
}

/** Sentinel key for the Momir Basic custom format (not a real {@link DeckFormat}). */
const MOMIR_FORMAT_VALUE = 'MOMIR'

/**
 * Non-deckbuilding game modes, surfaced as their own elevated section (see {@link CustomFormatSection})
 * rather than buried among constructed formats in the dropdown. Currently just Momir Basic — fixed
 * 60-basic decks, the avatar in the command zone, creatures rolled from every set.
 *
 * NOTE: the lobby's format channel is `(format, momirBasic)` where `momirBasic` is Momir-specific.
 * Adding a second custom mode here means widening that contract (e.g. a custom-format key) so this
 * section can tell the modes apart — today the toggle simply maps to the `momirBasic` flag.
 */
const CUSTOM_FORMATS: ReadonlyArray<{ key: string; name: string; desc: string; icon: string }> = [
  {
    key: MOMIR_FORMAT_VALUE,
    name: 'Momir Basic',
    desc: 'No deckbuilding — everyone runs 60 basics. Discard a card and pay {X} to flip a random creature with mana value X.',
    icon: momirVigUrl,
  },
]

/**
 * Lobby format picker — a single either/or choice, not two stacked settings.
 *
 * The top row restricts submitted decks to a *constructed* format ("No restriction" = anything);
 * below an explicit "or" divider, the *custom* (non-deckbuilding) modes appear as selectable cards.
 * The two are mutually exclusive server-side, so we make that visible: while a custom mode is
 * active the constructed dropdown is disabled (and reads "No restriction", since the server clears
 * `format`), and selecting a constructed format leaves every custom card unselected. Exactly one
 * side is ever live. Toggle a custom card off to return to no restriction and re-enable the dropdown.
 *
 * Host-only; the opponent sees both halves read-only.
 */
function FormatSelector({
  isMomir,
  format,
  isHost,
  onChange,
}: {
  isMomir: boolean
  format: DeckFormat | null
  isHost: boolean
  onChange: (format: DeckFormat | null, momirBasic: boolean) => void
}) {
  const dropdownValue = isMomir ? '' : (format ?? '')
  const dropdownDisabled = !isHost || isMomir
  return (
    <div className={styles.formatSelector}>
      <div className={styles.formatSelectorTop}>
        <span className={styles.settingsLabel}>Format</span>
        <select
          value={dropdownValue}
          onChange={(e) => {
            if (!isHost) return
            const v = e.target.value
            onChange(v ? (v as DeckFormat) : null, false)
          }}
          disabled={dropdownDisabled}
          title={
            isMomir
              ? 'A custom format is active — turn it off below to restrict by a constructed format.'
              : isHost
                ? 'Restrict submitted decks to a constructed format. None = no restriction.'
                : 'Only the host can change the format'
          }
          className={`${styles.settingsButton} ${isMomir ? styles.formatDropdownInactive : ''}`}
          style={{ minWidth: 160 }}
        >
          <option value="">No restriction</option>
          <optgroup label="Constructed">
            {FORMAT_OPTIONS.map((f) => (
              <option key={f.value} value={f.value}>{f.label}</option>
            ))}
          </optgroup>
        </select>
      </div>

      <div className={styles.formatOrDivider}>or pick a custom format</div>

      <div className={styles.customFormatCards}>
        {CUSTOM_FORMATS.map((cf) => {
          // With a single Momir-only contract, "active" == the Momir flag.
          const active = cf.key === MOMIR_FORMAT_VALUE && isMomir
          return (
            <button
              key={cf.key}
              type="button"
              disabled={!isHost}
              aria-pressed={active}
              onClick={() => {
                if (!isHost) return
                // Toggle: re-clicking the active mode drops back to no restriction.
                onChange(null, !active)
              }}
              className={`${styles.customFormatCard} ${active ? styles.customFormatCardActive : ''}`}
              title={isHost ? '' : 'Only the host can change the format'}
              data-testid={`custom-format-${cf.key.toLowerCase()}`}
            >
              <span
                aria-hidden
                className={styles.customFormatIcon}
                style={{ WebkitMaskImage: `url(${cf.icon})`, maskImage: `url(${cf.icon})` }}
              />
              <span className={styles.customFormatText}>
                <span className={styles.customFormatName}>
                  {cf.name}
                  {active && <span className={styles.customFormatCheck}>✓ Selected</span>}
                </span>
                <span className={styles.customFormatDesc}>{cf.desc}</span>
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}

function PlayerRow({
  player,
  isYou,
  isLast,
}: {
  player: QuickGameLobbyPlayerView
  isYou: boolean
  isLast: boolean
}) {
  const statusClass = player.ready ? styles.playerStatusReady : styles.playerStatusJoined
  const statusText = !player.deckSelected
    ? 'Choosing deck…'
    : player.ready
      ? `✓ Ready · ${player.deckLabel}`
      : `Deck: ${player.deckLabel}`
  return (
    <div
      className={styles.playerRow}
      style={{ borderBottom: isLast ? 'none' : undefined }}
    >
      <div className={styles.playerInfo}>
        <div className={`${styles.statusDot} ${styles.statusDotOnline}`} />
        <span className={styles.playerName}>{player.playerName}</span>
        {isYou && <span className={styles.hostBadge}>You</span>}
        {player.isAi && <span className={styles.hostBadge}>AI</span>}
      </div>
      <div className={styles.playerActions}>
        <span className={`${styles.playerStatus} ${statusClass}`}>
          {statusText}
        </span>
      </div>
    </div>
  )
}

/**
 * Stable key for a deck list, used to dedupe submissions. Sorted by name so two equal decks
 * always serialize the same regardless of insertion order in the picker.
 */
function serializeDeck(deck: Record<string, number>): string {
  return Object.entries(deck)
    .filter(([, n]) => n > 0)
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([name, n]) => `${name}=${n}`)
    .join('|')
}
