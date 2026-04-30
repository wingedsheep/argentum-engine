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
import type { QuickGameLobbyPlayerView } from '@/types'
import { randomBackground } from '@/utils/background.ts'
import { DeckPicker } from './DeckPicker'
import styles from './GameUI.module.css'

export function QuickGameLobbyOverlay() {
  const lobby = useGameStore((s) => s.quickGameLobbyState)
  const availableSets = useGameStore((s) => s.availableSets)
  const submitDeck = useGameStore((s) => s.submitQuickGameLobbyDeck)
  const setReady = useGameStore((s) => s.setQuickGameLobbyReady)
  const setSetCode = useGameStore((s) => s.setQuickGameLobbySetCode)
  const leave = useGameStore((s) => s.leaveQuickGameLobby)

  // Throttle deck submissions: the picker fires several times per keystroke, but we only
  // want one network round-trip per stable choice. Also dedupe — DeckPicker re-emits its
  // current deck whenever it re-renders (e.g. when a server broadcast lands), and resending
  // an unchanged deck would server-side reset our `ready` flag and trigger another broadcast,
  // which is the spam loop we used to have.
  const pendingDeckRef = useRef<Record<string, number> | null>(null)
  const lastSubmittedKeyRef = useRef<string | null>(null)
  const debounceRef = useRef<number | null>(null)
  const [deckValid, setDeckValid] = useState<boolean>(true)
  const [copied, setCopied] = useState(false)

  const handleDeckChange = useCallback(
    (deckList: Record<string, number>) => {
      const key = serializeDeck(deckList)
      // Skip if this deck is structurally identical to the last one we submitted.
      if (key === lastSubmittedKeyRef.current) return
      pendingDeckRef.current = deckList
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      debounceRef.current = window.setTimeout(() => {
        const pending = pendingDeckRef.current
        if (!pending) return
        const pendingKey = serializeDeck(pending)
        if (pendingKey === lastSubmittedKeyRef.current) return
        lastSubmittedKeyRef.current = pendingKey
        submitDeck(pending)
      }, 250)
    },
    [submitDeck]
  )

  // Flush any pending deck on unmount so we don't drop the user's last edit.
  useEffect(() => {
    return () => {
      if (debounceRef.current !== null) window.clearTimeout(debounceRef.current)
      const pending = pendingDeckRef.current
      if (pending && serializeDeck(pending) !== lastSubmittedKeyRef.current) {
        submitDeck(pending)
      }
    }
  }, [submitDeck])

  if (!lobby) return null

  const you = lobby.players.find((p) => p.playerId === lobby.youPlayerId)
  const others = lobby.players.filter((p) => p.playerId !== lobby.youPlayerId)
  const youReady = you?.ready ?? false

  const copyLobbyId = () => {
    navigator.clipboard.writeText(lobby.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className={styles.lobbyOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <div className={styles.lobbyContent}>
        <div className={styles.lobbyHeader}>
          <div className={`${styles.lobbyFormat} ${styles.lobbyFormatSealed}`}>
            Quick Game
          </div>
          <h1 className={styles.lobbyTitle}>
            {lobby.vsAi ? 'vs AI' : 'Lobby'}
          </h1>
          <p className={styles.lobbySubtitle}>
            {lobby.vsAi
              ? 'Pick a deck and ready up — the AI starts as soon as you do.'
              : 'Share the invite code with a friend, then both players ready up.'}
          </p>
        </div>

        {!lobby.vsAi && (
          <div
            onClick={copyLobbyId}
            className={`${styles.inviteBox} ${copied ? styles.inviteBoxCopied : ''}`}
            style={{ alignSelf: 'stretch', justifyContent: 'space-between' }}
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

        <DeckPicker
          onDeckChange={handleDeckChange}
          onValidityChange={setDeckValid}
          onSetCodeChange={setSetCode}
          initialSetCode={you?.setCode ?? null}
          availableSets={availableSets}
          disabled={youReady}
        />

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
              disabled={!deckValid || !you?.deckSelected}
              title={!you?.deckSelected ? 'Pick a deck first' : ''}
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
