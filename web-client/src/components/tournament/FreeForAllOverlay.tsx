import { useState, useCallback } from 'react'
import { useGameStore, type FfaState } from '@/store/gameStore.ts'
import { ReplayViewer, type GameSummary } from '../admin/ReplayViewer'
import type { ReplayData } from '@/replay/reconstructSnapshots.ts'
import styles from '../ui/GameUI.module.css'

/**
 * Free-for-All pod overlay — the FFA-mode counterpart of [TournamentOverlay]. Shown between
 * games: standings of the last game (placement order), a "Play Again" ready loop, and the
 * usual share/replays/deck/leave toolbar. During a game the board renders instead.
 */
export function FreeForAllOverlay({ ffaState }: { ffaState: FfaState }) {
  const playerId = useGameStore((state) => state.playerId)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)
  const unsubmitDeck = useGameStore((state) => state.unsubmitDeck)
  const leaveTournament = useGameStore((state) => state.leaveTournament)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const [linkCopied, setLinkCopied] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)

  const shareLink = `${window.location.origin}/tournament/${ffaState.lobbyId}`
  const copyShareLink = () => {
    navigator.clipboard.writeText(shareLink)
    setLinkCopied(true)
    setTimeout(() => setLinkCopied(false), 2000)
  }

  const fetchPodGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/tournament/${ffaState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [ffaState.lobbyId])

  const fetchPodReplay = useCallback(async (gameId: string): Promise<ReplayData> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}?lobbyId=${ffaState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as ReplayData
  }, [ffaState.lobbyId])

  if (showReplays) {
    return (
      <ReplayViewer
        fetchGames={fetchPodGames}
        fetchReplay={fetchPodReplay}
        onBack={() => setShowReplays(false)}
      />
    )
  }

  const isSpectator = !playerId || !lobbyState?.players.some((p) => p.playerId === playerId)
  const gameInProgress = ffaState.currentGameSessionId != null
  const isPlayerReady = playerId ? ffaState.readyPlayerIds.includes(playerId) : false
  const readyCount = ffaState.readyPlayerIds.length
  const totalPlayers = lobbyState?.players.filter((p) => p.isConnected).length
    ?? ffaState.standings?.length ?? 0

  return (
    <div className={styles.tournamentOverlay}>
      {/* ── Header ── */}
      <div className={styles.trnHeader}>
        <div className={styles.trnHeaderTop}>
          <h1 className={styles.trnTitle}>Free-for-All</h1>
          <span className={styles.trnRound}>
            {gameInProgress
              ? `Game ${ffaState.gameNumber} in progress`
              : ffaState.gamesPlayed > 0
                ? `After game ${ffaState.gamesPlayed}`
                : 'Waiting to start'}
          </span>
        </div>
        <div className={styles.trnToolbar}>
          <button onClick={copyShareLink} className={styles.trnToolbarBtn}>
            {linkCopied ? 'Copied!' : 'Share Link'}
          </button>
          {ffaState.gamesPlayed > 0 && (
            <button onClick={() => setShowReplays(true)} className={styles.trnToolbarBtn}>
              Replays
            </button>
          )}
          {gameInProgress && isSpectator && ffaState.currentGameSessionId && (
            <button
              onClick={() => spectateGame(ffaState.currentGameSessionId!)}
              className={styles.trnToolbarBtn}
            >
              Watch Game
            </button>
          )}
        </div>
      </div>

      {/* ── Action zone: play again / waiting ── */}
      {!isSpectator && !gameInProgress && (
        <div className={styles.trnActionZone}>
          <div className={styles.trnReadyRow}>
            <button
              onClick={readyForNextRound}
              disabled={isPlayerReady}
              className={styles.readyButton}
            >
              {isPlayerReady ? '✓ Ready' : ffaState.gamesPlayed > 0 ? 'Play Again' : 'Ready'}
            </button>
            {!isPlayerReady && deckBuildingState && (
              <button onClick={unsubmitDeck} className={styles.editDeckButton}>
                Edit Deck
              </button>
            )}
            <span className={styles.readyCount}>
              {readyCount}/{totalPlayers} ready
            </span>
          </div>
        </div>
      )}
      {gameInProgress && !isSpectator && (
        <div className={`${styles.statusBoxWaiting} ${styles.trnSection}`}>
          Game in progress...
        </div>
      )}

      {/* ── Standings (placement order of the last game) ── */}
      {ffaState.standings && (
        <div className={`${styles.standingsTable} ${styles.trnSection}`}>
          <table className={styles.standingsTableInner}>
            <thead className={styles.standingsHeader}>
              <tr>
                <th className={styles.standingsTh}>#</th>
                <th className={styles.standingsThLeft}>Player</th>
                <th className={styles.standingsTh}>Result</th>
              </tr>
            </thead>
            <tbody>
              {ffaState.standings.map((standing) => {
                const isMe = standing.playerId === playerId
                return (
                  <tr
                    key={standing.playerId}
                    className={`${styles.standingsRow} ${isMe ? styles.standingsRowMe : ''}`}
                  >
                    <td className={`${styles.standingsTd} ${styles.standingsRank} ${
                      standing.placement === 1 ? styles.standingsRankFirst :
                      standing.placement === 2 ? styles.standingsRankSecond :
                      standing.placement === 3 ? styles.standingsRankThird : ''
                    }`}>
                      {standing.placement}
                    </td>
                    <td className={styles.standingsTdLeft} style={{ fontWeight: isMe ? 600 : 400 }}>
                      <span className={styles.standingsPlayerName} title={standing.playerName}>
                        {standing.playerName}
                      </span>
                      {isMe && <span className={styles.meIndicator}>(you)</span>}
                    </td>
                    <td className={styles.standingsTd}>
                      {standing.placement === 1 ? 'Winner' : `${ordinal(standing.placement)} place`}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      {!ffaState.standings && !gameInProgress && (
        <div className={`${styles.statusBoxWaiting} ${styles.trnSection}`}>
          Waiting for all players to be ready...
        </div>
      )}

      {/* ── Leave ── */}
      <div className={styles.trnSection} style={{ display: 'flex', justifyContent: 'center', gap: 8 }}>
        {!confirmLeave ? (
          <button onClick={() => setConfirmLeave(true)} className={styles.leaveButton}>
            Leave
          </button>
        ) : (
          <>
            <button onClick={leaveTournament} className={styles.leaveButton}>
              Confirm Leave
            </button>
            <button onClick={() => setConfirmLeave(false)} className={styles.trnToolbarBtn}>
              Cancel
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function ordinal(n: number): string {
  if (n % 100 >= 11 && n % 100 <= 13) return `${n}th`
  switch (n % 10) {
    case 1: return `${n}st`
    case 2: return `${n}nd`
    case 3: return `${n}rd`
    default: return `${n}th`
  }
}
