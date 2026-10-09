/**
 * Matchmaking UI that follows the player across every route, mounted beside the router rather than
 * inside `App`: the socket outlives a client-side navigation, so a search started on the home screen
 * keeps running while the player is in the deckbuilder or the help pages.
 *
 * - The accept prompt shows wherever they are.
 * - Wherever the home panel isn't showing — another page, a lobby, a warm-up game against the AI —
 *   a small pill says the search is still running and offers to stop it. In a game it moves to the
 *   top-left, out of the hand's way.
 * - When the pair is seated, a player on another page is taken to `/`, where `App` renders what the
 *   server just sent: the game itself, the Jump In pack choice, or the Constructed lobby.
 */
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { isAppRoute, useHomeVisible } from '@/hooks/useHomeVisible'
import { useGameStore } from '@/store/gameStore.ts'
import { MatchFoundDialog } from './MatchFoundDialog'
import styles from './Matchmaking.module.css'
import { formatWait, queueLabel } from './queues'

export default function MatchmakingLayer() {
  const location = useLocation()
  const navigate = useNavigate()
  const status = useGameStore((s) => s.matchmaking)
  const onAppRoute = isAppRoute(location.pathname)
  const inGame = useGameStore((s) => s.gameState != null || s.mulliganState != null || s.waitingForOpponentMulligan)
  // `App` shows the home hub — and with it the queue panel — only when nothing else is up.
  const homeVisible = useHomeVisible()

  // Once per seating: the status object is replaced on every server message, so a later visit to
  // another page doesn't bounce the player home again.
  const handledStatus = useRef<typeof status>(null)
  useEffect(() => {
    if (!status?.matched || handledStatus.current === status) return
    handledStatus.current = status
    if (!onAppRoute) navigate('/')
  }, [status, onAppRoute, navigate])

  return (
    <>
      <MatchFoundDialog />
      {!homeVisible && status?.searching && (
        <SearchingPill
          label={queueLabel(status.mode, status.format, status.ranked)}
          since={status.searchingSince ?? null}
          inGame={inGame}
        />
      )}
    </>
  )
}

function SearchingPill({ label, since, inGame }: { label: string; since: number | null; inGame: boolean }) {
  const leave = useGameStore((s) => s.leaveMatchmaking)
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [])

  return (
    <div
      className={`${styles.pill} ${inGame ? styles.pillInGame : ''}`}
      role="status"
      aria-live="polite"
      data-testid="matchmaking-pill"
      title={inGame ? 'When a match is found you’ll be asked first — accepting ends this game' : undefined}
    >
      <span className={styles.spinner} aria-hidden />
      <span className={styles.pillLabel}>Searching {label}</span>
      {since !== null && <span className={styles.wait}>{formatWait(now - since)}</span>}
      {inGame && <span className={styles.pillHint}>· you’ll be asked first</span>}
      <button type="button" className={styles.pillClose} onClick={leave} aria-label="Stop searching" title="Stop searching">
        ×
      </button>
    </div>
  )
}
