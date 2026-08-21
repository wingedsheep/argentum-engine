import { useState, useEffect, useCallback, useRef } from 'react'
import { useGameStore, type TournamentState } from '@/store/gameStore.ts'
import type { SealedCardInfo } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { ManaCost } from '../ui/ManaSymbols'
import { ReplayViewer, type GameSummary } from '../admin/ReplayViewer'
import type { ReplayData } from '@/replay/reconstructSnapshots.ts'
import { useDeckLibrary, buildDraftedDeckSave, type SavedDeckEntry } from '@/store/deckLibrary'
import { useSaveDeck } from '@/store/useSaveDeck'
import { deriveRoundPhase } from './roundPhase'
import styles from '../ui/GameUI.module.css'

/**
 * Tournament overlay showing standings between rounds.
 */
interface HoveredStanding {
  playerId: string
  playerName: string
  wins: number
  losses: number
  draws: number
  points: number
  gamesWon: number
  gamesLost: number
  lifeDifferential: number | undefined
  omwPercent: number | undefined
  ogwPercent: number | undefined
  tiebreakerReason: string | null | undefined
  rect: DOMRect
}

export function TournamentOverlay({
  tournamentState,
}: {
  tournamentState: TournamentState
}) {
  const playerId = useGameStore((state) => state.playerId)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)
  const addExtraRound = useGameStore((state) => state.addExtraRound)
  const leaveTournament = useGameStore((state) => state.leaveTournament)
  const unsubmitDeck = useGameStore((state) => state.unsubmitDeck)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const disconnectedPlayers = useGameStore((state) => state.disconnectedPlayers)
  const addDisconnectTime = useGameStore((state) => state.addDisconnectTime)
  const kickPlayer = useGameStore((state) => state.kickPlayer)
  const [hoveredStanding, setHoveredStanding] = useState<HoveredStanding | null>(null)
  const [linkCopied, setLinkCopied] = useState(false)
  const [showDeckViewer, setShowDeckViewer] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
  const [deckSavedAt, setDeckSavedAt] = useState<number | null>(null)
  const [saveDeckDialog, setSaveDeckDialog] = useState<{ name: string } | null>(null)
  const hydrateDeckLibrary = useDeckLibrary((s) => s.hydrate)
  const { save: saveDeckRouted, isLoggedIn } = useSaveDeck()
  useEffect(() => { hydrateDeckLibrary() }, [hydrateDeckLibrary])

  const buildDeckSave = (): { cards: Record<string, number>; entries: SavedDeckEntry[] | undefined } | null => {
    if (!deckBuildingState) return null
    const built = buildDraftedDeckSave(
      deckBuildingState.deck,
      deckBuildingState.landCounts,
      [...deckBuildingState.cardPool, ...deckBuildingState.basicLands],
    )
    return Object.keys(built.cards).length === 0 ? null : built
  }

  const openSaveDeckDialog = () => {
    if (!deckBuildingState) return
    const setNames = lobbyState?.settings.setNames?.join(' + ')
    const stamp = new Date().toLocaleDateString()
    const defaultName = setNames ? `${setNames} draft – ${stamp}` : `Drafted deck – ${stamp}`
    setSaveDeckDialog({ name: defaultName })
  }

  const confirmSaveDeck = () => {
    if (!saveDeckDialog) return
    const built = buildDeckSave()
    if (!built) return
    const name = saveDeckDialog.name.trim() || `Drafted deck – ${new Date().toLocaleDateString()}`
    // Routes to the account when signed in, localStorage otherwise — same as the deckbuilder.
    void saveDeckRouted({ name, cards: built.cards, ...(built.entries ? { entries: built.entries } : {}) })
    setSaveDeckDialog(null)
    setDeckSavedAt(Date.now())
    setTimeout(() => setDeckSavedAt(null), 2000)
  }
  // Tick every second to update disconnect countdown timers
  const [, setTick] = useState(0)
  const hasDisconnected = Object.keys(disconnectedPlayers).length > 0
  useEffect(() => {
    if (!hasDisconnected) return
    const timer = setInterval(() => setTick((t) => t + 1), 1000)
    return () => clearInterval(timer)
  }, [hasDisconnected])

  const shareLink = `${window.location.origin}/tournament/${tournamentState.lobbyId}`
  const copyShareLink = () => {
    navigator.clipboard.writeText(shareLink)
    setLinkCopied(true)
    setTimeout(() => setLinkCopied(false), 2000)
  }

  // Spectators are not in the standings list
  const isSpectator = !playerId || !tournamentState.standings.some(s => s.playerId === playerId)

  // Waiting for ready (before the first game or between matches), and — the distinction that keeps an
  // early finisher honest — whether the round they just finished is still being played elsewhere.
  const { isWaitingForReady, roundStillRunning, roundNumber, readyLabel } = deriveRoundPhase(tournamentState)

  // Check if current player is ready
  const isPlayerReady = playerId ? tournamentState.readyPlayerIds.includes(playerId) : false
  const readyCount = tournamentState.readyPlayerIds.length
  const totalPlayers = tournamentState.standings.filter(s => s.isConnected).length

  // Auto-ready when player has a bye - no need for manual confirmation
  // Exception: Don't auto-ready before first round (round 0) so player can still edit deck
  useEffect(() => {
    if (!isSpectator && isWaitingForReady && tournamentState.nextRoundHasBye && !isPlayerReady && tournamentState.currentRound > 0) {
      readyForNextRound()
    }
  }, [isSpectator, isWaitingForReady, tournamentState.nextRoundHasBye, isPlayerReady, tournamentState.currentRound, readyForNextRound])

  const fetchTournamentGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/tournament/${tournamentState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [tournamentState.lobbyId])

  const fetchTournamentReplay = useCallback(async (gameId: string): Promise<ReplayData> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}?lobbyId=${tournamentState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as ReplayData
  }, [tournamentState.lobbyId])

  if (showReplays) {
    return (
      <ReplayViewer
        fetchGames={fetchTournamentGames}
        fetchReplay={fetchTournamentReplay}
        onBack={() => setShowReplays(false)}
      />
    )
  }

  const roundLabel = !tournamentState.isComplete
    ? `Round ${roundNumber} of ${tournamentState.totalRounds}`
    : null

  return (
    <div className={styles.tournamentOverlay}>
      {/* ── Header: title + round + toolbar ── */}
      <div className={styles.trnHeader}>
        <div className={styles.trnHeaderTop}>
          <h1 className={styles.trnTitle}>
            {tournamentState.isComplete ? 'Tournament Complete' : 'Standings'}
          </h1>
          {roundLabel && <span className={styles.trnRound}>{roundLabel}</span>}
        </div>
        <div className={styles.trnToolbar}>
          <button onClick={copyShareLink} className={styles.trnToolbarBtn}>
            {linkCopied ? 'Copied!' : 'Share Link'}
          </button>
          {tournamentState.lastRoundResults && (
            <button onClick={() => setShowReplays(true)} className={styles.trnToolbarBtn}>
              Replays
            </button>
          )}
          {!isSpectator && deckBuildingState && (
            <>
              <button
                onClick={openSaveDeckDialog}
                className={styles.trnToolbarBtn}
                style={{
                  background: deckSavedAt ? 'rgba(108, 192, 74, 0.2)' : 'var(--accent-primary, #6aa3ff)',
                  borderColor: deckSavedAt ? 'rgba(108, 192, 74, 0.5)' : 'var(--accent-primary, #6aa3ff)',
                  color: '#fff',
                  fontWeight: 600,
                }}
                title={isLoggedIn ? 'Save this drafted deck to your account' : 'Save this drafted deck to your browser My Decks library'}
              >
                {deckSavedAt ? 'Saved ✓' : 'Save Deck'}
              </button>
              <button onClick={() => setShowDeckViewer(true)} className={styles.trnToolbarBtn}>
                View Deck
              </button>
            </>
          )}
        </div>
      </div>

      {/* ── Action zone: next match / ready / bye ── */}
      {!isSpectator && !tournamentState.isComplete && (
        <div className={styles.trnActionZone}>
          {/* Next opponent */}
          {isWaitingForReady && !tournamentState.nextRoundHasBye && (
            <div className={styles.statusBoxMatch}>
              <div className={styles.statusBoxMatchLabel}>Next Match</div>
              <div className={styles.statusBoxMatchOpponent}>
                {tournamentState.nextOpponentName
                  ? `vs ${tournamentState.nextOpponentName}`
                  : 'Waiting for matchup...'}
              </div>
            </div>
          )}

          {/* BYE status */}
          {((isWaitingForReady && tournamentState.nextRoundHasBye) || (tournamentState.isBye && !isWaitingForReady)) && (
            <div className={styles.statusBoxBye}>
              <span className={styles.byeIcon}>&#x2713;</span>
              <span>Sitting out this round</span>
            </div>
          )}

          {/* Ready button row */}
          {isWaitingForReady && (
            <div className={styles.trnReadyRow}>
              <button
                onClick={readyForNextRound}
                disabled={isPlayerReady}
                className={styles.readyButton}
              >
                {isPlayerReady ? '✓ Ready' : readyLabel}
              </button>
              {tournamentState.currentRound === 0 && !isPlayerReady && (
                <button onClick={unsubmitDeck} className={styles.editDeckButton}>
                  Edit Deck
                </button>
              )}
              <span className={styles.readyCount}>
                {readyCount}/{totalPlayers} ready
              </span>
            </div>
          )}

          {/* Waiting for others — including the early finisher who may already be ready: the round
              they just played is still running, so the notice belongs next to the Ready button. */}
          {(roundStillRunning || !isWaitingForReady) && !tournamentState.isBye && !tournamentState.currentMatchGameSessionId && (
            <div className={styles.statusBoxWaiting}>
              {roundStillRunning
                ? isPlayerReady
                  ? `Round ${roundNumber} is still being played — your next match starts as soon as it can.`
                  : `Round ${roundNumber} is still being played at other tables.`
                : 'Waiting for other matches to complete...'}
            </div>
          )}
        </div>
      )}

      {/* ── Disconnected players ── */}
      {Object.keys(disconnectedPlayers).length > 0 && (
        <div className={`${styles.disconnectedBanner} ${styles.trnSection}`}>
          {Object.entries(disconnectedPlayers).map(([pid, info]) => {
            const elapsed = Math.floor((Date.now() - info.disconnectedAt) / 1000)
            const remaining = Math.max(0, info.secondsRemaining - elapsed)
            const mins = Math.floor(remaining / 60)
            const secs = remaining % 60
            const canKick = elapsed >= 120
            return (
              <div key={pid} className={styles.disconnectedPlayer}>
                <span className={styles.disconnectedName}>{info.playerName} disconnected</span>
                <span className={styles.disconnectedTimer}>{mins}:{secs.toString().padStart(2, '0')}</span>
                {!isSpectator && (
                  <>
                    <button className={styles.addTimeButton} onClick={() => addDisconnectTime(pid)}>
                      +1 min
                    </button>
                    {canKick && (
                      <button className={styles.kickButton} onClick={() => kickPlayer(pid)}>
                        Kick
                      </button>
                    )}
                  </>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* ── Standings table ── */}
      <div className={`${styles.standingsTable} ${styles.trnSection}`}>
        <table className={styles.standingsTableInner}>
          <thead className={styles.standingsHeader}>
            <tr>
              <th className={styles.standingsTh}>#</th>
              <th className={styles.standingsThLeft}>Player</th>
              <th className={styles.standingsTh}>Record</th>
              <th className={styles.standingsTh}>Pts</th>
              <th className={styles.standingsTh} title="Game Win Rate">GWR</th>
              {isWaitingForReady && <th className={styles.standingsTh}>Ready</th>}
            </tr>
          </thead>
          <tbody>
            {tournamentState.standings.map((standing, index) => {
              const isMe = standing.playerId === playerId
              const isReady = tournamentState.readyPlayerIds.includes(standing.playerId)
              const displayRank = standing.rank ?? index + 1

              const gamesWon = standing.gamesWon ?? 0
              const gamesLost = standing.gamesLost ?? 0
              const totalGames = gamesWon + gamesLost
              const winRate = totalGames > 0 ? ((gamesWon / totalGames) * 100).toFixed(0) : '-'

              const handleMouseEnter = (e: React.MouseEvent<HTMLTableRowElement>) => {
                const rect = e.currentTarget.getBoundingClientRect()
                setHoveredStanding({
                  playerId: standing.playerId,
                  playerName: standing.playerName,
                  wins: standing.wins,
                  losses: standing.losses,
                  draws: standing.draws,
                  points: standing.points,
                  gamesWon: gamesWon,
                  gamesLost: gamesLost,
                  lifeDifferential: standing.lifeDifferential,
                  omwPercent: standing.omwPercent,
                  ogwPercent: standing.ogwPercent,
                  tiebreakerReason: standing.tiebreakerReason,
                  rect,
                })
              }

              return (
                <tr
                  key={standing.playerId}
                  className={`${styles.standingsRow} ${isMe ? styles.standingsRowMe : ''}`}
                  onMouseEnter={handleMouseEnter}
                  onMouseLeave={() => setHoveredStanding(null)}
                >
                  <td className={`${styles.standingsTd} ${styles.standingsRank} ${displayRank === 1 ? styles.standingsRankFirst :
                    displayRank === 2 ? styles.standingsRankSecond :
                      displayRank === 3 ? styles.standingsRankThird : ''
                    }`}>
                    {displayRank}
                    {standing.tiebreakerReason === 'TIED' && (
                      <span className={styles.tiedIndicator}>*</span>
                    )}
                  </td>
                  <td className={styles.standingsTdLeft} style={{ fontWeight: isMe ? 600 : 400 }}>
                    <span className={styles.standingsPlayerName} title={standing.playerName}>
                      {standing.playerName}
                    </span>
                    {isMe && <span className={styles.meIndicator}>(you)</span>}
                    {!standing.isConnected && (
                      <span className={styles.disconnectedIndicator}>DC</span>
                    )}
                  </td>
                  <td className={`${styles.standingsTd} ${styles.standingsRecord}`}>
                    <span className={styles.standingsWins}>{standing.wins}</span>
                    {'-'}
                    <span className={styles.standingsLosses}>{standing.losses}</span>
                    {'-'}
                    <span className={styles.standingsDraws}>{standing.draws}</span>
                  </td>
                  <td className={`${styles.standingsTd} ${styles.standingsPoints}`}>{standing.points}</td>
                  <td className={`${styles.standingsTd} ${styles.standingsGwr}`}>
                    {totalGames > 0 ? `${winRate}%` : '-'}
                  </td>
                  {isWaitingForReady && (
                    <td className={styles.standingsTd} style={{ color: isReady ? 'var(--color-success-light)' : 'var(--text-disabled)' }}>
                      {standing.isConnected ? (isReady ? '✓' : '···') : '-'}
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>

        {/* Instant hover tooltip */}
        {hoveredStanding && (
          <div
            className={styles.standingsTooltip}
            style={{
              top: hoveredStanding.rect.top + hoveredStanding.rect.height / 2,
              left: hoveredStanding.rect.right + 12,
              transform: 'translateY(-50%)',
            }}
          >
            <div className={styles.tooltipName}>{hoveredStanding.playerName}</div>
            <div className={styles.tooltipStat}>
              <span className={styles.tooltipStatLabel}>Match Record</span>
              <span className={styles.tooltipStatValue}>
                {hoveredStanding.wins}W-{hoveredStanding.losses}L-{hoveredStanding.draws}D
              </span>
            </div>
            <div className={styles.tooltipStat}>
              <span className={styles.tooltipStatLabel}>Points</span>
              <span className={styles.tooltipStatValue}>{hoveredStanding.points}</span>
            </div>
            {(hoveredStanding.gamesWon + hoveredStanding.gamesLost) > 0 && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Game Win % (GW%)</span>
                <span className={styles.tooltipStatValue}>
                  {hoveredStanding.gamesWon}-{hoveredStanding.gamesLost} (
                  {((hoveredStanding.gamesWon / (hoveredStanding.gamesWon + hoveredStanding.gamesLost)) * 100).toFixed(0)}%)
                </span>
              </div>
            )}
            {hoveredStanding.omwPercent !== undefined && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Opp. Match Win % (OMW%)</span>
                <span className={styles.tooltipStatValue}>{(hoveredStanding.omwPercent * 100).toFixed(1)}%</span>
              </div>
            )}
            {hoveredStanding.ogwPercent !== undefined && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Opp. Game Win % (OGW%)</span>
                <span className={styles.tooltipStatValue}>{(hoveredStanding.ogwPercent * 100).toFixed(1)}%</span>
              </div>
            )}
            {hoveredStanding.lifeDifferential !== undefined && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Life Diff</span>
                <span className={styles.tooltipStatValue}>
                  {hoveredStanding.lifeDifferential >= 0 ? '+' : ''}{hoveredStanding.lifeDifferential}
                </span>
              </div>
            )}
            {hoveredStanding.tiebreakerReason && hoveredStanding.tiebreakerReason !== 'TIED' && (
              <div className={styles.tooltipTiebreaker}>
                {hoveredStanding.tiebreakerReason === 'OMW'
                  ? "Ranked by opponents' match-win %"
                  : hoveredStanding.tiebreakerReason === 'GW'
                    ? 'Ranked by game-win %'
                    : hoveredStanding.tiebreakerReason === 'OGW'
                      ? "Ranked by opponents' game-win %"
                      : null}
              </div>
            )}
            {hoveredStanding.tiebreakerReason === 'TIED' && (
              <div className={styles.tooltipTiebreaker}>Tied</div>
            )}
          </div>
        )}
      </div>

      {/* ── Live matches ── */}
      {!tournamentState.currentMatchGameSessionId && tournamentState.activeMatches && tournamentState.activeMatches.length > 0 && (
        <div className={`${styles.matchesSection} ${styles.trnSection}`}>
          <h3 className={styles.matchesSectionTitle}>
            Live Matches
          </h3>
          <div className={styles.matchesList}>
            {tournamentState.activeMatches.map((match) => (
              <button
                key={match.gameSessionId}
                onClick={() => spectateGame(match.gameSessionId)}
                className={styles.matchButton}
              >
                <div className={styles.matchPlayers}>
                  <span className={styles.matchPlayerName}>{match.player1Name}</span>
                  <span className={styles.matchVs}>vs</span>
                  <span className={styles.matchPlayerName}>{match.player2Name}</span>
                </div>
                <div className={styles.matchScore}>
                  <span className={match.player1Life <= 5 ? styles.matchLifeLow : styles.matchLifeHigh}>
                    {match.player1Life}
                  </span>
                  <span className={styles.matchScoreDash}>-</span>
                  <span className={match.player2Life <= 5 ? styles.matchLifeLow : styles.matchLifeHigh}>
                    {match.player2Life}
                  </span>
                  <span className={styles.matchWatch}>▶ Watch</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Last round results ── */}
      {tournamentState.lastRoundResults && (
        <div className={`${styles.resultsSection} ${styles.trnSection}`}>
          <h3 className={styles.resultsSectionTitle}>
            Round {tournamentState.currentRound} Results
          </h3>
          {tournamentState.lastRoundResults.map((result, i) => (
            <div key={i} className={styles.resultRow}>
              <span>{result.player1Name}</span>
              <span className={styles.resultOutcome}>
                {result.isBye
                  ? 'BYE'
                  : result.isDraw
                    ? 'Draw'
                    : `Winner: ${result.winnerId === result.player1Id ? result.player1Name : result.player2Name}`}
              </span>
              <span>{result.isBye ? '' : result.player2Name}</span>
            </div>
          ))}
        </div>
      )}

      {/* ── Footer actions ── */}
      <div className={styles.trnFooter}>
        {tournamentState.isComplete ? (
          <>
            {lobbyState?.isHost && (
              <button onClick={addExtraRound} className={styles.readyButton}>
                Add Round
              </button>
            )}
            <button onClick={leaveTournament} className={styles.returnButton}>
              Return to Menu
            </button>
          </>
        ) : (
          <button
            onClick={() => {
              if (confirmLeave) {
                leaveTournament()
              } else {
                setConfirmLeave(true)
                setTimeout(() => setConfirmLeave(false), 3000)
              }
            }}
            className={confirmLeave ? styles.leaveButtonConfirm : styles.leaveButton}
          >
            {confirmLeave ? 'Confirm Leave?' : 'Leave Tournament'}
          </button>
        )}
      </div>

      {/* Deck Viewer Modal */}
      {showDeckViewer && deckBuildingState && (
        <DeckViewerModal
          deckBuildingState={deckBuildingState}
          onClose={() => setShowDeckViewer(false)}
        />
      )}

      {/* Save Deck dialog */}
      {saveDeckDialog && (
        <SaveDeckDialog
          name={saveDeckDialog.name}
          online={isLoggedIn}
          onNameChange={(name) => setSaveDeckDialog({ name })}
          onCancel={() => setSaveDeckDialog(null)}
          onConfirm={confirmSaveDeck}
        />
      )}
    </div>
  )
}

function SaveDeckDialog({
  name,
  online,
  onNameChange,
  onConfirm,
  onCancel,
}: {
  name: string
  online: boolean
  onNameChange: (name: string) => void
  onConfirm: () => void
  onCancel: () => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  useEffect(() => {
    // Focus + select on mount so the user can immediately overtype the suggested name.
    inputRef.current?.focus()
    inputRef.current?.select()
  }, [])
  return (
    <div className={styles.deckViewerBackdrop} onClick={onCancel}>
      <div
        className={styles.deckViewerPanel}
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: 480 }}
      >
        <div className={styles.deckViewerHeader}>
          <h3 className={styles.deckViewerTitle}>Save Deck</h3>
          <button className={styles.deckViewerClose} onClick={onCancel}>
            &#x2715;
          </button>
        </div>
        <div style={{ padding: 'var(--space-4) var(--space-5)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <label style={{ fontSize: 'var(--font-sm)', color: 'var(--text-faint)' }}>
            Deck name
          </label>
          <p style={{ margin: 0, fontSize: 'var(--font-xs, 0.75rem)', color: 'var(--text-faint)' }}>
            {online
              ? 'Saving to your account — available on any device you sign in from.'
              : 'Saving to this browser. Sign in to save decks to your account.'}
          </p>
          <input
            ref={inputRef}
            type="text"
            value={name}
            onChange={(e) => onNameChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') onConfirm()
              if (e.key === 'Escape') onCancel()
            }}
            placeholder="Deck name"
            style={{
              background: 'rgba(0, 0, 0, 0.3)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              borderRadius: 'var(--radius-sm)',
              padding: 'var(--space-2) var(--space-3)',
              color: 'var(--text-primary)',
              fontSize: '0.95rem',
            }}
          />
          <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
            <button
              type="button"
              onClick={onCancel}
              className={styles.leaveButton}
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={!name.trim()}
              className={styles.startButton}
            >
              Save to My Decks
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * Read-only deck viewer modal for viewing submitted deck during tournament.
 */
function DeckViewerModal({
  deckBuildingState,
  onClose,
}: {
  deckBuildingState: { cardPool: readonly SealedCardInfo[]; basicLands: readonly SealedCardInfo[]; deck: readonly string[]; landCounts: Record<string, number> }
  onClose: () => void
}) {
  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const hydrateDeckLibrary = useDeckLibrary((s) => s.hydrate)
  const { save: saveDeckRouted, isLoggedIn } = useSaveDeck()
  useEffect(() => { hydrateDeckLibrary() }, [hydrateDeckLibrary])
  const defaultDeckName = `Drafted ${new Date().toLocaleDateString()} ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  const [saveName, setSaveName] = useState(defaultDeckName)
  const [savedAt, setSavedAt] = useState<number | null>(null)

  const handleHover = (card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }

  const cardInfoMap = new Map<string, SealedCardInfo>()
  for (const card of deckBuildingState.cardPool) {
    cardInfoMap.set(card.name, card)
  }
  for (const land of deckBuildingState.basicLands) {
    cardInfoMap.set(land.name, land)
  }

  // Count cards in deck
  const deckCounts = new Map<string, number>()
  for (const name of deckBuildingState.deck) {
    deckCounts.set(name, (deckCounts.get(name) ?? 0) + 1)
  }

  // Calculate CMC for a card
  const getCmc = (card: SealedCardInfo): number => {
    if (!card.manaCost) return 0
    let cmc = 0
    for (const match of card.manaCost.matchAll(/\{([^}]+)\}/g)) {
      const sym = match[1] ?? ''
      const num = parseInt(sym, 10)
      if (!isNaN(num)) cmc += num
      else if (sym !== 'X') cmc += 1
    }
    return cmc
  }

  // Group cards by CMC
  const grouped = new Map<number, { card: SealedCardInfo; count: number }[]>()
  for (const [name, count] of deckCounts) {
    const card = cardInfoMap.get(name)
    if (!card) continue
    const cmc = getCmc(card)
    if (!grouped.has(cmc)) grouped.set(cmc, [])
    grouped.get(cmc)!.push({ card, count })
  }
  // Sort groups by CMC, cards within by name
  const sortedGroups = [...grouped.entries()].sort((a, b) => a[0] - b[0])
  for (const [, cards] of sortedGroups) {
    cards.sort((a, b) => a.card.name.localeCompare(b.card.name))
  }

  // Calculate stats
  const totalLands = Object.values(deckBuildingState.landCounts).reduce((s, n) => s + n, 0)
  const totalSpellCards = deckBuildingState.deck.length
  let creatures = 0
  let nonCreatureSpells = 0
  for (const [name, count] of deckCounts) {
    const card = cardInfoMap.get(name)
    if (!card) continue
    if (card.typeLine.toLowerCase().includes('creature')) creatures += count
    else nonCreatureSpells += count
  }
  const totalCards = totalSpellCards + totalLands

  return (
    <div className={styles.deckViewerBackdrop} onClick={onClose}>
      <div className={styles.deckViewerPanel} onClick={(e) => e.stopPropagation()}>
        <div className={styles.deckViewerHeader}>
          <h3 className={styles.deckViewerTitle}>Your Deck ({totalCards})</h3>
          <button className={styles.deckViewerClose} onClick={onClose}>
            &#x2715;
          </button>
        </div>
        <div className={styles.deckViewerBody}>
          {/* Stats */}
          <div className={styles.deckViewerStats}>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{creatures}</span>
              <span className={styles.deckViewerStatLabel}>Creatures</span>
            </div>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{nonCreatureSpells}</span>
              <span className={styles.deckViewerStatLabel}>Spells</span>
            </div>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{totalLands}</span>
              <span className={styles.deckViewerStatLabel}>Lands</span>
            </div>
          </div>

          {/* Card list grouped by CMC */}
          {sortedGroups.map(([cmc, cards]) => (
            <div key={cmc} className={styles.deckViewerGroup}>
              <div className={styles.deckViewerGroupHeader}>
                {cmc === 0 ? 'CMC 0' : `CMC ${cmc}`} ({cards.reduce((s, c) => s + c.count, 0)})
              </div>
              {cards.map(({ card, count }) => (
                <div
                  key={card.name}
                  className={styles.deckViewerRow}
                  onMouseEnter={(e) => handleHover(card, e)}
                  onMouseMove={(e) => handleHover(card, e)}
                  onMouseLeave={() => handleHover(null)}
                >
                  <span className={styles.deckViewerCount}>{count}</span>
                  <span className={styles.deckViewerCardName}>{card.name}</span>
                  <span className={styles.deckViewerManaCost}>
                    {card.manaCost
                      ? <ManaCost cost={card.manaCost} size={12} />
                      : <span style={{ color: '#666', fontSize: 10 }}>({getCmc(card)})</span>}
                  </span>
                </div>
              ))}
            </div>
          ))}

          {/* Basic lands */}
          {totalLands > 0 && (
            <div className={styles.deckViewerLandsSection}>
              <div className={styles.deckViewerGroupHeader}>
                Lands ({totalLands})
              </div>
              {Object.entries(deckBuildingState.landCounts)
                .filter(([, count]) => count > 0)
                .map(([name, count]) => {
                  const landCard = cardInfoMap.get(name)
                  return (
                    <div
                      key={name}
                      className={styles.deckViewerLandRow}
                      onMouseEnter={(e) => landCard && handleHover(landCard, e)}
                      onMouseMove={(e) => landCard && handleHover(landCard, e)}
                      onMouseLeave={() => handleHover(null)}
                    >
                      <span className={styles.deckViewerCount}>{count}</span>
                      <span className={styles.deckViewerLandName}>{name}</span>
                    </div>
                  )
                })}
            </div>
          )}
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: 'var(--space-3) var(--space-5)',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          }}
        >
          <input
            type="text"
            value={saveName}
            onChange={(e) => setSaveName(e.target.value)}
            placeholder="Deck name"
            style={{
              flex: 1,
              background: 'rgba(0, 0, 0, 0.3)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: 'var(--radius-sm)',
              padding: 'var(--space-1) var(--space-2)',
              color: 'var(--text-primary)',
              fontSize: '0.85rem',
            }}
          />
          <button
            type="button"
            onClick={() => {
              // Combine non-basic deck cards with basic-land counts, pinning each to the
              // printing it was drafted as so the saved deck keeps the exact art/printing.
              const { cards, entries } = buildDraftedDeckSave(
                deckBuildingState.deck,
                deckBuildingState.landCounts,
                [...deckBuildingState.cardPool, ...deckBuildingState.basicLands],
              )
              // Routes to the account when signed in, localStorage otherwise.
              void saveDeckRouted({ name: saveName.trim() || defaultDeckName, cards, ...(entries ? { entries } : {}) })
              setSavedAt(Date.now())
              setTimeout(() => setSavedAt(null), 2000)
            }}
            disabled={totalCards === 0}
            className={styles.startButton}
            style={{ flexShrink: 0 }}
            title={isLoggedIn ? 'Save to your account' : 'Save to your browser My Decks library'}
          >
            {savedAt ? 'Saved ✓' : isLoggedIn ? 'Save to My Decks (online)' : 'Save to My Decks'}
          </button>
        </div>
      </div>

      {/* Card image preview on hover */}
      {hoveredCard && <DeckViewerCardPreview card={hoveredCard} pos={hoverPos} />}
    </div>
  )
}

/**
 * Card image preview that follows the cursor, shown when hovering cards in the deck viewer.
 */
function DeckViewerCardPreview({ card, pos }: { card: SealedCardInfo; pos: { x: number; y: number } | null }) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 250
  const previewHeight = Math.round(previewWidth * 1.4)

  let top = 80
  let left = 20
  if (pos) {
    const margin = 20
    if (pos.x + previewWidth + margin + 20 < window.innerWidth) {
      left = pos.x + margin
    } else {
      left = pos.x - previewWidth - margin
    }
    top = Math.max(10, Math.min(pos.y - previewHeight / 2, window.innerHeight - previewHeight - 10))
  }

  return (
    <div
      style={{
        position: 'fixed',
        top,
        left,
        pointerEvents: 'none',
        zIndex: 1002,
        transition: 'top 0.05s, left 0.05s',
      }}
    >
      <div
        style={{
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8)',
        }}
      >
        <img
          src={imageUrl}
          alt={card.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </div>
    </div>
  )
}
