import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import {
  AI_INSIGHT_ERROR,
  AI_INSIGHT_UNAVAILABLE,
  clearAiInsight,
  downloadAiInsightExport,
  fetchAiInsight,
  resumeAi,
  setAiStepMode,
  type AiActionOption,
  type AiInsightDecision,
  type AiInsightListResponse,
} from '@/api/aiInsight'

/**
 * Local testing mode: browse the actions the AI weighed on each decision ranked by how strongly it
 * preferred them, hold it before it moves so you can play a different option, and export a position
 * plus its ratings as AI-training input.
 *
 * Renders nothing unless the server runs the mode (`game.ai.insight-enabled`) — the route 404s
 * otherwise and the component stays invisible for the rest of the game. It polls slowly while
 * closed (so the button's count is honest) and quickly while open.
 *
 * Scores are the AI's raw board-evaluator units. They have no absolute meaning; what's readable is
 * the ordering and the distance from the baseline row (passing / not attacking), which is the
 * threshold an option had to clear to be taken at all.
 */
export function AiInsightPanel() {
  const playerId = useGameStore((state) => state.playerId)
  const [available, setAvailable] = useState<boolean | null>(null)
  const [expanded, setExpanded] = useState(false)
  const [data, setData] = useState<AiInsightListResponse | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Tracked in state because the rows are inline-styled buttons, and Chrome's default button hover
  // is a near-white fill that makes the dark panel unreadable under the pointer.
  const [hoveredId, setHoveredId] = useState<number | null>(null)

  const decisions = data?.decisions ?? []
  const pending = data?.pending ?? null
  const stepMode = data?.stepMode ?? false
  const pendingId = pending?.decisionId ?? null

  // Requests are issued from a 700ms poll *and* from every button, so they overlap and can resolve
  // out of order. Without this, a poll that left before you toggled step mode can answer after it
  // and paint the old value back — the toggle appears to bounce, and the panel disagrees with the
  // server. Only the newest request may write.
  const requestSeq = useRef(0)

  const refresh = useCallback(async () => {
    if (!playerId) return
    const seq = ++requestSeq.current
    const result = await fetchAiInsight(playerId)
    if (seq !== requestSeq.current) return
    // A blip leaves `available` alone: one dropped request must not hide the panel for the game.
    if (result === AI_INSIGHT_ERROR) return
    if (result === AI_INSIGHT_UNAVAILABLE) {
      setAvailable(false)
      return
    }
    setAvailable(true)
    setData(result)
  }, [playerId])

  useEffect(() => {
    if (!playerId || available === false) return
    void refresh()
    // The poll doubles as the server's watchdog heartbeat: step mode only holds the AI while a
    // client is demonstrably watching, so a closed tab can never strand a game at the AI's turn.
    const timer = window.setInterval(() => void refresh(), expanded ? 700 : 5000)
    return () => window.clearInterval(timer)
  }, [playerId, expanded, available, refresh])

  // A held AI is the one thing worth looking at, so it takes the detail pane whatever was pinned.
  useEffect(() => {
    if (pendingId !== null) setSelectedId(pendingId)
  }, [pendingId])

  const latest = decisions[0] ?? null
  const selected =
    selectedId === null ? latest : decisions.find((d) => d.id === selectedId) ?? latest
  const isHeld = pendingId !== null && selected?.id === pendingId

  const run = useCallback(
    async (work: () => Promise<void>) => {
      setBusy(true)
      setError(null)
      try {
        await work()
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Something went wrong')
      } finally {
        setBusy(false)
      }
      await refresh()
    },
    [refresh],
  )

  if (!playerId || available !== true) return null

  if (!expanded) {
    return (
      <button
        onClick={() => setExpanded(true)}
        style={{ ...styles.toggleButton, ...(pending ? styles.toggleButtonHeld : null) }}
        title={pending ? 'The AI is waiting for you' : 'Browse what the AI considered'}
      >
        {pending ? '⏸ AI waiting' : `AI Insight (${decisions.length})`}
      </button>
    )
  }

  return (
    <div style={styles.panel}>
      <div style={styles.header}>
        <span style={styles.headerTitle}>AI Insight</span>
        <div style={styles.headerActions}>
          <button
            style={{ ...styles.actionButton, ...(stepMode ? styles.actionButtonOn : null) }}
            disabled={busy}
            onClick={() => void run(() => setAiStepMode(playerId, !stepMode))}
            title={
              stepMode
                ? 'The AI stops before each move it weighed. Click to let it play freely.'
                : 'Stop the AI before each move it weighed, so you can inspect or replace it.'
            }
          >
            {stepMode ? '⏸ Stepping' : '▶ Playing'}
          </button>
          <button
            style={styles.actionButton}
            disabled={busy || !selected}
            onClick={() => void run(() => downloadAiInsightExport(playerId, selected?.id))}
            title="Download this position and every rating the AI gave it"
          >
            Export
          </button>
          <button
            style={styles.actionButton}
            disabled={busy || decisions.length === 0}
            onClick={() =>
              void run(async () => {
                await clearAiInsight(playerId)
                setSelectedId(null)
              })
            }
            title="Drop the recorded history for this game"
          >
            Clear
          </button>
          <button onClick={() => setExpanded(false)} style={styles.closeButton} title="Close">
            &times;
          </button>
        </div>
      </div>

      {error && <div style={styles.error}>{error}</div>}

      {pending && (
        <div style={styles.heldBanner}>
          <div style={styles.heldTitle}>
            Holding — the AI wants to play <strong>{pending.proposedLabel}</strong>
          </div>
          <div style={styles.heldHint}>
            Let it through, or play any option below to see what that line does instead.
          </div>
          <button
            style={styles.heldButton}
            disabled={busy}
            onClick={() => void run(() => resumeAi(playerId))}
          >
            Let the AI play it
          </button>
        </div>
      )}

      {decisions.length === 0 ? (
        <div style={styles.empty}>
          Nothing recorded yet. Decisions appear once the AI has a real choice to make — a window
          with only one legal action is taken without weighing anything.
        </div>
      ) : (
        <>
          <div style={styles.timeline}>
            {decisions.map((decision) => {
              const isSelected = decision.id === selected?.id
              const held = pendingId === decision.id
              return (
                <button
                  key={decision.id}
                  // Clicking the pinned row unpins it and resumes following the latest decision.
                  onClick={() =>
                    setSelectedId(selectedId !== null && isSelected ? null : decision.id)
                  }
                  onMouseEnter={() => setHoveredId(decision.id)}
                  onMouseLeave={() =>
                    setHoveredId((current) => (current === decision.id ? null : current))
                  }
                  style={{
                    ...styles.timelineRow,
                    ...(isSelected ? styles.timelineRowSelected : null),
                    ...(hoveredId === decision.id ? styles.timelineRowHovered : null),
                  }}
                >
                  <span style={styles.timelineTurn}>
                    {held && <span style={styles.heldDot}>⏸ </span>}
                    T{decision.insight.turnNumber} {kindTag(decision.insight.kind)}
                  </span>
                  <span style={styles.timelineChoice}>
                    {decision.humanOverride?.label ?? decision.insight.chosenLabel}
                  </span>
                  {decision.humanOverride && <span style={styles.overrideTag}>yours</span>}
                </button>
              )
            })}
          </div>

          {selected && (
            <DecisionDetail
              decision={selected}
              pinned={selectedId !== null && !isHeld}
              held={isHeld}
              busy={busy}
              onPlay={(index) => void run(() => resumeAi(playerId, index))}
            />
          )}
        </>
      )}
    </div>
  )
}

function DecisionDetail({
  decision,
  pinned,
  held,
  busy,
  onPlay,
}: {
  decision: AiInsightDecision
  pinned: boolean
  held: boolean
  busy: boolean
  onPlay: (optionIndex: number) => void
}) {
  const { insight } = decision
  // Bar width is relative to the biggest swing in this decision: the interesting question is
  // "how much better than the alternatives", and a fixed scale flattens every close call.
  const maxMagnitude = useMemo(() => {
    const values = insight.options
      .map((option) => option.advantage)
      .filter((value): value is number => value !== null)
      .map(Math.abs)
    return Math.max(0.01, ...values)
  }, [insight.options])

  return (
    <div style={styles.detail}>
      <div style={styles.detailMeta}>
        Turn {insight.turnNumber} · {formatStep(insight.step)} ·{' '}
        {insight.onOwnTurn ? "AI's turn" : 'your turn'} · {insight.thinkTimeMs}ms
        {pinned && <span style={styles.pinned}> · pinned</span>}
      </div>
      <div style={styles.detailMeta}>
        Baseline: {insight.baselineLabel} at {insight.baselineScore.toFixed(2)} — an option is only
        taken if it scores above this.
      </div>
      {decision.humanOverride && (
        <div style={styles.overrideNote}>
          You played <strong>{decision.humanOverride.label}</strong> instead of the AI's{' '}
          <strong>{insight.chosenLabel}</strong>.
        </div>
      )}
      <div style={styles.options}>
        {insight.options.map((option, index) => (
          <OptionRow
            key={`${option.label}-${index}`}
            option={option}
            optionIndex={index}
            maxMagnitude={maxMagnitude}
            playedByHuman={decision.humanOverride?.optionIndex === index}
            canPlay={held && option.action !== undefined && option.action !== null}
            busy={busy}
            onPlay={() => onPlay(index)}
          />
        ))}
      </div>
    </div>
  )
}

function OptionRow({
  option,
  optionIndex,
  maxMagnitude,
  playedByHuman,
  canPlay,
  busy,
  onPlay,
}: {
  option: AiActionOption
  optionIndex: number
  maxMagnitude: number
  playedByHuman: boolean
  canPlay: boolean
  busy: boolean
  onPlay: () => void
}) {
  const unscored = option.score === null
  const advantage = option.advantage ?? 0
  const barWidth = unscored ? 0 : Math.min(100, (Math.abs(advantage) / maxMagnitude) * 100)
  const positive = advantage >= 0

  return (
    <div
      data-testid={`ai-insight-option-${optionIndex}`}
      style={{
        ...styles.option,
        ...(option.chosen ? styles.optionChosen : null),
        ...(option.baseline ? styles.optionBaseline : null),
        ...(playedByHuman ? styles.optionPlayed : null),
        ...(unscored ? styles.optionUnscored : null),
      }}
    >
      <div style={styles.optionTop}>
        <span style={styles.optionRank}>{unscored ? '·' : optionIndex + 1}</span>
        <span style={styles.optionLabel} title={option.label}>
          {option.label}
        </span>
        {option.chosen && <span style={styles.aiPickTag}>AI pick</span>}
        {playedByHuman && <span style={styles.playedTag}>played</span>}
        <span
          style={{
            ...styles.optionScore,
            color: unscored ? '#666' : positive ? '#6ec98b' : '#d08a7a',
          }}
        >
          {unscored ? '—' : `${advantage >= 0 ? '+' : ''}${advantage.toFixed(2)}`}
        </span>
        {canPlay && (
          <button
            data-testid={`ai-insight-play-${optionIndex}`}
            style={styles.playButton}
            disabled={busy}
            onClick={onPlay}
            title="Submit this move instead of the AI's"
          >
            Play
          </button>
        )}
      </div>
      {!unscored && (
        <div style={styles.barTrack}>
          <div
            style={{
              ...styles.barFill,
              // Zero sits at the track's midpoint: the baseline row is the reference, and a
              // rejected option reads as "below the line" rather than as a short green bar.
              width: `${barWidth / 2}%`,
              marginLeft: positive ? '50%' : `${50 - barWidth / 2}%`,
              backgroundColor: positive ? 'rgba(110, 201, 139, 0.55)' : 'rgba(208, 138, 122, 0.55)',
            }}
          />
        </div>
      )}
      {(option.note || option.rawScore !== null) && (
        <div style={styles.optionNote}>
          {option.rawScore !== null && `board score ${option.rawScore.toFixed(2)}`}
          {option.rawScore !== null && option.note ? ' · ' : ''}
          {option.note}
        </div>
      )}
    </div>
  )
}

function kindTag(kind: string): string {
  switch (kind) {
    case 'DECLARE_ATTACKERS':
      return 'attacks'
    case 'DECLARE_BLOCKERS':
      return 'blocks'
    default:
      return 'priority'
  }
}

function formatStep(step: string): string {
  return step.toLowerCase().replace(/_/g, ' ')
}

const styles: Record<string, React.CSSProperties> = {
  toggleButton: {
    position: 'fixed',
    bottom: 12,
    left: 92,
    zIndex: 500,
    padding: '6px 12px',
    fontSize: 12,
    backgroundColor: 'rgba(20, 20, 40, 0.85)',
    color: '#aaa',
    border: '1px solid #444',
    borderRadius: 6,
    cursor: 'pointer',
  },
  toggleButtonHeld: {
    backgroundColor: 'rgba(224, 168, 64, 0.22)',
    borderColor: 'rgba(224, 168, 64, 0.6)',
    color: '#f0c473',
  },
  panel: {
    position: 'fixed',
    bottom: 48,
    left: 12,
    zIndex: 501,
    width: 'min(520px, calc(100vw - 24px))',
    maxHeight: '72vh',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: 'rgba(10, 10, 25, 0.96)',
    border: '1px solid #333',
    borderRadius: 8,
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '6px 10px',
    borderBottom: '1px solid #333',
    gap: 8,
    flexShrink: 0,
  },
  headerTitle: {
    color: '#aaa',
    fontSize: 12,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 1,
    whiteSpace: 'nowrap',
  },
  headerActions: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  actionButton: {
    padding: '3px 8px',
    fontSize: 11,
    backgroundColor: 'rgba(60, 60, 90, 0.6)',
    color: '#bbb',
    border: '1px solid #444',
    borderRadius: 4,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  actionButtonOn: {
    backgroundColor: 'rgba(224, 168, 64, 0.22)',
    borderColor: 'rgba(224, 168, 64, 0.55)',
    color: '#f0c473',
  },
  closeButton: {
    background: 'none',
    border: 'none',
    color: '#888',
    fontSize: 18,
    cursor: 'pointer',
    padding: '0 4px',
    lineHeight: 1,
  },
  error: {
    padding: '4px 10px',
    fontSize: 11,
    color: '#d08a7a',
    flexShrink: 0,
  },
  heldBanner: {
    padding: '8px 10px',
    borderBottom: '1px solid #333',
    backgroundColor: 'rgba(224, 168, 64, 0.10)',
    flexShrink: 0,
  },
  heldTitle: {
    fontSize: 12,
    color: '#f0c473',
    lineHeight: 1.5,
  },
  heldHint: {
    fontSize: 11,
    color: '#8a8a99',
    lineHeight: 1.5,
    paddingTop: 2,
  },
  heldButton: {
    marginTop: 6,
    padding: '4px 10px',
    fontSize: 11,
    backgroundColor: 'rgba(224, 168, 64, 0.2)',
    color: '#f0c473',
    border: '1px solid rgba(224, 168, 64, 0.5)',
    borderRadius: 4,
    cursor: 'pointer',
  },
  empty: {
    color: '#666',
    fontSize: 12,
    padding: 12,
    lineHeight: 1.5,
  },
  timeline: {
    maxHeight: 110,
    overflowY: 'auto',
    borderBottom: '1px solid #333',
    flexShrink: 0,
  },
  timelineRow: {
    display: 'flex',
    gap: 8,
    alignItems: 'baseline',
    width: '100%',
    textAlign: 'left',
    padding: '3px 10px',
    fontSize: 11,
    background: 'none',
    border: 'none',
    borderBottom: '1px solid rgba(255,255,255,0.04)',
    color: '#999',
    cursor: 'pointer',
  },
  timelineRowSelected: {
    backgroundColor: 'rgba(91, 192, 222, 0.12)',
    color: '#cfe8f2',
  },
  timelineRowHovered: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    color: '#ddd',
  },
  timelineTurn: {
    flexShrink: 0,
    width: 100,
    color: '#777',
  },
  heldDot: {
    color: '#f0c473',
  },
  timelineChoice: {
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  overrideTag: {
    flexShrink: 0,
    fontSize: 9,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    color: '#7fb3d5',
  },
  detail: {
    flex: 1,
    overflowY: 'auto',
    padding: '6px 10px 10px',
  },
  detailMeta: {
    fontSize: 11,
    color: '#777',
    lineHeight: 1.5,
    paddingBottom: 2,
  },
  pinned: {
    color: '#5bc0de',
  },
  overrideNote: {
    fontSize: 11,
    color: '#7fb3d5',
    lineHeight: 1.5,
    paddingTop: 2,
  },
  options: {
    paddingTop: 6,
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  option: {
    padding: '4px 6px',
    borderRadius: 4,
    border: '1px solid transparent',
  },
  optionChosen: {
    backgroundColor: 'rgba(110, 201, 139, 0.10)',
    borderColor: 'rgba(110, 201, 139, 0.35)',
  },
  optionBaseline: {
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
  },
  optionPlayed: {
    backgroundColor: 'rgba(127, 179, 213, 0.12)',
    borderColor: 'rgba(127, 179, 213, 0.45)',
  },
  optionUnscored: {
    opacity: 0.55,
  },
  optionTop: {
    display: 'flex',
    alignItems: 'baseline',
    gap: 6,
    fontSize: 12,
  },
  optionRank: {
    width: 12,
    flexShrink: 0,
    color: '#666',
    fontSize: 10,
    fontVariantNumeric: 'tabular-nums',
  },
  optionLabel: {
    flex: 1,
    color: '#ddd',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  aiPickTag: {
    flexShrink: 0,
    fontSize: 9,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    color: '#6ec98b',
  },
  playedTag: {
    flexShrink: 0,
    fontSize: 9,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    color: '#7fb3d5',
  },
  optionScore: {
    flexShrink: 0,
    fontVariantNumeric: 'tabular-nums',
    fontSize: 12,
  },
  playButton: {
    flexShrink: 0,
    padding: '1px 7px',
    fontSize: 10,
    backgroundColor: 'rgba(224, 168, 64, 0.18)',
    color: '#f0c473',
    border: '1px solid rgba(224, 168, 64, 0.45)',
    borderRadius: 3,
    cursor: 'pointer',
  },
  barTrack: {
    height: 3,
    marginTop: 3,
    marginLeft: 18,
    backgroundColor: 'rgba(255,255,255,0.06)',
    borderRadius: 2,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    borderRadius: 2,
  },
  optionNote: {
    marginLeft: 18,
    marginTop: 2,
    fontSize: 10,
    color: '#6b6b7a',
    fontStyle: 'italic',
  },
}
