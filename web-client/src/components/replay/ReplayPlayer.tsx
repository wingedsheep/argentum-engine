/**
 * The replay playback surface: transport controls, scrubber, share/export actions and the
 * spectator board.
 *
 * **One surface, two entry points.** There used to be two near-identical copies of this — the
 * `/replay/:gameId` route and the overlay behind the home screen's "Game Replays" button — which
 * had already drifted: only the route knew about replay metadata, archived frames, multiplayer seat
 * labels and team stamping. This is the route's (better) version, extracted so the overlay gets all
 * of that for free and neither can drift again.
 *
 * The two *entry points* stay separate on purpose, because they are genuinely different things: the
 * route is a shareable URL that loads a public replay by id, while the overlay is an in-app screen
 * that lists games and must not navigate (navigating away would drop the WebSocket). What they
 * share is everything after "here are the frames" — which is all of this file.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import type { SpectatingState } from '@/store/slices'
import type { PublicReplayData, SpectatorStateUpdate } from '@/replay/reconstructSnapshots.ts'
import { buildReplayScenarioUrl } from '../scenario/shareScenario'
import { useViewportSize } from '@/hooks/useResponsive.ts'

const HEADER_HEIGHT = 55
const AUTOPLAY_INTERVAL_MS = 1000

export type ReplayMetadata = PublicReplayData['metadata']

export function ReplayPlayer({
  snapshots,
  gameId,
  metadata,
  onExit,
}: {
  snapshots: readonly SpectatorStateUpdate[]
  /** Replay id, used to build the share/scenario links and fetch a frame's full state. */
  gameId: string
  /** Only the public-replay route has this; the overlay passes nothing and loses only the extras. */
  metadata?: ReplayMetadata | null
  /** Back button and Escape. The route navigates home; the overlay returns to its game list. */
  onExit: () => void
}) {
  const [currentStep, setCurrentStep] = useState(0)
  const [autoPlay, setAutoPlay] = useState(false)
  /**
   * Whether frame 0 has reached the store yet.
   *
   * `GameBoard` calls a different number of hooks depending on whether there is spectating state,
   * so mounting it against an empty store and populating it a tick later crashes React with
   * "Rendered more hooks than during the previous render". Both call sites used to avoid this by
   * accident, writing frame 0 in the same batch that revealed the board. Gating the mount here
   * makes that ordering explicit and keeps it in one place.
   */
  const [primed, setPrimed] = useState(false)
  const setSpectatingState = useGameStore((s) => s.setSpectatingState)

  const writeSnapshotToStore = useCallback(
    (snapshot: SpectatorStateUpdate) => {
      const state: SpectatingState = {
        gameSessionId: snapshot.gameSessionId,
        gameState: snapshot.gameState as SpectatingState['gameState'],
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
        player1: snapshot.player1 as SpectatingState['player1'],
        player2: snapshot.player2 as SpectatingState['player2'],
        currentPhase: snapshot.currentPhase,
        activePlayerId: snapshot.activePlayerId,
        priorityPlayerId: snapshot.priorityPlayerId,
        combat: snapshot.combat as SpectatingState['combat'],
        decisionStatus: snapshot.decisionStatus as SpectatingState['decisionStatus'],
        isReplay: true,
      }
      setSpectatingState(state)
    },
    [setSpectatingState],
  )

  // Show frame 0 as soon as frames arrive, and rewind whenever a different replay is loaded.
  // The store write and `setPrimed` land in one commit, so the board's first render already has
  // state to read.
  useEffect(() => {
    setCurrentStep(0)
    setAutoPlay(false)
    const hasFrames = snapshots.length > 0
    if (hasFrames) writeSnapshotToStore(snapshots[0]!)
    setPrimed(hasFrames)
  }, [snapshots, writeSnapshotToStore])

  useEffect(() => {
    return () => { setSpectatingState(null) }
  }, [setSpectatingState])

  const goToStep = useCallback(
    (step: number) => {
      if (step < 0 || step >= snapshots.length) return
      setCurrentStep(step)
      writeSnapshotToStore(snapshots[step]!)
    },
    [snapshots, writeSnapshotToStore],
  )

  useEffect(() => {
    if (!autoPlay) return
    const timer = setInterval(() => {
      setCurrentStep((prev) => {
        const next = prev + 1
        if (next >= snapshots.length) {
          setAutoPlay(false)
          return prev
        }
        writeSnapshotToStore(snapshots[next]!)
        return next
      })
    }, AUTOPLAY_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [autoPlay, snapshots, writeSnapshotToStore])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') { e.preventDefault(); goToStep(currentStep - 1) }
      else if (e.key === 'ArrowRight') { e.preventDefault(); goToStep(currentStep + 1) }
      else if (e.key === ' ') { e.preventDefault(); setAutoPlay((p) => !p) }
      else if (e.key === 'Escape') { onExit() }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [goToStep, currentStep, onExit])

  // Measure the header so the board sits below it even when the controls wrap to a 2nd row
  // on narrow windows (otherwise the rightmost buttons overflow off-screen).
  const headerRef = useRef<HTMLDivElement>(null)
  const [headerHeight, setHeaderHeight] = useState(HEADER_HEIGHT)
  useEffect(() => {
    const el = headerRef.current
    if (!el) return
    const update = () => setHeaderHeight(el.offsetHeight)
    update()
    const ro = new ResizeObserver(update)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  // Same breakpoint as useResponsive's isMobile. The scenario/snapshot/share buttons don't fit the
  // header on phones — hide them there (they're desktop-tooling features anyway).
  const isMobile = useViewportSize().width < 640

  const [scenarioCopied, setScenarioCopied] = useState(false)
  const [replayCopied, setReplayCopied] = useState(false)
  const [downloaded, setDownloaded] = useState(false)
  const [downloadError, setDownloadError] = useState(false)

  const copyToClipboard = async (url: string, prompt: string, flag: (v: boolean) => void) => {
    try {
      await navigator.clipboard.writeText(url)
      flag(true)
      setTimeout(() => flag(false), 2500)
    } catch {
      window.prompt(prompt, url)
    }
  }

  const handleShareAsScenario = () =>
    copyToClipboard(
      buildReplayScenarioUrl(window.location.origin, gameId, currentStep),
      'Copy this scenario link',
      setScenarioCopied,
    )

  const handleShareReplay = () =>
    copyToClipboard(`${window.location.origin}/replay/${gameId}`, 'Copy this replay link', setReplayCopied)

  const handleDownloadSnapshot = async () => {
    // Download the frame's full game state as a file you can reload from the Scenario Builder.
    const r = await fetch(`/api/public/replays/${gameId}/frames/${currentStep}/full-state`)
    if (!r.ok) {
      setDownloadError(true)
      setTimeout(() => setDownloadError(false), 2500)
      return
    }
    const blob = new Blob([await r.text()], { type: 'application/json' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `scenario-${gameId}-frame${currentStep}.json`
    a.click()
    URL.revokeObjectURL(a.href)
    setDownloaded(true)
    setTimeout(() => setDownloaded(false), 2500)
  }

  const snapshot = snapshots[currentStep]
  if (!primed || !snapshot) return null

  // Older replays predate the flag; they re-simulate, so treat a missing value as reproducible.
  const stateReproducible = metadata?.stateReproducible !== false

  // Replay metadata only carries the first two seat names (legacy 2-player shape), so a 3+ player
  // game would misleadingly read "Alice vs Bob". The reconstructed snapshot's gameState carries
  // every seat in turn order — use it to list all players for multiplayer.
  const allSeats = (snapshot.gameState as SpectatingState['gameState'] | null)?.players ?? []
  const isMultiplayerReplay = allSeats.length > 2
  const matchupLabel = isMultiplayerReplay
    ? allSeats.map((p) => p.name).join('  ·  ')
    : `${metadata?.player1Name ?? snapshot.player1Name} vs ${metadata?.player2Name ?? snapshot.player2Name}`

  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
      }}
    >
      <div style={styles.replayContainer}>
        <div ref={headerRef} style={styles.replayHeader}>
          <button onClick={onExit} style={styles.backButton}>
            Back
          </button>
          <div style={styles.replayControls}>
            <button onClick={() => goToStep(currentStep - 1)} disabled={currentStep === 0} style={styles.controlButton} title="Previous (Left Arrow)">
              Prev
            </button>
            <button onClick={() => setAutoPlay(!autoPlay)} style={styles.controlButton} title="Play/Pause (Space)">
              {autoPlay ? 'Pause' : 'Play'}
            </button>
            <button onClick={() => goToStep(currentStep + 1)} disabled={currentStep >= snapshots.length - 1} style={styles.controlButton} title="Next (Right Arrow)">
              Next
            </button>
          </div>
          <div style={styles.scrubberContainer}>
            <input
              type="range"
              min={0}
              max={snapshots.length - 1}
              value={currentStep}
              onChange={(e) => goToStep(Number(e.target.value))}
              style={styles.scrubber}
            />
            <span style={styles.stepCounter}>
              {currentStep + 1} / {snapshots.length}
            </span>
          </div>
          <div style={styles.replayInfo}>
            <span style={styles.replayLabel}>{isMultiplayerReplay ? `Replay · ${allSeats.length} players` : 'Replay'}</span>
            <span style={styles.matchupText}>{matchupLabel}</span>
            {metadata?.winnerName && (
              <span style={styles.winnerText}>Winner: {metadata.winnerName}</span>
            )}
          </div>
          {/*
            Something about these frames isn't the plain case, and the badge says which. DIVERGED:
            the recorded inputs no longer re-simulate on this build, so the server served the frames
            it stored when the game was played — everything on screen is the real game, but there is
            no live game state behind it, so the scenario buttons are gone rather than merely
            failing when clicked. Otherwise the frames are an exact re-simulation of a recording
            that stops before the game did (a game long enough that recording had to give up), so
            the scenario buttons keep working and only the ending is missing.
          */}
          {metadata?.degradedReason && (
            <span style={styles.archivedBadge} title={metadata.degradedReason}>
              {metadata.fidelity === 'DIVERGED' ? 'From archive' : 'Partial recording'}
            </span>
          )}
          {!isMobile && (
            <>
              {stateReproducible && (
                <>
                  <button
                    onClick={() => void handleShareAsScenario()}
                    style={styles.scenarioButton}
                    title="Copy a short link that drops you into this exact position — full board, hands, libraries, stack, targets and mana — to play it out yourself or against the AI."
                  >
                    {scenarioCopied ? 'Copied!' : 'Share as scenario'}
                  </button>
                  <button
                    onClick={() => void handleDownloadSnapshot()}
                    style={styles.scenarioButton}
                    title="Download this exact position as a snapshot file you can reload later from the Scenario Builder ('Load file')."
                  >
                    {downloadError ? 'Failed' : downloaded ? 'Saved!' : 'Save snapshot'}
                  </button>
                </>
              )}
              <button onClick={() => void handleShareReplay()} style={styles.shareButton} title="Copy a link to this replay">
                {replayCopied ? 'Copied!' : 'Share replay'}
              </button>
            </>
          )}
        </div>
        <div style={styles.gameBoardContainer}>
          <GameBoard spectatorMode topOffset={headerHeight} />
        </div>
      </div>
      <CombatArrows />
    </SpectatorContext.Provider>
  )
}

const styles: Record<string, React.CSSProperties> = {
  replayContainer: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#0a0a12',
    display: 'flex',
    flexDirection: 'column',
    zIndex: 1500,
  },
  replayHeader: {
    display: 'flex',
    alignItems: 'center',
    flexWrap: 'wrap',
    rowGap: 8,
    padding: '10px 16px',
    borderBottom: '1px solid #1a1a25',
    backgroundColor: '#0d0d15',
    flexShrink: 0,
    zIndex: 1600,
    gap: 10,
  },
  backButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: 'transparent',
    color: '#888',
    border: '1px solid #333',
    borderRadius: 6,
    cursor: 'pointer',
    flexShrink: 0,
  },
  replayControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  controlButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#1a1a2e',
    color: '#ccc',
    border: '1px solid #2a2a3e',
    borderRadius: 4,
    cursor: 'pointer',
  },
  scrubberContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    flex: 1,
    minWidth: 0,
  },
  scrubber: {
    flex: 1,
    minWidth: 80,
    height: 4,
    appearance: 'none' as const,
    WebkitAppearance: 'none' as const,
    background: '#2a2a3e',
    borderRadius: 2,
    outline: 'none',
    cursor: 'pointer',
    accentColor: '#4fc3f7',
  },
  stepCounter: {
    color: '#888',
    fontSize: 13,
    minWidth: 70,
    flexShrink: 0,
  },
  replayInfo: {
    textAlign: 'right',
    flexShrink: 1,
    minWidth: 0,
    overflow: 'hidden',
  },
  replayLabel: {
    display: 'block',
    color: '#666',
    fontSize: 11,
    textTransform: 'uppercase',
    letterSpacing: '0.1em',
  },
  matchupText: {
    display: 'block',
    color: '#aaa',
    fontSize: 13,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  winnerText: {
    display: 'block',
    color: '#4fc3f7',
    fontSize: 11,
  },
  archivedBadge: {
    padding: '4px 8px',
    fontSize: 11,
    color: '#fbbf24',
    border: '1px solid #78550f',
    borderRadius: 4,
    backgroundColor: '#2a2008',
    whiteSpace: 'nowrap',
    cursor: 'help',
  },
  shareButton: {
    padding: '7px 10px',
    fontSize: 12,
    backgroundColor: '#1e40af',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    flexShrink: 0,
    whiteSpace: 'nowrap',
  },
  scenarioButton: {
    padding: '7px 10px',
    fontSize: 12,
    backgroundColor: '#6d28d9',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    flexShrink: 0,
    whiteSpace: 'nowrap',
  },
  gameBoardContainer: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },
}
